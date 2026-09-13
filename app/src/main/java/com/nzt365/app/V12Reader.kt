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

enum class V12ReaderTheme { PAPER, WHITE, SEPIA, NIGHT, OLED }
private data class V12Colors(val outer: Color, val page: Color, val text: Color, val muted: Color)

private fun V12ReaderTheme.colors() = when (this) {
    V12ReaderTheme.PAPER -> V12Colors(Color(0xFFE9E3D7), Color(0xFFFFFCF4), Color(0xFF28231F), Color(0xFF766F67))
    V12ReaderTheme.WHITE -> V12Colors(Color(0xFFE9EDF0), Color.White, Color(0xFF161616), Color(0xFF666666))
    V12ReaderTheme.SEPIA -> V12Colors(Color(0xFFD9C7A7), Color(0xFFF5E4C1), Color(0xFF33281D), Color(0xFF75624D))
    V12ReaderTheme.NIGHT -> V12Colors(Color(0xFF071018), Color(0xFF111B22), Color(0xFFE9EDF0), Color(0xFF89949B))
    V12ReaderTheme.OLED -> V12Colors(Color.Black, Color.Black, Color(0xFFECECEC), Color(0xFF888888))
}

private class V14ReaderPrefs(context: Context) {
    private val p = context.getSharedPreferences("nzt_reader_v14", Context.MODE_PRIVATE)
    var theme: V12ReaderTheme
        get() = runCatching { V12ReaderTheme.valueOf(p.getString("theme", V12ReaderTheme.PAPER.name)!!) }.getOrDefault(V12ReaderTheme.PAPER)
        set(v) { p.edit().putString("theme", v.name).apply() }
    var font: Int
        get() = p.getInt("font", 20)
        set(v) { p.edit().putInt("font", v).apply() }
    var line: Float
        get() = p.getFloat("line", 1.50f)
        set(v) { p.edit().putFloat("line", v).apply() }
    var margin: Int
        get() = p.getInt("margin", 22)
        set(v) { p.edit().putInt("margin", v).apply() }
    var justify: Boolean
        get() = p.getBoolean("justify", true)
        set(v) { p.edit().putBoolean("justify", v).apply() }
}

class V12ReaderActivity : ComponentActivity() {
    companion object {
        fun intent(context: Context, book: V10Book) = Intent(context, V12ReaderActivity::class.java).putExtra("id", book.id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val library = V10Library(this)
        val id = intent.getStringExtra("id") ?: return finish()
        val book = library.books().firstOrNull { it.id == id } ?: return finish()
        setContent { NZTProTheme { V14Reader(book, library) { finish() } } }
    }
}

@Composable
private fun V14Reader(initial: V10Book, library: V10Library, onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { V14ReaderPrefs(context) }
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
            initial.progress > 0 -> (loaded.length * (initial.progress / 100f)).roundToInt()
            initial.position > 0 && initial.position < loaded.length -> initial.position
            else -> 0
        }.coerceIn(0, loaded.lastIndex.coerceAtLeast(0))
        pageStart = snapToWordStartV14(loaded, restored)
        pageEnd = pageStart
        history = listOf(pageStart)
        historyIndex = 0
        loading = false
    }

    val palette = theme.colors()
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
            history = base + next
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
            val approx = (pageStart - 1400).coerceAtLeast(0)
            val target = snapToWordStartV14(text, approx)
            pageStart = target
            pageEnd = target
            history = listOf(target)
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
        containerColor = palette.outer,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (controls) {
                Surface(color = palette.outer.copy(alpha = .98f)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null, tint = palette.text) }
                        Column(Modifier.weight(1f)) {
                            Text(book.title, color = palette.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$progress% • NZT READER", color = palette.muted, fontSize = 10.sp)
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
                Surface(color = palette.outer.copy(alpha = .98f)) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Slider(
                            value = pageStart.toFloat(),
                            onValueChange = {
                                val target = snapToWordStartV14(text, it.roundToInt())
                                pageStart = target
                                pageEnd = target
                                history = listOf(target)
                                historyIndex = 0
                            },
                            valueRange = 0f..text.lastIndex.coerceAtLeast(1).toFloat()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$progress%", color = palette.muted, fontSize = 10.sp)
                            val minutes = (((text.length - pageStart).coerceAtLeast(0) / 900f)).roundToInt().coerceAtLeast(1)
                            Text("≈ $minutes мин", color = palette.muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(palette.outer)) {
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = NztAccent)
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (theme == V12ReaderTheme.OLED) 0.dp else 8.dp, vertical = 4.dp)
                        .then(if (theme == V12ReaderTheme.OLED) Modifier else Modifier.shadow(5.dp, RoundedCornerShape(5.dp))),
                    color = palette.page,
                    shape = RoundedCornerShape(if (theme == V12ReaderTheme.OLED) 0.dp else 5.dp)
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val lineDp = (font * line).coerceAtLeast(16f)
                        val usableHeight = (maxHeight.value - 44f).coerceAtLeast(120f)
                        val maxLines = (usableHeight / lineDp).toInt().coerceAtLeast(4)
                        val candidateEnd = (pageStart + 12000).coerceAtMost(text.length)
                        val candidate = if (pageStart < text.length) text.substring(pageStart, candidateEnd) else ""

                        Text(
                            text = candidate,
                            modifier = Modifier.fillMaxSize().padding(horizontal = margin.dp, vertical = 20.dp),
                            color = palette.text,
                            fontSize = font.sp,
                            lineHeight = (font * line).sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = if (justify) TextAlign.Justify else TextAlign.Start,
                            maxLines = maxLines,
                            overflow = TextOverflow.Clip,
                            onTextLayout = { result ->
                                if (result.lineCount > 0) {
                                    val localEnd = result.getLineEnd(result.lineCount - 1, visibleEnd = true)
                                    val absoluteEnd = (pageStart + localEnd).coerceIn(pageStart, text.length)
                                    if (absoluteEnd > pageStart) pageEnd = absoluteEnd
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
                            color = palette.muted.copy(alpha = .70f),
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
                Text("NZT READER · ВИД ЧТЕНИЯ", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V12ReaderTheme.entries.forEach { t ->
                        FilterChip(
                            selected = theme == t,
                            onClick = { theme = t; prefs.theme = t },
                            label = { Text(when (t) {
                                V12ReaderTheme.PAPER -> "Книга"
                                V12ReaderTheme.WHITE -> "Белый"
                                V12ReaderTheme.SEPIA -> "Сепия"
                                V12ReaderTheme.NIGHT -> "Ночь"
                                V12ReaderTheme.OLED -> "OLED"
                            }, fontSize = 9.sp) }
                        )
                    }
                }
                Text("Шрифт $font", fontWeight = FontWeight.Bold)
                Slider(font.toFloat(), { font = it.roundToInt() }, valueRange = 14f..30f)
                Text("Интервал ${"%.1f".format(line)}", fontWeight = FontWeight.Bold)
                Slider(line, { line = it }, valueRange = 1.2f..1.9f)
                Text("Поля $margin", fontWeight = FontWeight.Bold)
                Slider(margin.toFloat(), { margin = it.roundToInt() }, valueRange = 12f..38f)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Выравнивание как в книге", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Switch(justify, { justify = it })
                }
                Text(
                    "Страница формируется по реально видимым строкам. Следующая начинается ровно с первого непрочитанного символа — без пропусков и дублирования текста.",
                    color = NztMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
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
                            val target = snapToWordStartV14(text, found)
                            pageStart = target
                            pageEnd = target
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

private fun snapToWordStartV14(text: String, offset: Int): Int {
    if (text.isBlank()) return 0
    var i = offset.coerceIn(0, text.lastIndex)
    while (i > 0 && !text[i - 1].isWhitespace()) i--
    return i
}
