package com.nzt365.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

class V12WorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides profile.language()) {
                NZTProTheme { V12WorkoutScreen(date) { finish() } }
            }
        }
    }
}

@Composable
private fun V12WorkoutScreen(date: LocalDate, onClose: () -> Unit) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val store = remember { V12WorkoutStore(context) }
    val plan = remember(date) { StathamEngine.forDate(date) }
    var tick by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    var finish by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(1000); elapsed++ } }

    val all = plan.session.exercises.flatMap { ex ->
        (0 until ex.sets.coerceAtLeast(1)).map { i -> store.load(date, ex.name, i, v12DefaultTarget(ex.target)) }
    }
    val done = all.count { it.done }
    val total = all.size
    val volume = all.filter { it.done }.sumOf(::v12Volume)
    val best = all.filter { it.done }.maxOfOrNull(::v12E1rm) ?: 0.0
    val progress = if (total == 0) 0f else done.toFloat() / total

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = NztBg) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                        Column(Modifier.weight(1f)) {
                            Text("NZT PERFORMANCE", color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(plan.session.title, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                            Text(v12Time(elapsed), Modifier.padding(12.dp, 8.dp), color = NztAccent, fontWeight = FontWeight.Black)
                        }
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp), color = NztAccent, trackColor = NztLine)
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF08121A)) {
                Button(
                    onClick = { finish = true },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Flag, null)
                    Spacer(Modifier.width(8.dp))
                    Text(v12Text(lang, "ЗАВЕРШИТЬ", "FINISH", "ZAKOŃCZ", "ЗАВЕРШИТИ"), fontWeight = FontWeight.Black)
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { V12Summary(done, total, volume, best, lang) }
            item { V12Coach(lang) }
            itemsIndexed(plan.session.exercises) { index, ex ->
                V12ExerciseCard(date, index, ex, plan.session, store, tick) { tick++ }
            }
        }
    }

    if (finish) {
        AlertDialog(
            onDismissRequest = { finish = false },
            containerColor = NztSurface,
            title = { Text(v12Text(lang, "ТРЕНИРОВКА ЗАВЕРШЕНА", "WORKOUT COMPLETE", "TRENING ZAKOŃCZONY", "ТРЕНУВАННЯ ЗАВЕРШЕНО"), fontWeight = FontWeight.Black) },
            text = { Text("$done / $total • ${(progress * 100).roundToInt()}%\n${v12Text(lang, "Объём", "Volume", "Objętość", "Обсяг")}: ${volume.roundToInt()} kg") },
            confirmButton = {
                Button(onClick = {
                    plan.session.exercises.forEach { ex ->
                        val states = (0 until ex.sets.coerceAtLeast(1)).map { i -> store.load(date, ex.name, i, v12DefaultTarget(ex.target)) }
                        store.finishExercise(ex.name, states)
                    }
                    store.markWorkout(date, plan.session.code)
                    onClose()
                }) { Text(v12Text(lang, "ГОТОВО", "DONE", "GOTOWE", "ГОТОВО")) }
            },
            dismissButton = { TextButton(onClick = { finish = false }) { Text(v12Text(lang, "НАЗАД", "BACK", "WSTECZ", "НАЗАД")) } }
        )
    }
}

@Composable
private fun V12Summary(done: Int, total: Int, volume: Double, best: Double, lang: AppLanguage) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        V12Metric(v12Text(lang, "ПОДХОДЫ", "SETS", "SERIE", "ПІДХОДИ"), "$done/$total", Modifier.weight(1f))
        V12Metric(v12Text(lang, "ОБЪЁМ", "VOLUME", "OBJĘTOŚĆ", "ОБСЯГ"), if (volume > 0) "${volume.roundToInt()} kg" else "—", Modifier.weight(1f))
        V12Metric("e1RM", v12Kg(best), Modifier.weight(1f))
    }
}

@Composable
private fun V12Coach(lang: AppLanguage) {
    Surface(color = Color(0xFF10222A), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, NztLine)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = NztAccent)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("NZT COACH", color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(v12Text(lang,
                    "Для кардио фиксируй время и выполнение. Для силовых — вес, повторы и RIR.",
                    "Cardio: log time and completion. Strength: weight, reps and RIR.",
                    "Cardio: zapisuj czas i wykonanie. Siła: ciężar, powtórzenia i RIR.",
                    "Кардіо: фіксуй час і виконання. Силові: вага, повтори й RIR."),
                    color = NztMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun V12ExerciseCard(
    date: LocalDate,
    index: Int,
    ex: StathamExercise,
    session: StathamSession,
    store: V12WorkoutStore,
    tick: Int,
    onRefresh: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val cardio = v12IsCardio(ex, session)
    val states = remember(tick, date, ex.name) {
        (0 until ex.sets.coerceAtLeast(1)).map { i -> store.load(date, ex.name, i, v12DefaultTarget(ex.target)) }
    }
    var info by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumbnail(ex.name, Modifier.size(94.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}. ${ex.name}", fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(ex.target, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(exerciseMediaLabel(ex.name), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                    Text("${states.count { it.done }}/${states.size}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontWeight = FontWeight.Black)
                }
            }
            HorizontalDivider(color = NztLine.copy(alpha = .55f))
            Column(Modifier.padding(14.dp)) {
                if (cardio) {
                    Surface(color = Color(0xFF10252B), shape = RoundedCornerShape(14.dp)) {
                        Text(v12Text(lang,
                            "Кардио-режим: фактическое время без полей веса и e1RM.",
                            "Cardio mode: actual time without weight/e1RM fields.",
                            "Tryb cardio: rzeczywisty czas bez pól ciężaru i e1RM.",
                            "Кардіо-режим: фактичний час без полів ваги та e1RM."),
                            Modifier.padding(11.dp), color = NztAccent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V12Small(v12Text(lang, "ПРОШЛЫЙ", "PREVIOUS", "POPRZEDNIO", "ПОПЕРЕДНІЙ"), store.previous(ex.name), Modifier.weight(1f))
                        V12Small("BEST e1RM", v12Kg(store.bestE1rm(ex.name)), Modifier.weight(1f))
                        V12Small(v12Text(lang, "ОБЪЁМ", "VOLUME", "OBJĘTOŚĆ", "ОБСЯГ"), v12Kg(store.bestVolume(ex.name)), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                states.forEachIndexed { i, state ->
                    V12SetRow(state, i, cardio, lang) { next -> store.save(date, ex.name, i, next); onRefresh() }
                    Spacer(Modifier.height(7.dp))
                }
                Surface(color = Color(0xFF10252B), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = NztAccent2, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (cardio) v12Text(lang,
                            "NEXT: ровная интенсивность, длительность увеличивай постепенно.",
                            "NEXT: steady intensity, build duration gradually.",
                            "NEXT: równa intensywność, stopniowo wydłużaj czas.",
                            "NEXT: рівна інтенсивність, тривалість збільшуй поступово.") else ex.progression,
                            color = NztAccent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = { info = !info }) { Text(v12Text(lang, "ТЕХНИКА И ПЛАН", "FORM & PLAN", "TECHNIKA I PLAN", "ТЕХНІКА І ПЛАН")) }
                if (info) {
                    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(v12Text(lang, "ТЕХНИКА", "TECHNIQUE", "TECHNIKA", "ТЕХНІКА"), color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(ex.technique, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V12SetRow(state: V12SetState, index: Int, cardio: Boolean, lang: AppLanguage, onChange: (V12SetState) -> Unit) {
    var cur by remember(state, index) { mutableStateOf(state) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(Modifier.size(38.dp), color = NztSurface2, shape = CircleShape, border = BorderStroke(1.dp, NztLine)) {
            Box(contentAlignment = Alignment.Center) { Text((index + 1).toString(), fontWeight = FontWeight.Black) }
        }
        if (cardio) {
            V12Number(cur.reps, v12Text(lang, "мин", "min", "min", "хв"), Modifier.weight(1f)) { cur = cur.copy(reps = it); onChange(cur) }
        } else {
            V12Number(cur.kg, "kg", Modifier.weight(1f)) { cur = cur.copy(kg = it); onChange(cur) }
            V12Number(cur.reps, v12Text(lang, "повт.", "reps", "powt.", "повт."), Modifier.weight(1f)) { cur = cur.copy(reps = it); onChange(cur) }
            Surface(Modifier.width(56.dp).height(48.dp).clickable { cur = cur.copy(rir = (cur.rir + 1) % 6); onChange(cur) }, color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("RIR ${cur.rir}", fontSize = 10.sp, color = NztAccent2, fontWeight = FontWeight.Black) }
            }
        }
        FilledIconButton(onClick = { cur = cur.copy(done = !cur.done); onChange(cur) }, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (cur.done) NztAccent else NztSurface2, contentColor = if (cur.done) Color.Black else NztText)) {
            Icon(if (cur.done) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, null)
        }
    }
}

@Composable
private fun V12Number(value: String, hint: String, modifier: Modifier, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValue(raw.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
        modifier = modifier.height(50.dp),
        placeholder = { Text(hint, fontSize = 10.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun V12Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun V12Small(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(9.dp)) {
            Text(label, color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun v12Time(sec: Int) = String.format("%02d:%02d", sec / 60, sec % 60)
