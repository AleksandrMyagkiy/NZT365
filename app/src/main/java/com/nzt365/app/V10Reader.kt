package com.nzt365.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Html
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.junrar.Junrar
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.math.roundToInt

internal data class V10Book(
    val id: String,
    val title: String,
    val path: String,
    val type: String,
    val progress: Int = 0,
    val position: Int = 0,
    val bookmarks: Set<Int> = emptySet()
)

internal class V10Library(private val context: Context) {
    private val p = context.getSharedPreferences("nzt_book_library_v10", Context.MODE_PRIVATE)
    private val supported = setOf("pdf","epub","fb2","zip","txt","html","htm","rtf","mobi","azw","azw3","doc","cbz","cbr","djvu","djv")

    init { migrateV9() }

    fun books(): List<V10Book> = runCatching {
        val a = JSONArray(p.getString("books", "[]") ?: "[]")
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            V10Book(
                id=o.getString("id"), title=o.getString("title"), path=o.getString("path"), type=o.getString("type"),
                progress=o.optInt("progress"), position=o.optInt("position"),
                bookmarks=o.optJSONArray("bookmarks")?.let { ar -> (0 until ar.length()).map { ar.getInt(it) }.toSet() } ?: emptySet()
            )
        }.filter { File(it.path).exists() }
    }.getOrDefault(emptyList())

    fun import(uri: Uri): V10Book? {
        var name = "Book"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if(c.moveToFirst()) name = c.getString(0) ?: name }
        var ext = name.substringAfterLast('.', "").lowercase()
        if (ext == "zip") {
            // A .fb2.zip archive is common and should be treated as a book, not a comic.
            val low = name.lowercase()
            if (low.endsWith(".fb2.zip")) ext = "fb2.zip"
        }
        if (ext !in supported && ext != "fb2.zip") return null
        val dir = File(context.filesDir, "reader10").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val safeExt = ext.replace('.', '_')
        val target = File(dir, "$id.$safeExt")
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: return null
        val type = when(ext) {
            "fb2.zip" -> "FB2.ZIP"
            "djv" -> "DJVU"
            else -> ext.uppercase()
        }
        val item = V10Book(id, name.removeSuffix(".$ext").removeSuffix(".fb2").ifBlank { name }, target.absolutePath, type)
        save(books() + item)
        return item
    }

    fun update(book: V10Book) = save(books().map { if(it.id==book.id) book else it })
    fun delete(book: V10Book) { runCatching { File(book.path).deleteRecursively() }; save(books().filterNot { it.id==book.id }) }

    private fun save(list: List<V10Book>) {
        val a=JSONArray(); list.forEach { b ->
            a.put(JSONObject().apply {
                put("id",b.id); put("title",b.title); put("path",b.path); put("type",b.type); put("progress",b.progress); put("position",b.position)
                put("bookmarks", JSONArray().apply { b.bookmarks.sorted().forEach { put(it) } })
            })
        }; p.edit().putString("books",a.toString()).apply()
    }

    private fun migrateV9() {
        if(p.getBoolean("migrated",false)) return
        val old=context.getSharedPreferences("nzt_book_library_v9",Context.MODE_PRIVATE).getString("books","[]") ?: "[]"
        val migrated=runCatching {
            val a=JSONArray(old); (0 until a.length()).mapNotNull { i ->
                val o=a.getJSONObject(i); val path=o.optString("path"); if(!File(path).exists()) null else V10Book(
                    o.optString("id",UUID.randomUUID().toString()),o.optString("title","Book"),path,o.optString("type","TXT"),o.optInt("progress"),o.optInt("page"))
            }
        }.getOrDefault(emptyList())
        if(migrated.isNotEmpty()) save(migrated)
        p.edit().putBoolean("migrated",true).apply()
    }
}

@Composable
fun V10BooksScreen(lang: AppLanguage, modifier: Modifier = Modifier) {
    val context=LocalContext.current
    val library=remember { V10Library(context) }
    var tick by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val books=remember(tick,query) { library.books().filter { query.isBlank() || it.title.contains(query,true) } }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) { if(library.import(uri)==null) error=v10t(lang,"Этот формат пока не распознан.","This file format was not recognized.","Nie rozpoznano tego formatu.","Цей формат не розпізнано.") else tick++ }
    }
    LazyColumn(modifier, contentPadding=PaddingValues(horizontal=20.dp,vertical=12.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("NZT READER",color=NztAccent,fontWeight=FontWeight.Black,fontSize=12.sp); Text(v10t(lang,"Библиотека","Library","Biblioteka","Бібліотека"),fontSize=30.sp,fontWeight=FontWeight.Black) }
                Surface(color=NztSurface2,shape=RoundedCornerShape(18.dp)){ Icon(Icons.Default.AutoStories,null,tint=NztAccent,modifier=Modifier.padding(14.dp)) }
            }
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF10242B)),shape=RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(v10t(lang,"Читалка уровня отдельного приложения","A full reader inside NZT","Pełny czytnik w NZT","Повноцінна читалка в NZT"),fontWeight=FontWeight.Black,fontSize=18.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("EPUB • FB2/FB2.ZIP • MOBI/AZW3 • TXT • HTML • RTF • DOC • PDF • CBZ/CBR • DJVU",color=NztMuted,fontSize=11.sp,lineHeight=16.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick={launcher.launch(arrayOf("*/*"))},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add,null); Spacer(Modifier.width(7.dp)); Text(v10t(lang,"Добавить книгу","Add book","Dodaj książkę","Додати книгу"),fontWeight=FontWeight.Black) }
                }
            }
        }
        item { OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text(v10t(lang,"Поиск по библиотеке","Search library","Szukaj w bibliotece","Пошук у бібліотеці"))},shape=RoundedCornerShape(18.dp)) }
        if(books.isEmpty()) item { Card(colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)){ Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){ Icon(Icons.Default.MenuBook,null,tint=NztAccent,modifier=Modifier.size(36.dp)); Spacer(Modifier.height(10.dp)); Text(v10t(lang,"Загрузи первую книгу","Import your first book","Dodaj pierwszą książkę","Додай першу книгу"),fontWeight=FontWeight.Bold) } } }
        items(books,key={it.id}) { book ->
            Card(Modifier.fillMaxWidth().clickable { context.startActivity(V10ReaderActivity.intent(context,book)) },colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(21.dp)) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=NztSurface2,shape=RoundedCornerShape(15.dp)){ Column(Modifier.size(64.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){ Icon(if(book.type in setOf("CBZ","CBR","PDF","DJVU")) Icons.Default.CollectionsBookmark else Icons.Default.Article,null,tint=NztAccent); Text(book.type,fontSize=8.sp,color=NztMuted,fontWeight=FontWeight.Bold) } }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){ Text(book.title,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis); Text("${book.progress}%",color=NztAccent,fontSize=11.sp,fontWeight=FontWeight.Bold); LinearProgressIndicator(progress={book.progress/100f},modifier=Modifier.fillMaxWidth().height(5.dp),color=NztAccent,trackColor=NztLine) }
                    IconButton(onClick={library.delete(book);tick++}){Icon(Icons.Default.DeleteOutline,null,tint=NztMuted)}
                }
            }
        }
        item { Text(v10t(lang,"ПЛАН 12 КНИГ / 12 МЕСЯЦЕВ","12 BOOKS / 12 MONTHS","12 KSIĄŻEK / 12 MIESIĘCY","12 КНИГ / 12 МІСЯЦІВ"),color=NztAccent,fontWeight=FontWeight.Black,fontSize=11.sp) }
        items(v3Books,key={"plan_${it.number}"}) { b ->
            Card(colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(18.dp)){ Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){ Surface(color=NztSurface2,shape=RoundedCornerShape(12.dp)){ Text(b.number.toString().padStart(2,'0'),Modifier.padding(13.dp),color=NztAccent,fontWeight=FontWeight.Black) }; Spacer(Modifier.width(12.dp)); Column{Text(b.title,fontWeight=FontWeight.Bold);Text(b.author,color=NztMuted,fontSize=12.sp)} } }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
    error?.let { AlertDialog(onDismissRequest={error=null},title={Text(v10t(lang,"Не удалось открыть","Could not open","Nie można otworzyć","Не вдалося відкрити"))},text={Text(it)},confirmButton={TextButton(onClick={error=null}){Text("OK")}},containerColor=NztSurface) }
}

class V10ReaderActivity: ComponentActivity() {
    companion object {
        fun intent(c:Context,b:V10Book)=Intent(c,V10ReaderActivity::class.java).apply { putExtra("id",b.id);putExtra("title",b.title);putExtra("path",b.path);putExtra("type",b.type) }
    }
    override fun onCreate(savedInstanceState:Bundle?){ super.onCreate(savedInstanceState); val lib=V10Library(this); val id=intent.getStringExtra("id")?:return finish(); val book=lib.books().firstOrNull{it.id==id}?:return finish(); setContent { NZTProTheme { V10Reader(book,lib){finish()} } } }
}

private enum class ReaderTheme { NIGHT, PAPER, SEPIA, OLED }
@Composable private fun V10Reader(initial:V10Book,library:V10Library,onClose:()->Unit){
    var book by remember { mutableStateOf(initial) }; var theme by remember { mutableStateOf(ReaderTheme.NIGHT) }; var font by remember { mutableIntStateOf(18) }; var settings by remember { mutableStateOf(false) }
    val bg=when(theme){ReaderTheme.NIGHT->Color(0xFF071018);ReaderTheme.PAPER->Color(0xFFF5F0E6);ReaderTheme.SEPIA->Color(0xFFE9D6B4);ReaderTheme.OLED->Color.Black}; val fg=if(theme==ReaderTheme.NIGHT||theme==ReaderTheme.OLED) Color(0xFFF2F4F7) else Color(0xFF241D18)
    Scaffold(containerColor=bg,contentWindowInsets=WindowInsets.safeDrawing,topBar={ Surface(color=bg){Row(Modifier.fillMaxWidth().padding(5.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onClose){Icon(Icons.Default.ArrowBack,null,tint=fg)};Column(Modifier.weight(1f)){Text(book.title,color=fg,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${book.type} • ${book.progress}%",color=fg.copy(alpha=.6f),fontSize=10.sp)};IconButton(onClick={book=book.copy(bookmarks=if(book.position in book.bookmarks) book.bookmarks-book.position else book.bookmarks+book.position);library.update(book)}){Icon(if(book.position in book.bookmarks)Icons.Default.Bookmark else Icons.Default.BookmarkBorder,null,tint=NztAccent)};IconButton(onClick={settings=!settings}){Icon(Icons.Default.TextFields,null,tint=fg)}}}},bottomBar={if(settings) Surface(color=if(theme==ReaderTheme.OLED)Color.Black else NztSurface){Column(Modifier.navigationBarsPadding().padding(12.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ReaderTheme.entries.forEach{t->FilterChip(selected=t==theme,onClick={theme=t},label={Text(t.name)})}};Row(verticalAlignment=Alignment.CenterVertically){Text("A−",color=fg);Slider(font.toFloat(),{font=it.roundToInt()},valueRange=12f..30f,modifier=Modifier.weight(1f));Text("A+",color=fg)}}}}){pad-> Box(Modifier.padding(pad).fillMaxSize().background(bg)){when(book.type){"PDF"->V10Pdf(book,library,{book=it},fg);"CBZ","CBR"->V10Comic(book,library,{book=it},fg);"DJVU"->V10UnsupportedDjvu(fg);else->V10Text(book,library,{book=it},font,fg)}}}
}

@Composable private fun V10Text(initial:V10Book,library:V10Library,onUpdate:(V10Book)->Unit,font:Int,fg:Color){ var text by remember { mutableStateOf("") }; var loading by remember{mutableStateOf(true)}; LaunchedEffect(initial.path){text=loadV10Text(File(initial.path),initial.type);loading=false}; val pages=remember(text){if(text.isBlank()) listOf("") else text.chunked(3200)}; var page by remember{mutableIntStateOf(initial.position.coerceIn(0,(pages.size-1).coerceAtLeast(0)))}; LaunchedEffect(page,pages.size){val b=initial.copy(position=page,progress=(((page+1)*100f)/pages.size.coerceAtLeast(1)).roundToInt().coerceIn(0,100));library.update(b);onUpdate(b)}; if(loading) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()} else Column(Modifier.fillMaxSize()){Text(pages[page],Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=14.dp),color=fg,fontSize=font.sp,lineHeight=(font*1.55).sp);Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){IconButton(onClick={page=(page-1).coerceAtLeast(0)},enabled=page>0){Icon(Icons.Default.ChevronLeft,null,tint=fg)};Text("${page+1} / ${pages.size}",color=fg.copy(alpha=.7f));IconButton(onClick={page=(page+1).coerceAtMost(pages.size-1)},enabled=page<pages.size-1){Icon(Icons.Default.ChevronRight,null,tint=fg)}}} }

@Composable private fun V10Pdf(initial:V10Book,library:V10Library,onUpdate:(V10Book)->Unit,fg:Color){val file=remember{File(initial.path)};val count=remember{pdfCount10(file)};var page by remember{mutableIntStateOf(initial.position.coerceIn(0,(count-1).coerceAtLeast(0)))};var bmp by remember{mutableStateOf<Bitmap?>(null)};LaunchedEffect(page){bmp=renderPdf10(file,page);val b=initial.copy(position=page,progress=if(count>0)(((page+1)*100f)/count).roundToInt() else 0);library.update(b);onUpdate(b)};Column(Modifier.fillMaxSize()){Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),contentAlignment=Alignment.TopCenter){bmp?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxWidth(),contentScale=ContentScale.FillWidth)}?:CircularProgressIndicator(Modifier.padding(40.dp))};ReaderPager(page,count,{page=it},fg)} }

@Composable private fun V10Comic(initial:V10Book,library:V10Library,onUpdate:(V10Book)->Unit,fg:Color){val pages=remember(initial.path){comicPages(File(initial.path),initial.type)};var page by remember{mutableIntStateOf(initial.position.coerceIn(0,(pages.size-1).coerceAtLeast(0)))};val bmp=remember(page,pages){pages.getOrNull(page)?.let{BitmapFactory.decodeFile(it.absolutePath)}};LaunchedEffect(page){val b=initial.copy(position=page,progress=if(pages.isNotEmpty())(((page+1)*100f)/pages.size).roundToInt() else 0);library.update(b);onUpdate(b)};Column(Modifier.fillMaxSize()){Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),contentAlignment=Alignment.TopCenter){bmp?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxWidth(),contentScale=ContentScale.FillWidth)}?:Text("No image pages",color=fg,modifier=Modifier.padding(30.dp))};ReaderPager(page,pages.size,{page=it},fg)} }
@Composable private fun ReaderPager(page:Int,count:Int,set:(Int)->Unit,fg:Color){Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){IconButton(onClick={set((page-1).coerceAtLeast(0))},enabled=page>0){Icon(Icons.Default.ChevronLeft,null,tint=fg)};Text("${page+1} / ${count.coerceAtLeast(1)}",color=fg);IconButton(onClick={set((page+1).coerceAtMost((count-1).coerceAtLeast(0)))},enabled=page<count-1){Icon(Icons.Default.ChevronRight,null,tint=fg)}}}
@Composable private fun V10UnsupportedDjvu(fg:Color){Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.AutoStories,null,tint=NztAccent,modifier=Modifier.size(56.dp));Spacer(Modifier.height(14.dp));Text("DJVU",color=fg,fontSize=26.sp,fontWeight=FontWeight.Black);Text("DJVU is accepted by the library. Rendering support is being prepared for the next reader engine update.",color=fg.copy(alpha=.7f),modifier=Modifier.padding(top=8.dp))}}

private fun loadV10Text(file:File,type:String):String=runCatching{when(type){"TXT"->decodeText(file.readBytes());"HTML","HTM"->stripHtml(decodeText(file.readBytes()));"RTF"->parseRtf(decodeText(file.readBytes()));"EPUB"->zipText(file,setOf("html","htm","xhtml"));"FB2"->stripFb2(decodeText(file.readBytes()));"FB2.ZIP"->ZipFile(file).use{z->val e=z.entries().toList().firstOrNull{it.name.endsWith(".fb2",true)}?:return@use "";stripFb2(decodeText(z.getInputStream(e).readBytes()))};"MOBI","AZW","AZW3"->parseMobi(file.readBytes());"DOC"->extractDocText(file.readBytes());else->decodeText(file.readBytes())}}.getOrElse{"Unable to decode this book: ${it.message ?: "unknown error"}"}
private fun decodeText(b:ByteArray):String{if(b.isEmpty())return "";return runCatching{b.toString(Charsets.UTF_8)}.getOrElse{b.toString(Charset.forName("windows-1251"))}}
private fun stripHtml(s:String)=Html.fromHtml(s,Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\n{3,}"),"\n\n").trim()
private fun stripFb2(s:String):String=stripHtml(s.replace(Regex("<binary[\\s\\S]*?</binary>",RegexOption.IGNORE_CASE),"").replace("<section", "<p><section",true).replace("</section>","</section></p>",true))
private fun zipText(file:File,exts:Set<String>):String{val out=mutableListOf<String>();ZipFile(file).use{z->z.entries().toList().filter{!it.isDirectory&&it.name.substringAfterLast('.').lowercase() in exts}.sortedBy{it.name}.forEach{e->val t=stripHtml(decodeText(z.getInputStream(e).readBytes()));if(t.isNotBlank())out+=t}};return out.joinToString("\n\n")}
private fun parseRtf(s:String):String{s.replace(Regex("\\\\'([0-9a-fA-F]{2})")){m->m.groupValues[1].toInt(16).toChar().toString()}.let{r->return r.replace(Regex("\\\\par[d]? ?"),"\n").replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"),"").replace("{","").replace("}","").replace(Regex("\n{3,}"),"\n\n").trim()}}
private fun extractDocText(b:ByteArray):String{val utf16=runCatching{String(b,Charsets.UTF_16LE)}.getOrDefault("");val runs=Regex("[\\p{L}\\p{N}\\p{Punct} \\t\\r\\n]{20,}").findAll(utf16).map{it.value}.toList();if(runs.joinToString().length>200)return runs.joinToString("\n");return b.map{(it.toInt() and 0xff).toChar()}.joinToString("").replace(Regex("[^\\p{L}\\p{N}\\p{Punct} \\r\\n\\t]")," ").replace(Regex(" {3,}")," ")}
private fun parseMobi(data:ByteArray):String{if(data.size<100)return "";val records=((data[76].toInt()and255)shl8)or(data[77].toInt()and255);if(records<2)return "";fun off(i:Int):Int{val p=78+i*8;if(p+3>=data.size)return data.size;return ((data[p].toInt()and255)shl24)or((data[p+1].toInt()and255)shl16)or((data[p+2].toInt()and255)shl8)or(data[p+3].toInt()and255)};val r0=off(0);if(r0+16>data.size)return "";val comp=((data[r0].toInt()and255)shl8)or(data[r0+1].toInt()and255);val textRecords=((data[r0+8].toInt()and255)shl8)or(data[r0+9].toInt()and255);val out=ByteArrayOutputStream();for(i in 1..textRecords.coerceAtMost(records-1)){val a=off(i).coerceIn(0,data.size);val z=if(i+1<records)off(i+1).coerceIn(a,data.size)else data.size;val rec=data.copyOfRange(a,z);out.write(if(comp==2)palmDoc(rec)else rec)};return stripHtml(decodeText(out.toByteArray()))}
private fun palmDoc(src:ByteArray):ByteArray{val out=ByteArrayOutputStream();var i=0;while(i<src.size){val c=src[i].toInt()and255;i++;when{c==0->out.write(0);c in 1..8->{repeat(c){if(i<src.size)out.write(src[i++].toInt())}};c in 9..127->out.write(c);c in 128..191&&i<src.size->{val c2=src[i++].toInt()and255;val v=(c shl8)or c2;val distance=(v shr3)and0x7ff;val length=(v and7)+3;val buf=out.toByteArray();repeat(length){val idx=buf.size-distance+(it%distance.coerceAtLeast(1));if(idx in buf.indices)out.write(buf[idx].toInt())}};c>=192->{out.write(' '.code);out.write((c xor 0x80))}}};return out.toByteArray()}
private fun pdfCount10(f:File)=runCatching{ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY).use{p->PdfRenderer(p).use{it.pageCount}}}.getOrDefault(0)
private fun renderPdf10(f:File,index:Int):Bitmap?=runCatching{ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY).use{p->PdfRenderer(p).use{r->r.openPage(index.coerceIn(0,r.pageCount-1)).use{pg->val w=1400;val h=(w*pg.height.toFloat()/pg.width).roundToInt();Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also{it.eraseColor(AndroidColor.WHITE);pg.render(it,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)}}}}}.getOrNull()
private fun comicPages(f:File,type:String):List<File>{val dir=File(f.parentFile,"${f.nameWithoutExtension}_pages").apply{mkdirs()};if(dir.listFiles()?.isNotEmpty()==true)return dir.listFiles()!!.filter{it.extension.lowercase()in setOf("jpg","jpeg","png","webp","bmp","gif")}.sortedBy{it.name};runCatching{if(type=="CBZ")ZipFile(f).use{z->z.entries().toList().filter{!it.isDirectory&&it.name.substringAfterLast('.').lowercase()in setOf("jpg","jpeg","png","webp","bmp","gif")}.forEachIndexed{i,e->File(dir,"%05d.%s".format(i,e.name.substringAfterLast('.'))).outputStream().use{o->z.getInputStream(e).use{it.copyTo(o)}}}}else Junrar.extract(f,dir)};return dir.walkTopDown().filter{it.isFile&&it.extension.lowercase()in setOf("jpg","jpeg","png","webp","bmp","gif")}.sortedBy{it.name}.toList()}
private fun v10t(l:AppLanguage,ru:String,en:String,pl:String,uk:String)=when(l){AppLanguage.RU->ru;AppLanguage.EN->en;AppLanguage.PL->pl;AppLanguage.UK->uk}
