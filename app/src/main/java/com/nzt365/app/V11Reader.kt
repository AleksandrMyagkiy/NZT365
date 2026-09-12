package com.nzt365.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.roundToInt

private enum class V11Theme { LIGHT, PAPER, SEPIA, NIGHT, OLED }
private data class V11Palette(val outside: Color, val page: Color, val text: Color, val muted: Color)

private fun V11Theme.palette(): V11Palette = when (this) {
    V11Theme.LIGHT -> V11Palette(Color(0xFFECEFF1), Color.White, Color(0xFF161616), Color(0xFF686868))
    V11Theme.PAPER -> V11Palette(Color(0xFFE9E3D7), Color(0xFFFFFCF4), Color(0xFF28231F), Color(0xFF756E66))
    V11Theme.SEPIA -> V11Palette(Color(0xFFD6C3A0), Color(0xFFF3E2BE), Color(0xFF33291D), Color(0xFF76634C))
    V11Theme.NIGHT -> V11Palette(Color(0xFF071018), Color(0xFF111B22), Color(0xFFE9EDF0), Color(0xFF89949B))
    V11Theme.OLED -> V11Palette(Color.Black, Color.Black, Color(0xFFECECEC), Color(0xFF888888))
}

private class V11ReaderPrefs(context: Context) {
    private val p = context.getSharedPreferences("nzt_reader_v11", Context.MODE_PRIVATE)
    var theme: V11Theme
        get() = runCatching { V11Theme.valueOf(p.getString("theme", V11Theme.PAPER.name)!!) }.getOrDefault(V11Theme.PAPER)
        set(v) { p.edit().putString("theme", v.name).apply() }
    var font: Int
        get() = p.getInt("font", 19)
        set(v) { p.edit().putInt("font", v).apply() }
    var line: Float
        get() = p.getFloat("line", 1.48f)
        set(v) { p.edit().putFloat("line", v).apply() }
    var margin: Int
        get() = p.getInt("margin", 22)
        set(v) { p.edit().putInt("margin", v).apply() }
    var justify: Boolean
        get() = p.getBoolean("justify", true)
        set(v) { p.edit().putBoolean("justify", v).apply() }
}

class V11ReaderActivity : ComponentActivity() {
    companion object {
        internal fun intent(c: Context, b: V10Book) = Intent(c, V11ReaderActivity::class.java).apply {
            putExtra("id", b.id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lib = V10Library(this)
        val id = intent.getStringExtra("id") ?: return finish()
        val book = lib.books().firstOrNull { it.id == id } ?: return finish()
        setContent { NZTProTheme { V12Reader(book, lib) { finish() } } }
    }
}

@Composable
private fun V12Reader(initial: V10Book, library: V10Library, onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { V11ReaderPrefs(context) }
    var book by remember { mutableStateOf(initial) }
    var theme by remember { mutableStateOf(prefs.theme) }
    var font by remember { mutableIntStateOf(prefs.font) }
    var line by remember { mutableFloatStateOf(prefs.line) }
    var margin by remember { mutableIntStateOf(prefs.margin) }
    var justify by remember { mutableStateOf(prefs.justify) }
    var controls by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var pageStart by remember { mutableIntStateOf(0) }
    var pageEnd by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(listOf(0)) }
    var historyIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(initial.path) {
        val loaded = loadV10Text(File(initial.path), initial.type)
            .replace("\r\n", "\n")
            .replace(Regex("\n{4,}"), "\n\n")
        text = loaded
        val restored = when {
            initial.position > 120 -> initial.position
            initial.progress > 0 -> (loaded.length * (initial.progress / 100f)).roundToInt()
            else -> 0
        }.coerceIn(0, loaded.length.coerceAtLeast(1) - 1)
        pageStart = snapToWordStart(loaded, restored)
        pageEnd = pageStart
        history = listOf(pageStart)
        historyIndex = 0
        loading = false
    }

    val palette = theme.palette()
    val progress = if (text.isBlank()) 0 else ((pageStart * 100f) / text.length).roundToInt().coerceIn(0, 100)

    fun savePrefs() {
        prefs.theme = theme
        prefs.font = font
        prefs.line = line
        prefs.margin = margin
        prefs.justify = justify
    }

    fun moveTo(offset: Int, addHistory: Boolean = true) {
        if (text.isBlank()) return
        val next = offset.coerceIn(0, text.lastIndex.coerceAtLeast(0))
        pageStart = next
        pageEnd = next
        if (addHistory) {
            val base = history.take(historyIndex + 1)
            history = (base + next).distinct()
            historyIndex = history.lastIndex
        }
    }

    fun nextPage() {
        if (pageEnd <= pageStart || pageEnd >= text.length) return
        moveTo(pageEnd)
    }

    fun previousPage() {
        if (historyIndex > 0) {
            historyIndex--
            pageStart = history[historyIndex]
            pageEnd = pageStart
        } else if (pageStart > 0) {
            val approx = (pageStart - 900).coerceAtLeast(0)
            moveTo(snapToWordStart(text, approx), addHistory = false)
            history = listOf(pageStart)
            historyIndex = 0
        }
    }

    LaunchedEffect(pageStart, text.length) {
        if (text.isNotBlank()) {
            val updated = book.copy(position = pageStart, progress = progress)
            book = updated
            library.update(updated)
        }
    }
    DisposableEffect(Unit) { onDispose { savePrefs() } }

    Scaffold(
        containerColor = palette.outside,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (controls) {
                Surface(color = palette.outside.copy(alpha = .98f)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null, tint = palette.text) }
                        Column(Modifier.weight(1f)) {
                            Text(book.title, color = palette.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$progress% • позиция ${pageStart + 1}", color = palette.muted, fontSize = 10.sp)
                        }
                        IconButton(onClick = { searchOpen = true }) { Icon(Icons.Default.Search, null, tint = palette.text) }
                        IconButton(onClick = {
                            book = book.copy(bookmarks = if (pageStart in book.bookmarks) book.bookmarks - pageStart else book.bookmarks + pageStart)
                            library.update(book)
                        }) {
                            Icon(if (pageStart in book.bookmarks) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = NztAccent)
                        }
                        IconButton(onClick = { settings = true }) { Icon(Icons.Default.Tune, null, tint = palette.text) }
                    }
                }
            }
        },
        bottomBar = {
            if (controls) {
                Surface(color = palette.outside.copy(alpha = .98f)) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Slider(
                            value = pageStart.toFloat(),
                            onValueChange = {
                                val target = snapToWordStart(text, it.roundToInt())
                                moveTo(target, addHistory = false)
                                history = listOf(target)
                                historyIndex = 0
                            },
                            valueRange = 0f..text.lastIndex.coerceAtLeast(1).toFloat()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$progress%", color = palette.muted, fontSize = 10.sp)
                            val minutes = (((text.length - pageStart).coerceAtLeast(0) / 850f) * 2f).roundToInt().coerceAtLeast(1)
                            Text("≈ $minutes мин", color = palette.muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(palette.outside)) {
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = NztAccent)
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (theme == V11Theme.OLED) 0.dp else 8.dp, vertical = 4.dp)
                        .then(if (theme == V11Theme.OLED) Modifier else Modifier.shadow(5.dp, RoundedCornerShape(5.dp))),
                    color = palette.page,
                    shape = RoundedCornerShape(if (theme == V11Theme.OLED) 0.dp else 5.dp)
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val lineDp = (font * line).coerceAtLeast(16f)
                        val usableHeight = (maxHeight.value - 42f).coerceAtLeast(120f)
                        val maxLines = (usableHeight / lineDp).toInt().coerceAtLeast(4)
                        val candidateEnd = (pageStart + 10000).coerceAtMost(text.length)
                        val candidate = if (pageStart < text.length) text.substring(pageStart, candidateEnd) else ""

                        Text(
                            text = candidate,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = margin.dp, vertical = 20.dp),
                            color = palette.text,
                            fontSize = font.sp,
                            lineHeight = (font * line).sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = if (justify) TextAlign.Justify else TextAlign.Start,
                            maxLines = maxLines,
                            overflow = TextOverflow.Clip,
                            onTextLayout = { result ->
                                if (result.lineCount > 0) {
                                    val visible = result.getLineEnd(result.lineCount - 1, visibleEnd = true)
                                    val absolute = (pageStart + visible).coerceIn(pageStart, text.length)
                                    if (absolute > pageStart) pageEnd = absolute
                                }
                            }
                        )

                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(.27f).fillMaxHeight().clickable { previousPage() })
                            Box(Modifier.weight(.46f).fillMaxHeight().clickable { controls = !controls })
                            Box(Modifier.weight(.27f).fillMaxHeight().clickable { nextPage() })
                        }
                        Text(
                            "$progress%",
                            color = palette.muted.copy(alpha = .7f),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
                        )
                    }
                }
            }
        }
    }

    if (settings) {
        ModalBottomSheet(onDismissRequest = { settings = false; savePrefs() }, containerColor = NztSurface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding()) {
                Text("ВИД ЧТЕНИЯ", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    V11Theme.entries.forEach { t ->
                        FilterChip(
                            selected = theme == t,
                            onClick = { theme = t; prefs.theme = t },
                            label = { Text(when(t){V11Theme.LIGHT->"Белый";V11Theme.PAPER->"Книга";V11Theme.SEPIA->"Сепия";V11Theme.NIGHT->"Ночь";V11Theme.OLED->"OLED"}, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Шрифт  $font", fontWeight = FontWeight.Bold)
                Slider(font.toFloat(), { font = it.roundToInt() }, valueRange = 14f..30f)
                Text("Интервал  ${"%.1f".format(line)}", fontWeight = FontWeight.Bold)
                Slider(line, { line = it }, valueRange = 1.2f..1.9f)
                Text("Поля  $margin", fontWeight = FontWeight.Bold)
                Slider(margin.toFloat(), { margin = it.roundToInt() }, valueRange = 12f..38f)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Выравнивание как в книге", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Switch(justify, { justify = it })
                }
                Text("В v12 страница заканчивается ровно там, где заканчивается видимый текст. Следующая страница начинается с первого непрочитанного символа.", color = NztMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }

    if (searchOpen) {
        AlertDialog(
            onDismissRequest = { searchOpen = false },
            title = { Text("Поиск по книге") },
            text = {
                OutlinedTextField(search, { search = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) })
            },
            confirmButton = {
                Button(onClick = {
                    val q = search.trim()
                    if (q.isNotEmpty()) {
                        val found = text.indexOf(q, startIndex = (pageStart + 1).coerceAtMost(text.length), ignoreCase = true)
                            .takeIf { it >= 0 }
                            ?: text.indexOf(q, ignoreCase = true).takeIf { it >= 0 }
                        if (found != null) {
                            val target = snapToWordStart(text, found)
                            moveTo(target, addHistory = false)
                            history = listOf(target)
                            historyIndex = 0
                        }
                    }
                    searchOpen = false
                }) { Text("Найти") }
            },
            dismissButton = { TextButton(onClick = { searchOpen = false }) { Text("Отмена") } },
            containerColor = NztSurface
        )
    }
}

private fun snapToWordStart(text: String, offset: Int): Int {
    if (text.isBlank()) return 0
    var i = offset.coerceIn(0, text.lastIndex)
    while (i > 0 && !text[i - 1].isWhitespace()) i--
    return i
}
