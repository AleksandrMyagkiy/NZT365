package com.nzt365.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
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

data class V3Book(val number:Int,val title:String,val author:String,val purpose:String)

val v3Books = listOf(
    V3Book(1,"Never Split the Difference","Chris Voss","Переговоры и влияние"),
    V3Book(2,"Atomic Habits","James Clear","Системы и привычки"),
    V3Book(3,"The Psychology of Money","Morgan Housel","Деньги и мышление"),
    V3Book(4,"Influence","Robert Cialdini","Психология убеждения"),
    V3Book(5,"Deep Work","Cal Newport","Концентрация и продуктивность"),
    V3Book(6,"The Effective Executive","Peter Drucker","Рост до руководителя"),
    V3Book(7,"Good to Great","Jim Collins","Системное управление"),
    V3Book(8,"Principles","Ray Dalio","Система принятия решений"),
    V3Book(9,"Thinking, Fast and Slow","Daniel Kahneman","Мышление и когнитивные ошибки"),
    V3Book(10,"The 48 Laws of Power","Robert Greene","Влияние, статус и власть"),
    V3Book(11,"Antifragile","Nassim Nicholas Taleb","Устойчивость и неопределённость"),
    V3Book(12,"The Intelligent Investor","Benjamin Graham","Капитал и инвестиционная дисциплина")
)

private class V3BookStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_books_v3", Context.MODE_PRIVATE)
    fun progress(n:Int)=p.getInt("progress_$n",0)
    fun note(n:Int)=p.getString("note_$n","") ?: ""
    fun idea(n:Int)=p.getString("idea_$n","") ?: ""
    fun save(n:Int,progress:Int,note:String,idea:String)=p.edit().putInt("progress_$n",progress.coerceIn(0,100)).putString("note_$n",note).putString("idea_$n",idea).apply()
}

@Composable
fun V3BooksScreen(lang: AppLanguage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { V3BookStore(context) }
    var selected by remember { mutableStateOf<V3Book?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val completed = remember(refresh) { v3Books.count { store.progress(it.number) >= 100 } }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(V3Strings.t(lang,"booksSubtitle").uppercase(), color=NztMuted, fontSize=12.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp)
                Text("$completed / 12", color=NztAccent, fontSize=28.sp, fontWeight=FontWeight.Black)
            }
            Icon(Icons.Default.Book, null, tint=NztAccent, modifier=Modifier.size(32.dp))
        }
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp, vertical=8.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(v3Books, key={it.number}) { book ->
                val p = store.progress(book.number)
                Card(
                    modifier=Modifier.fillMaxWidth().clickable { selected=book },
                    colors=CardDefaults.cardColors(containerColor=NztSurface),
                    shape=RoundedCornerShape(22.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                        Surface(color=NztSurface2, shape=RoundedCornerShape(16.dp)) {
                            Box(Modifier.size(58.dp), contentAlignment=Alignment.Center) {
                                Text("${book.number}", color=NztAccent, fontSize=20.sp, fontWeight=FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(book.title, fontWeight=FontWeight.Bold, fontSize=17.sp, maxLines=2, overflow=TextOverflow.Ellipsis)
                            Text(book.author, color=NztMuted, fontSize=13.sp)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress={p/100f}, modifier=Modifier.fillMaxWidth().height(6.dp), color=NztAccent, trackColor=NztLine)
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(color=if(p>=100) NztAccent else Color(0xFF5B5268), shape=RoundedCornerShape(16.dp)) {
                            Box(Modifier.width(72.dp).height(52.dp), contentAlignment=Alignment.Center) {
                                if(p>=100) Icon(Icons.Default.Check,null,tint=Color.Black) else Text("$p%", fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    selected?.let { book ->
        V3BookDialog(lang, book, store.progress(book.number), store.note(book.number), store.idea(book.number), onDismiss={selected=null}) { p,n,i ->
            store.save(book.number,p,n,i); refresh++; selected=null
        }
    }
}

@Composable
private fun V3BookDialog(lang:AppLanguage, book:V3Book, initial:Int, initialNote:String, initialIdea:String, onDismiss:()->Unit, onSave:(Int,String,String)->Unit) {
    var progress by remember { mutableIntStateOf(initial) }
    var note by remember { mutableStateOf(initialNote) }
    var idea by remember { mutableStateOf(initialIdea) }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={ Column { Text(book.title, fontWeight=FontWeight.Black); Text(book.author, color=NztMuted, fontSize=13.sp) } },
        text={
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(book.purpose, color=NztAccent)
                Text("${V3Strings.t(lang,"bookProgress")}: $progress%", fontWeight=FontWeight.Bold)
                Slider(value=progress.toFloat(), onValueChange={progress=(it/5).toInt()*5}, valueRange=0f..100f, steps=19)
                OutlinedTextField(value=idea,onValueChange={idea=it},label={Text(V3Strings.t(lang,"appliedIdea"))},modifier=Modifier.fillMaxWidth(),minLines=2)
                OutlinedTextField(value=note,onValueChange={note=it},label={Text(V3Strings.t(lang,"note"))},modifier=Modifier.fillMaxWidth(),minLines=2)
                OutlinedButton(onClick={progress=100},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Check,null); Spacer(Modifier.width(8.dp)); Text(V3Strings.t(lang,"markRead")) }
            }
        },
        confirmButton={ Button(onClick={onSave(progress,note,idea)}) { Text(V3Strings.t(lang,"save")) } },
        dismissButton={ TextButton(onClick=onDismiss) { Text(V3Strings.t(lang,"close")) } },
        containerColor=NztSurface
    )
}
