package com.nzt365.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun V5BooksScreen(lang: AppLanguage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { V5BookStore(context) }
    var selected by remember { mutableStateOf<V3Book?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val completed = remember(refresh) { v3Books.count { store.progress(it.number) >= 100 } }
    val current = remember(refresh) { v3Books.firstOrNull { store.progress(it.number) < 100 } ?: v3Books.last() }
    val currentProgress = remember(refresh) { store.progress(current.number) }

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
                            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MenuBook, null, tint = NztAccent)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(v5BookText(lang, "current"), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(current.title, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(current.author, color = NztMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { currentProgress / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = NztAccent, trackColor = NztLine)
                    Spacer(Modifier.height(6.dp))
                    Text("$currentProgress%", color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

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
            onSave = { p, note, idea ->
                store.save(book.number, p, note, idea)
                refresh++
                selected = null
            }
        )
    }
}

@Composable
private fun V5BookCover(number: Int, title: String) {
    val palettes = listOf(
        listOf(Color(0xFF203B4B), Color(0xFF14232E)),
        listOf(Color(0xFF39412C), Color(0xFF1E281C)),
        listOf(Color(0xFF493B52), Color(0xFF292032)),
        listOf(Color(0xFF4A332E), Color(0xFF2A1F1B))
    )
    val colors = palettes[(number - 1) % palettes.size]
    Box(
        Modifier.width(62.dp).height(82.dp).background(Brush.linearGradient(colors), RoundedCornerShape(14.dp)).padding(8.dp)
    ) {
        Text(number.toString().padStart(2, '0'), color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(title.take(18), modifier = Modifier.align(Alignment.BottomStart), fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V5BookDialog(
    lang: AppLanguage,
    book: V3Book,
    initial: Int,
    initialNote: String,
    initialIdea: String,
    onDismiss: () -> Unit,
    onSave: (Int, String, String) -> Unit
) {
    var progress by remember { mutableIntStateOf(initial) }
    var note by remember { mutableStateOf(initialNote) }
    var idea by remember { mutableStateOf(initialIdea) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(book.title, fontWeight = FontWeight.Black)
                Text(book.author, color = NztMuted, fontSize = 13.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(v5Purpose(lang, book.number), color = NztAccent, fontSize = 13.sp)
                Text("${v5BookText(lang, "progress")}: $progress%", fontWeight = FontWeight.Bold)
                Slider(value = progress.toFloat(), onValueChange = { progress = ((it / 5f).toInt() * 5).coerceIn(0, 100) }, valueRange = 0f..100f)
                OutlinedTextField(idea, { idea = it }, label = { Text(v5BookText(lang, "idea")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(note, { note = it }, label = { Text(v5BookText(lang, "note")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedButton(onClick = { progress = 100 }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(v5BookText(lang, "read"))
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(progress, note.trim(), idea.trim()) }) { Text(v5BookText(lang, "save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v5BookText(lang, "close")) } },
        containerColor = NztSurface
    )
}

private fun v5BookText(lang: AppLanguage, key: String): String = when (lang) {
    AppLanguage.RU -> mapOf("year_plan" to "12 КНИГ / 12 МЕСЯЦЕВ", "current" to "СЕЙЧАС", "progress" to "Прогресс", "idea" to "Идея, которую внедряю", "note" to "Заметки", "read" to "Отметить прочитанной", "save" to "Сохранить", "close" to "Закрыть")[key] ?: key
    AppLanguage.EN -> mapOf("year_plan" to "12 BOOKS / 12 MONTHS", "current" to "CURRENT", "progress" to "Progress", "idea" to "Idea I will apply", "note" to "Notes", "read" to "Mark as read", "save" to "Save", "close" to "Close")[key] ?: key
    AppLanguage.PL -> mapOf("year_plan" to "12 KSIĄŻEK / 12 MIESIĘCY", "current" to "TERAZ", "progress" to "Postęp", "idea" to "Pomysł do wdrożenia", "note" to "Notatki", "read" to "Oznacz jako przeczytaną", "save" to "Zapisz", "close" to "Zamknij")[key] ?: key
    AppLanguage.UK -> mapOf("year_plan" to "12 КНИГ / 12 МІСЯЦІВ", "current" to "ЗАРАЗ", "progress" to "Прогрес", "idea" to "Ідея для впровадження", "note" to "Нотатки", "read" to "Позначити прочитаною", "save" to "Зберегти", "close" to "Закрити")[key] ?: key
}

private fun v5Purpose(lang: AppLanguage, n: Int): String {
    val ru = listOf("Переговоры и влияние", "Системы и привычки", "Деньги и мышление", "Психология убеждения", "Концентрация и продуктивность", "Рост до руководителя", "Системное управление", "Система принятия решений", "Мышление и когнитивные ошибки", "Влияние, статус и власть", "Устойчивость и неопределённость", "Капитал и инвестиционная дисциплина")
    val en = listOf("Negotiation and influence", "Systems and habits", "Money and mindset", "Psychology of persuasion", "Focus and productivity", "Executive effectiveness", "Systemic management", "Decision systems", "Thinking and cognitive bias", "Influence, status and power", "Resilience and uncertainty", "Capital and investment discipline")
    val pl = listOf("Negocjacje i wpływ", "Systemy i nawyki", "Pieniądze i myślenie", "Psychologia perswazji", "Koncentracja i produktywność", "Skuteczność menedżera", "Zarządzanie systemowe", "System podejmowania decyzji", "Myślenie i błędy poznawcze", "Wpływ, status i władza", "Odporność i niepewność", "Kapitał i dyscyplina inwestycyjna")
    val uk = listOf("Переговори та вплив", "Системи й звички", "Гроші та мислення", "Психологія переконання", "Концентрація та продуктивність", "Ефективність керівника", "Системне управління", "Система прийняття рішень", "Мислення та когнітивні помилки", "Вплив, статус і влада", "Стійкість і невизначеність", "Капітал та інвестиційна дисципліна")
    val list = when (lang) { AppLanguage.RU -> ru; AppLanguage.EN -> en; AppLanguage.PL -> pl; AppLanguage.UK -> uk }
    return list[(n - 1).coerceIn(0, 11)]
}
