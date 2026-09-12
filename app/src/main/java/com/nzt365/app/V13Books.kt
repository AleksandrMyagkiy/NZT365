package com.nzt365.app

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

data class V13Goal(val number:Int,val title:String,val author:String,val bookId:String?)

private class V13GoalStore(context:Context){
    private val p=context.getSharedPreferences("nzt_goals_v13",Context.MODE_PRIVATE)
    fun goals():List<V13Goal>{
        val raw=p.getString("goals",null) ?: return v3Books.map{V13Goal(it.number,it.title,it.author,null)}
        return runCatching{val a=JSONArray(raw);(0 until a.length()).map{i->val o=a.getJSONObject(i);V13Goal(o.getInt("number"),o.getString("title"),o.optString("author"),o.optString("bookId").takeIf{it.isNotBlank()})}}.getOrElse{v3Books.map{V13Goal(it.number,it.title,it.author,null)}}
    }
    fun save(list:List<V13Goal>){val a=JSONArray();list.sortedBy{it.number}.forEach{g->a.put(JSONObject().apply{put("number",g.number);put("title",g.title);put("author",g.author);put("bookId",g.bookId?:"")})};p.edit().putString("goals",a.toString()).apply()}
}

@Composable fun V13BooksScreen(lang:AppLanguage,modifier:Modifier=Modifier){
    val context=LocalContext.current;val library=remember{V10Library(context)};val store=remember{V13GoalStore(context)}
    var tick by remember{mutableIntStateOf(0)};var goals by remember{mutableStateOf(store.goals())};var edit by remember{mutableStateOf<V13Goal?>(null)};var query by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)}
    val books=remember(tick,query){library.books().filter{query.isBlank()||it.title.contains(query,true)}}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){val b=library.import(uri);if(b==null)error=v13bt(lang,"Формат не распознан","Format not recognized","Nie rozpoznano formatu","Формат не розпізнано") else tick++}}
    LazyColumn(modifier,contentPadding=PaddingValues(20.dp,12.dp,20.dp,90.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("NZT READER",color=NztAccent,fontSize=11.sp,fontWeight=FontWeight.Black);Text(v13bt(lang,"Библиотека и цели","Library & goals","Biblioteka i cele","Бібліотека і цілі"),fontSize=28.sp,fontWeight=FontWeight.Black)};FilledIconButton(onClick={launcher.launch(arrayOf("*/*"))}){Icon(Icons.Default.Add,null)}}}
        item{OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text(v13bt(lang,"Поиск по библиотеке","Search library","Szukaj w bibliotece","Пошук у бібліотеці"))},shape=RoundedCornerShape(18.dp))}
        if(books.isNotEmpty()){
            item{Text(v13bt(lang,"МОЯ БИБЛИОТЕКА","MY LIBRARY","MOJA BIBLIOTEKA","МОЯ БІБЛІОТЕКА"),color=NztAccent,fontWeight=FontWeight.Black,fontSize=11.sp)}
            items(books,key={"lib_${it.id}"}){book->Card(Modifier.fillMaxWidth().clickable{context.startActivity(if(book.type in setOf("PDF","CBZ","CBR","DJVU"))V10ReaderActivity.intent(context,book) else V12ReaderActivity.intent(context,book))},colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Surface(color=NztSurface2,shape=RoundedCornerShape(14.dp)){Column(Modifier.size(64.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.MenuBook,null,tint=NztAccent);Text(book.type,fontSize=8.sp,color=NztMuted)}};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(book.title,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis);Text("${book.progress}%",color=NztAccent,fontSize=11.sp);LinearProgressIndicator(progress={book.progress/100f},modifier=Modifier.fillMaxWidth().height(5.dp),color=NztAccent,trackColor=NztLine)};IconButton(onClick={library.delete(book);goals=goals.map{if(it.bookId==book.id)it.copy(bookId=null) else it};store.save(goals);tick++}){Icon(Icons.Default.DeleteOutline,null,tint=NztMuted)}}}}
        }
        item{Text(v13bt(lang,"12 КНИГ / 12 МЕСЯЦЕВ · РЕДАКТИРУЕТСЯ","12 BOOKS / 12 MONTHS · EDITABLE","12 KSIĄŻEK / 12 MIESIĘCY · EDYCJA","12 КНИГ / 12 МІСЯЦІВ · РЕДАГУЄТЬСЯ"),color=NztAccent,fontWeight=FontWeight.Black,fontSize=11.sp)}
        items(goals,key={"goal_${it.number}"}){g->val linked=books.firstOrNull{it.id==g.bookId};Card(Modifier.fillMaxWidth().clickable{edit=g},colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Surface(color=NztSurface2,shape=RoundedCornerShape(14.dp)){Text(g.number.toString().padStart(2,'0'),Modifier.padding(14.dp),color=NztAccent,fontWeight=FontWeight.Black)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(g.title,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis);Text(g.author.ifBlank{v13bt(lang,"Автор не указан","No author","Brak autora","Автор не вказаний")},color=NztMuted,fontSize=12.sp);if(linked!=null){Spacer(Modifier.height(5.dp));AssistChip(onClick={context.startActivity(if(linked.type in setOf("PDF","CBZ","CBR","DJVU"))V10ReaderActivity.intent(context,linked) else V12ReaderActivity.intent(context,linked))},label={Text("📖 ${linked.title}",maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={Icon(Icons.Default.Link,null)})}else Text(v13bt(lang,"Файл книги не привязан","No file linked","Brak podpiętego pliku","Файл книги не прив’язаний"),color=NztMuted,fontSize=10.sp)};Icon(Icons.Default.Edit,null,tint=NztAccent)}}}
    }
    if(edit!=null){val current=edit!!;var title by remember(current){mutableStateOf(current.title)};var author by remember(current){mutableStateOf(current.author)};var selectedId by remember(current){mutableStateOf(current.bookId)};AlertDialog(onDismissRequest={edit=null},title={Text(v13bt(lang,"Цель чтения","Reading goal","Cel czytelniczy","Ціль читання"))},text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)){OutlinedTextField(title,{title=it},label={Text(v13bt(lang,"Название","Title","Tytuł","Назва"))});OutlinedTextField(author,{author=it},label={Text(v13bt(lang,"Автор","Author","Autor","Автор"))});Text(v13bt(lang,"Привязать скачанную книгу","Link downloaded book","Podepnij pobraną książkę","Прив’язати завантажену книгу"),color=NztMuted,fontSize=11.sp);FilterChip(selected=selectedId==null,onClick={selectedId=null},label={Text(v13bt(lang,"Без файла","No file","Bez pliku","Без файлу"))});books.forEach{b->FilterChip(selected=selectedId==b.id,onClick={selectedId=b.id},label={Text(b.title,maxLines=1,overflow=TextOverflow.Ellipsis)})}}},confirmButton={Button(onClick={goals=goals.map{if(it.number==current.number)it.copy(title=title.trim().ifBlank{current.title},author=author.trim(),bookId=selectedId)else it};store.save(goals);edit=null}){Text(v13bt(lang,"Сохранить","Save","Zapisz","Зберегти"))}},dismissButton={TextButton(onClick={edit=null}){Text(v13bt(lang,"Отмена","Cancel","Anuluj","Скасувати"))}},containerColor=NztSurface)}
    error?.let{AlertDialog(onDismissRequest={error=null},confirmButton={TextButton(onClick={error=null}){Text("OK")}},title={Text("NZT Reader")},text={Text(it)},containerColor=NztSurface)}
}

private fun v13bt(l:AppLanguage,ru:String,en:String,pl:String,uk:String)=when(l){AppLanguage.RU->ru;AppLanguage.EN->en;AppLanguage.PL->pl;AppLanguage.UK->uk}
