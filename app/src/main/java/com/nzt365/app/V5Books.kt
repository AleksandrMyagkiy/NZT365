package com.nzt365.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.math.roundToInt

private class V5BookStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_books_v3", Context.MODE_PRIVATE)
    fun progress(n: Int) = p.getInt("progress_$n", 0)
    fun note(n: Int) = p.getString("note_$n", "") ?: ""
    fun idea(n: Int) = p.getString("idea_$n", "") ?: ""
    fun save(n: Int, progress: Int, note: String, idea: String) = p.edit()
        .putInt("progress_$n", progress.coerceIn(0, 100))
        .putString("note_$n", note)
        .putString("idea_$n", idea)
        .apply()
}

private data class V9ImportedBook(
    val id: String,
    val title: String,
    val path: String,
    val type: String,
    val progress: Int = 0,
    val page: Int = 0
)

private class V9BookLibrary(private val context: Context) {
    private val p = context.getSharedPreferences("nzt_book_library_v9", Context.MODE_PRIVATE)

    fun books(): List<V9ImportedBook> {
        val raw = p.getString("books", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                V9ImportedBook(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    path = o.getString("path"),
                    type = o.getString("type"),
                    progress = o.optInt("progress", 0),
                    page = o.optInt("page", 0)
                )
            }.filter { File(it.path).exists() }
        }.getOrDefault(emptyList())
    }

    fun import(uri: Uri): V9ImportedBook? {
        val resolver = context.contentResolver
        var display = "Book"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) display = c.getString(0) ?: display
        }
        val mime = resolver.getType(uri).orEmpty()
        val ext = when {
            display.substringAfterLast('.', "").equals("pdf", true) || mime.contains("pdf") -> "pdf"
            display.substringAfterLast('.', "").equals("epub", true) || mime.contains("epub") -> "epub"
            display.substringAfterLast('.', "").equals("txt", true) || mime.startsWith("text/") -> "txt"
            else -> return null
        }
        val dir = File(context.filesDir, "books").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val target = File(dir, "$id.$ext")
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } } ?: return null
        val item = V9ImportedBook(id, display.substringBeforeLast('.').ifBlank { display }, target.absolutePath, ext.uppercase())
        save(books() + item)
        return item
    }

    fun update(book: V9ImportedBook) {
        save(books().map { if (it.id == book.id) book else it })
    }

    fun delete(book: V9ImportedBook) {
        runCatching { File(book.path).delete() }
        save(books().filterNot { it.id == book.id })
    }

    private fun save(items: List<V9ImportedBook>) {
        val arr = JSONArray()
        items.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id); put("title", b.title); put("path", b.path); put("type", b.type); put("progress", b.progress); put("page", b.page)
            })
        }
        p.edit().putString("books", arr.toString()).apply()
    }
}

@Composable
fun V5BooksScreen(lang: AppLanguage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { V5BookStore(context) }
    val library = remember { V9BookLibrary(context) }
    var selected by remember { mutableStateOf<V3Book?>(null) }
    var reading by remember { mutableStateOf<V9ImportedBook?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var importError by remember { mutableStateOf(false) }
    val completed = remember(refresh) { v3Books.count { store.progress(it.number) >= 100 } }
    val imported = remember(refresh) { library.books() }
    val current = remember(refresh) { v3Books.firstOrNull { store.progress(it.number) < 100 } ?: v3Books.last() }
    val currentProgress = remember(refresh) { store.progress(current.number) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (library.import(uri) == null) importError = true else refresh++
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v5BookText(lang, "year_plan"), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("$completed / 12", color = NztAccent, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(color = NztSurface2, shape = RoundedCornerShape(16.dp)) {
                            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.MenuBook, null, tint = NztAccent) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(v5BookText(lang, "current"), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(current.title, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(current.author, color = NztMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { currentProgress / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = NztAccent, trackColor = NztLine)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10222A)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LibraryBooks, null, tint = NztAccent)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v5BookText(lang, "my_library"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(v5BookText(lang, "formats"), color = NztMuted, fontSize = 11.sp)
                        }
                        Text(imported.size.toString(), color = NztAccent, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { launcher.launch(arrayOf("application/pdf", "application/epub+zip", "text/plain", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text(v5BookText(lang, "import"), fontWeight = FontWeight.Black)
                    }
                    Text(v5BookText(lang, "legal"), color = NztMuted, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        if (imported.isNotEmpty()) {
            item { Text(v5BookText(lang, "downloaded"), color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black) }
            items(imported, key = { it.id }) { book ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { reading = book },
                    colors = CardDefaults.cardColors(containerColor = NztSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(if (book.type == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Article, null, tint = NztAccent)
                                    Text(book.type, fontSize = 8.sp, color = NztMuted)
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${book.progress}%", color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            LinearProgressIndicator(progress = { book.progress / 100f }, modifier = Modifier.fillMaxWidth().height(5.dp), color = NztAccent, trackColor = NztLine)
                        }
                        IconButton(onClick = { library.delete(book); refresh++ }) { Icon(Icons.Default.DeleteOutline, null, tint = NztMuted) }
                    }
                }
            }
        }

        item { Text(v5BookText(lang, "plan"), color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black) }
        items(v3Books, key = { it.number }) { book ->
            val p = store.progress(book.number)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { selected = book },
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    V5BookCover(book.number, book.title)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(book.author, color = NztMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { p / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = NztAccent, trackColor = NztLine)
                    }
                    Spacer(Modifier.width(12.dp))
                    Surface(color = if (p >= 100) NztAccent else Color(0xFF5A5266), shape = RoundedCornerShape(15.dp)) {
                        Box(Modifier.width(64.dp).height(48.dp), contentAlignment = Alignment.Center) {
                            if (p >= 100) Icon(Icons.Default.Check, null, tint = Color.Black) else Text("$p%", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }

    selected?.let { book ->
        V5BookDialog(
            lang = lang,
            book = book,
            initial = store.progress(book.number),
            initialNote = store.note(book.number),
            initialIdea = store.idea(book.number),
            onDismiss = { selected = null },
            onSave = { p, note, idea -> store.save(book.number, p, note, idea); refresh++; selected = null }
        )
    }

    reading?.let { book ->
        V9ReaderDialog(lang, book, library, onClose = { reading = null; refresh++ })
    }

    if (importError) {
        AlertDialog(
            onDismissRequest = { importError = false },
            title = { Text(v5BookText(lang, "unsupported")) },
            text = { Text(v5BookText(lang, "supported")) },
            confirmButton = { TextButton(onClick = { importError = false }) { Text("OK") } },
            containerColor = NztSurface
        )
    }
}

@Composable
private fun V9ReaderDialog(lang: AppLanguage, book: V9ImportedBook, library: V9BookLibrary, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = NztBg) {
            if (book.type == "PDF") V9PdfReader(lang, book, library, onClose) else V9TextReader(lang, book, library, onClose)
        }
    }
}

@Composable
private fun V9PdfReader(lang: AppLanguage, initial: V9ImportedBook, library: V9BookLibrary, onClose: () -> Unit) {
    var book by remember { mutableStateOf(initial) }
    val file = remember { File(book.path) }
    val pageCount = remember(file.path) { pdfPageCount(file) }
    var page by remember { mutableIntStateOf(book.page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(page, file.path) {
        bitmap = withContext(Dispatchers.IO) { renderPdfPage(file, page) }
        if (pageCount > 0) {
            val progress = (((page + 1) * 100f) / pageCount).roundToInt().coerceIn(0, 100)
            book = book.copy(page = page, progress = progress)
            library.update(book)
        }
    }

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
                Column(Modifier.weight(1f)) {
                    Text(book.title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("PDF · ${page + 1}/$pageCount · ${book.progress}%", color = NztMuted, fontSize = 10.sp)
                }
            }
        },
        bottomBar = {
            Surface(color = NztSurface) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ChevronLeft, null); Text(v5BookText(lang,"prev")) }
                    Button(onClick = { page = (page + 1).coerceAtMost((pageCount - 1).coerceAtLeast(0)) }, enabled = page < pageCount - 1, modifier = Modifier.weight(1f)) { Text(v5BookText(lang,"next")); Icon(Icons.Default.ChevronRight, null) }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
                ?: CircularProgressIndicator(Modifier.padding(40.dp))
        }
    }
}

@Composable
private fun V9TextReader(lang: AppLanguage, initial: V9ImportedBook, library: V9BookLibrary, onClose: () -> Unit) {
    var book by remember { mutableStateOf(initial) }
    var fontSize by remember { mutableIntStateOf(18) }
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(book.path) {
        text = withContext(Dispatchers.IO) { loadTextBook(File(book.path), book.type) }
        loading = false
    }
    val chunks = remember(text) { text.chunked(3500) }

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
                Column(Modifier.weight(1f)) {
                    Text(book.title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${book.type} · ${book.progress}%", color = NztMuted, fontSize = 10.sp)
                }
                IconButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(12) }) { Text("A−", fontWeight = FontWeight.Black) }
                IconButton(onClick = { fontSize = (fontSize + 2).coerceAtMost(30) }) { Text("A+", fontWeight = FontWeight.Black) }
            }
        },
        bottomBar = {
            Surface(color = NztSurface) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(v5BookText(lang,"reading_progress") + " ${book.progress}%", color = NztMuted, fontSize = 10.sp)
                    Slider(value = book.progress.toFloat(), onValueChange = {
                        book = book.copy(progress = it.roundToInt().coerceIn(0, 100)); library.update(book)
                    }, valueRange = 0f..100f)
                }
            }
        }
    ) { pad ->
        if (loading) Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(chunks) { chunk -> Text(chunk, fontSize = fontSize.sp, lineHeight = (fontSize * 1.55).sp) }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

private fun pdfPageCount(file: File): Int = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd -> PdfRenderer(pfd).use { it.pageCount } }
}.getOrDefault(0)

private fun renderPdfPage(file: File, pageIndex: Int): Bitmap? = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount <= 0) return@use null
            renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1)).use { page ->
                val width = 1400.coerceAtMost(page.width * 2).coerceAtLeast(page.width)
                val height = (width * (page.height.toFloat() / page.width)).roundToInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }
}.getOrNull()

private fun loadTextBook(file: File, type: String): String = runCatching {
    if (type == "TXT") file.readText(Charsets.UTF_8) else {
        val parts = mutableListOf<String>()
        ZipFile(file).use { zip ->
            val entries = zip.entries().toList().filter { !it.isDirectory && (it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true) || it.name.endsWith(".htm", true)) }.sortedBy { it.name }
            entries.forEach { entry ->
                val html = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                val clean = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\n{3,}"), "\n\n").trim()
                if (clean.isNotBlank()) parts += clean
            }
        }
        parts.joinToString("\n\n")
    }
}.getOrElse { "Unable to open this book." }

@Composable
private fun V5BookCover(number: Int, title: String) {
    val palettes = listOf(
        listOf(Color(0xFF203B4B), Color(0xFF14232E)), listOf(Color(0xFF39412C), Color(0xFF1E281C)),
        listOf(Color(0xFF493B52), Color(0xFF292032)), listOf(Color(0xFF4A332E), Color(0xFF2A1F1B))
    )
    val colors = palettes[(number - 1) % palettes.size]
    Box(Modifier.width(62.dp).height(82.dp).background(Brush.linearGradient(colors), RoundedCornerShape(14.dp)).padding(8.dp)) {
        Text(number.toString().padStart(2, '0'), color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(title.take(18), modifier = Modifier.align(Alignment.BottomStart), fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V5BookDialog(lang: AppLanguage, book: V3Book, initial: Int, initialNote: String, initialIdea: String, onDismiss: () -> Unit, onSave: (Int, String, String) -> Unit) {
    var progress by remember { mutableIntStateOf(initial) }
    var note by remember { mutableStateOf(initialNote) }
    var idea by remember { mutableStateOf(initialIdea) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Column { Text(book.title, fontWeight = FontWeight.Black); Text(book.author, color = NztMuted, fontSize = 13.sp) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(v5Purpose(lang, book.number), color = NztAccent, fontSize = 13.sp)
                Text("${v5BookText(lang,"progress")}: $progress%", fontWeight = FontWeight.Bold)
                Slider(value = progress.toFloat(), onValueChange = { progress = ((it / 5f).toInt() * 5).coerceIn(0, 100) }, valueRange = 0f..100f)
                OutlinedTextField(idea, { idea = it }, label = { Text(v5BookText(lang,"idea")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(note, { note = it }, label = { Text(v5BookText(lang,"note")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedButton(onClick = { progress = 100 }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text(v5BookText(lang,"read")) }
            }
        },
        confirmButton = { Button(onClick = { onSave(progress, note.trim(), idea.trim()) }) { Text(v5BookText(lang,"save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v5BookText(lang,"close")) } },
        containerColor = NztSurface
    )
}

private fun v5BookText(lang: AppLanguage, key: String): String {
    val ru = mapOf("year_plan" to "12 КНИГ / 12 МЕСЯЦЕВ", "current" to "СЕЙЧАС", "progress" to "Прогресс", "idea" to "Идея, которую внедряю", "note" to "Заметки", "read" to "Отметить прочитанной", "save" to "Сохранить", "close" to "Закрыть", "my_library" to "Моя библиотека", "formats" to "Загружай PDF, EPUB и TXT и читай внутри NZT", "import" to "Загрузить книгу", "legal" to "Импортируй только файлы, которыми имеешь право пользоваться.", "downloaded" to "ЗАГРУЖЕННЫЕ КНИГИ", "plan" to "ПЛАН НА 12 МЕСЯЦЕВ", "unsupported" to "Формат не поддерживается", "supported" to "Поддерживаются PDF, EPUB и TXT.", "prev" to "Назад", "next" to "Далее", "reading_progress" to "Прогресс чтения")
    val en = mapOf("year_plan" to "12 BOOKS / 12 MONTHS", "current" to "CURRENT", "progress" to "Progress", "idea" to "Idea I will apply", "note" to "Notes", "read" to "Mark as read", "save" to "Save", "close" to "Close", "my_library" to "My library", "formats" to "Import PDF, EPUB and TXT and read inside NZT", "import" to "Import book", "legal" to "Only import files you have the right to use.", "downloaded" to "IMPORTED BOOKS", "plan" to "12-MONTH PLAN", "unsupported" to "Unsupported format", "supported" to "PDF, EPUB and TXT are supported.", "prev" to "Back", "next" to "Next", "reading_progress" to "Reading progress")
    val pl = mapOf("year_plan" to "12 KSIĄŻEK / 12 MIESIĘCY", "current" to "TERAZ", "progress" to "Postęp", "idea" to "Pomysł do wdrożenia", "note" to "Notatki", "read" to "Oznacz jako przeczytaną", "save" to "Zapisz", "close" to "Zamknij", "my_library" to "Moja biblioteka", "formats" to "Importuj PDF, EPUB i TXT i czytaj w NZT", "import" to "Importuj książkę", "legal" to "Importuj tylko pliki, z których masz prawo korzystać.", "downloaded" to "ZAIMPORTOWANE KSIĄŻKI", "plan" to "PLAN 12-MIESIĘCZNY", "unsupported" to "Nieobsługiwany format", "supported" to "Obsługiwane są PDF, EPUB i TXT.", "prev" to "Wstecz", "next" to "Dalej", "reading_progress" to "Postęp czytania")
    val uk = mapOf("year_plan" to "12 КНИГ / 12 МІСЯЦІВ", "current" to "ЗАРАЗ", "progress" to "Прогрес", "idea" to "Ідея для впровадження", "note" to "Нотатки", "read" to "Позначити прочитаною", "save" to "Зберегти", "close" to "Закрити", "my_library" to "Моя бібліотека", "formats" to "Завантажуй PDF, EPUB і TXT та читай у NZT", "import" to "Завантажити книгу", "legal" to "Імпортуй лише файли, якими маєш право користуватися.", "downloaded" to "ЗАВАНТАЖЕНІ КНИГИ", "plan" to "ПЛАН НА 12 МІСЯЦІВ", "unsupported" to "Формат не підтримується", "supported" to "Підтримуються PDF, EPUB і TXT.", "prev" to "Назад", "next" to "Далі", "reading_progress" to "Прогрес читання")
    return when (lang) { AppLanguage.RU -> ru; AppLanguage.EN -> en; AppLanguage.PL -> pl; AppLanguage.UK -> uk }[key] ?: key
}

private fun v5Purpose(lang: AppLanguage, n: Int): String {
    val ru = listOf("Переговоры и влияние", "Системы и привычки", "Деньги и мышление", "Психология убеждения", "Концентрация и продуктивность", "Рост до руководителя", "Системное управление", "Система принятия решений", "Мышление и когнитивные ошибки", "Влияние, статус и власть", "Устойчивость и неопределённость", "Капитал и инвестиционная дисциплина")
    val en = listOf("Negotiation and influence", "Systems and habits", "Money and mindset", "Psychology of persuasion", "Focus and productivity", "Executive effectiveness", "Systemic management", "Decision systems", "Thinking and cognitive bias", "Influence, status and power", "Resilience and uncertainty", "Capital and investment discipline")
    val pl = listOf("Negocjacje i wpływ", "Systemy i nawyki", "Pieniądze i myślenie", "Psychologia perswazji", "Koncentracja i produktywność", "Skuteczność menedżera", "Zarządzanie systemowe", "System podejmowania decyzji", "Myślenie i błędy poznawcze", "Wpływ, status i władza", "Odporność i niepewność", "Kapitał i dyscyplina inwestycyjna")
    val uk = listOf("Переговори та вплив", "Системи й звички", "Гроші та мислення", "Психологія переконання", "Концентрація та продуктивність", "Ефективність керівника", "Системне управління", "Система прийняття рішень", "Мислення та когнітивні помилки", "Вплив, статус і влада", "Стійкість і невизначеність", "Капітал та інвестиційна дисципліна")
    val list = when (lang) { AppLanguage.RU -> ru; AppLanguage.EN -> en; AppLanguage.PL -> pl; AppLanguage.UK -> uk }
    return list[(n - 1).coerceIn(0, 11)]
}
