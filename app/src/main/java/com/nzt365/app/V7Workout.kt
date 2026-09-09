package com.nzt365.app

import android.content.Context
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

enum class V7SetType { NORMAL, WARMUP, DROP, FAILURE }

data class V7SetState(
    val kg: String = "",
    val reps: String = "",
    val rir: Int = 2,
    val done: Boolean = false,
    val type: V7SetType = V7SetType.NORMAL
)

private class V7WorkoutStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_workout_v7", Context.MODE_PRIVATE)
    private fun k(date: LocalDate, ex: String, set: Int, field: String) = "${date}_${ex.hashCode()}_${set}_$field"

    fun load(date: LocalDate, ex: String, set: Int, defaultReps: Int): V7SetState = V7SetState(
        kg = p.getString(k(date, ex, set, "kg"), previousKg(ex, set)) ?: "",
        reps = p.getString(k(date, ex, set, "reps"), previousReps(ex, set).ifBlank { defaultReps.toString() }) ?: defaultReps.toString(),
        rir = p.getInt(k(date, ex, set, "rir"), 2),
        done = p.getBoolean(k(date, ex, set, "done"), false),
        type = runCatching { V7SetType.valueOf(p.getString(k(date, ex, set, "type"), V7SetType.NORMAL.name) ?: V7SetType.NORMAL.name) }.getOrDefault(V7SetType.NORMAL)
    )

    fun save(date: LocalDate, ex: String, set: Int, s: V7SetState) {
        p.edit().putString(k(date, ex, set, "kg"), s.kg).putString(k(date, ex, set, "reps"), s.reps)
            .putInt(k(date, ex, set, "rir"), s.rir).putBoolean(k(date, ex, set, "done"), s.done)
            .putString(k(date, ex, set, "type"), s.type.name).apply()
    }

    fun extraSets(ex: String) = p.getInt("extra_${ex.hashCode()}", 0)
    fun setExtraSets(ex: String, v: Int) = p.edit().putInt("extra_${ex.hashCode()}", v.coerceIn(-2, 4)).apply()
    fun note(ex: String) = p.getString("note_${ex.hashCode()}", "") ?: ""
    fun setNote(ex: String, v: String) = p.edit().putString("note_${ex.hashCode()}", v).apply()
    fun previousKg(ex: String, set: Int) = p.getString("prevkg_${ex.hashCode()}_$set", "") ?: ""
    fun previousReps(ex: String, set: Int) = p.getString("prevreps_${ex.hashCode()}_$set", "") ?: ""
    fun bestE1rm(ex: String) = p.getFloat("e1rm_${ex.hashCode()}", 0f).toDouble()
    fun bestVolume(ex: String) = p.getFloat("vol_${ex.hashCode()}", 0f).toDouble()
    fun workoutCount() = p.getInt("workout_count", 0)

    fun finishExercise(ex: String, states: List<V7SetState>) {
        states.forEachIndexed { i, s ->
            if (s.done) p.edit().putString("prevkg_${ex.hashCode()}_$i", s.kg).putString("prevreps_${ex.hashCode()}_$i", s.reps).apply()
        }
        val e1rm = states.filter { it.done }.maxOfOrNull { estimated1RM(it) } ?: 0.0
        val volume = states.filter { it.done }.sumOf { (it.kg.replace(',', '.').toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }
        if (e1rm > bestE1rm(ex)) p.edit().putFloat("e1rm_${ex.hashCode()}", e1rm.toFloat()).apply()
        if (volume > bestVolume(ex)) p.edit().putFloat("vol_${ex.hashCode()}", volume.toFloat()).apply()
    }

    fun markWorkout(date: LocalDate, code: String) {
        p.edit().putBoolean("workout_${date}_$code", true).putInt("workout_count", workoutCount() + 1).apply()
        contextLegacy(date, code)
    }

    private fun contextLegacy(date: LocalDate, code: String) {
        // Keep compatibility with dashboard counters used by older NZT modules.
        p.edit().putBoolean("legacy_${date}_$code", true).apply()
    }
}

class V7WorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides profile.language()) {
                NZTProTheme { V7WorkoutScreen(date) { finish() } }
            }
        }
    }
}

@Composable
private fun V7WorkoutScreen(date: LocalDate, onClose: () -> Unit) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val store = remember { V7WorkoutStore(context) }
    val plan = remember(date) { StathamEngine.forDate(date) }
    var tick by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    var rest by remember { mutableIntStateOf(0) }
    var restOpen by remember { mutableStateOf(false) }
    var finishOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { while (true) { delay(1000); elapsed++ } }
    LaunchedEffect(restOpen, rest) {
        if (restOpen && rest > 0) { delay(1000); rest-- } else if (restOpen) restOpen = false
    }

    val allStates = plan.session.exercises.flatMap { ex ->
        val count = (ex.sets + store.extraSets(ex.name)).coerceAtLeast(1)
        (0 until count).map { store.load(date, ex.name, it, defaultReps7(ex.target)) }
    }
    val doneSets = allStates.count { it.done }
    val totalSets = allStates.size
    val totalVolume = allStates.filter { it.done }.sumOf { (it.kg.replace(',', '.').toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }
    val bestE1rm = allStates.filter { it.done }.maxOfOrNull { estimated1RM(it) } ?: 0.0
    val progress = if (totalSets == 0) 0f else doneSets.toFloat() / totalSets

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = NztBg) {
                Column(Modifier.statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                        Column(Modifier.weight(1f)) {
                            Text("NZT PERFORMANCE", color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                            Text(plan.session.title, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                            Text(formatTime7(elapsed), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = NztAccent, fontWeight = FontWeight.Black)
                        }
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp), color = NztAccent, trackColor = NztLine)
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF08121A)) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { rest = 90; restOpen = true }, modifier = Modifier.weight(0.38f).height(54.dp), shape = RoundedCornerShape(17.dp)) {
                        Icon(Icons.Default.Timer, null); Spacer(Modifier.width(6.dp)); Text("REST")
                    }
                    Button(onClick = { finishOpen = true }, modifier = Modifier.weight(0.62f).height(54.dp), shape = RoundedCornerShape(17.dp)) {
                        Icon(Icons.Default.Flag, null); Spacer(Modifier.width(8.dp)); Text(V3Strings.t(lang, "finish"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V7Metric("SETS", "$doneSets/$totalSets", Modifier.weight(1f))
                    V7Metric("VOLUME", if (totalVolume > 0) "${totalVolume.roundToInt()} kg" else "—", Modifier.weight(1f))
                    V7Metric("e1RM", if (bestE1rm > 0) "${bestE1rm.roundToInt()} kg" else "—", Modifier.weight(1f))
                }
            }
            item {
                Surface(color = Color(0xFF10202B), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NztLine)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = NztAccent)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("NZT COACH", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Text("Log clean sets. Keep 1–3 RIR on compounds. When all work sets are completed at 2+ RIR, progress next session.", color = NztMuted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
            itemsIndexed(plan.session.exercises) { index, ex ->
                V7ExerciseCard(date, index, ex, store, tick, onChanged = { tick++ }, onSetDone = { sec -> rest = sec; restOpen = sec > 0; tick++ })
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (restOpen) V7RestSheet(rest, onMinus = { rest = (rest - 15).coerceAtLeast(0) }, onPlus = { rest += 15 }, onDone = { restOpen = false })

    if (finishOpen) {
        AlertDialog(
            onDismissRequest = { finishOpen = false },
            title = { Text("WORKOUT COMPLETE", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("$doneSets / $totalSets sets • ${(progress * 100).roundToInt()}%")
                    Text("Volume: ${totalVolume.roundToInt()} kg")
                    Text("Best estimated 1RM: ${bestE1rm.roundToInt()} kg")
                    Text("The workout will update previous-set values, PRs and next-session targets.", color = NztMuted)
                }
            },
            confirmButton = {
                Button(onClick = {
                    plan.session.exercises.forEach { ex ->
                        val count = (ex.sets + store.extraSets(ex.name)).coerceAtLeast(1)
                        store.finishExercise(ex.name, (0 until count).map { store.load(date, ex.name, it, defaultReps7(ex.target)) })
                    }
                    store.markWorkout(date, plan.session.code)
                    onClose()
                }) { Text(V3Strings.t(lang, "done")) }
            },
            dismissButton = { TextButton(onClick = { finishOpen = false }) { Text(V3Strings.t(lang, "back")) } },
            containerColor = NztSurface
        )
    }
}

@Composable
private fun V7Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp)); Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun V7ExerciseCard(
    date: LocalDate,
    index: Int,
    ex: StathamExercise,
    store: V7WorkoutStore,
    tick: Int,
    onChanged: () -> Unit,
    onSetDone: (Int) -> Unit
) {
    val defaultReps = defaultReps7(ex.target)
    val setCount = (ex.sets + store.extraSets(ex.name)).coerceAtLeast(1)
    val states = remember(tick, date, ex.name, setCount) { (0 until setCount).map { store.load(date, ex.name, it, defaultReps) } }
    val done = states.count { it.done }
    val currentVol = states.filter { it.done }.sumOf { (it.kg.replace(',', '.').toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }
    val currentE1rm = states.filter { it.done }.maxOfOrNull { estimated1RM(it) } ?: 0.0
    val pr = currentE1rm > store.bestE1rm(ex.name) && currentE1rm > 0
    var infoOpen by remember { mutableStateOf(false) }
    var noteOpen by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(false) }
    var warmupOpen by remember { mutableStateOf(false) }
    var note by remember(ex.name) { mutableStateOf(store.note(ex.name)) }

    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Box {
                ExercisePhoto(ex.name, Modifier.fillMaxWidth().height(210.dp))
                Surface(Modifier.align(Alignment.TopEnd).padding(12.dp), color = if (pr) NztAccent else Color(0xCC10202B), shape = RoundedCornerShape(14.dp)) {
                    Text(if (pr) "NEW PR" else "$done/$setCount", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = if (pr) Color.Black else NztText, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text("${index + 1}. ${ex.name}", fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("${ex.target} • ${ex.restSeconds}s rest", color = NztMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V7InfoPill("PREVIOUS", previousSummary7(store, ex.name), Modifier.weight(1f))
                    V7InfoPill("BEST e1RM", if (store.bestE1rm(ex.name) > 0) "${store.bestE1rm(ex.name).roundToInt()} kg" else "—", Modifier.weight(1f))
                    V7InfoPill("VOLUME", if (currentVol > 0) currentVol.roundToInt().toString() else "—", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp)) {
                    Text("SET", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(43.dp))
                    Text("KG", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("REPS", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("RIR", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
                    Spacer(Modifier.width(46.dp))
                }
                Spacer(Modifier.height(5.dp))
                states.forEachIndexed { setIndex, initial ->
                    V7SetRow(date, ex, setIndex, initial, store) { newState, justDone ->
                        store.save(date, ex.name, setIndex, newState); onChanged(); if (justDone) onSetDone(ex.restSeconds)
                    }
                    Spacer(Modifier.height(7.dp))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { store.setExtraSets(ex.name, store.extraSets(ex.name) - 1); onChanged() }, modifier = Modifier.weight(1f), enabled = setCount > 1) { Text("− SET") }
                    OutlinedButton(onClick = { store.setExtraSets(ex.name, store.extraSets(ex.name) + 1); onChanged() }, modifier = Modifier.weight(1f)) { Text("+ SET") }
                    OutlinedButton(onClick = { toolsOpen = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Calculate, null) }
                }

                val suggestion = progressionSuggestion7(states)
                Spacer(Modifier.height(10.dp))
                Surface(color = Color(0xFF10252B), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = NztAccent2, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(suggestion, color = NztAccent2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { infoOpen = !infoOpen }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Info, null); Spacer(Modifier.width(5.dp)); Text("FORM") }
                    TextButton(onClick = { noteOpen = !noteOpen }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.EditNote, null); Spacer(Modifier.width(5.dp)); Text("NOTE") }
                    TextButton(onClick = { warmupOpen = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Whatshot, null); Spacer(Modifier.width(5.dp)); Text("WARMUP") }
                }

                if (infoOpen) {
                    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(12.dp)) {
                        Text("TECHNIQUE", color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(ex.technique, fontSize = 13.sp, lineHeight = 19.sp)
                        Spacer(Modifier.height(8.dp)); Text("PROGRESSION", color = NztAccent2, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(ex.progression, color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
                    } }
                }
                if (noteOpen) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(note, { note = it.take(220) }, modifier = Modifier.fillMaxWidth(), label = { Text("Exercise note") }, minLines = 2)
                    TextButton(onClick = { store.setNote(ex.name, note) }) { Text("SAVE NOTE") }
                }
            }
        }
    }

    if (toolsOpen) V7PlateCalculator(onDismiss = { toolsOpen = false })
    if (warmupOpen) V7WarmupDialog(states.firstOrNull()?.kg?.replace(',', '.')?.toDoubleOrNull() ?: 0.0, onDismiss = { warmupOpen = false })
}

@Composable
private fun V7SetRow(date: LocalDate, ex: StathamExercise, setIndex: Int, initial: V7SetState, store: V7WorkoutStore, onChanged: (V7SetState, Boolean) -> Unit) {
    var s by remember(initial, date, ex.name, setIndex) { mutableStateOf(initial) }
    var typeMenu by remember { mutableStateOf(false) }
    val typeColor = when (s.type) { V7SetType.NORMAL -> NztMuted; V7SetType.WARMUP -> Color(0xFFFFC857); V7SetType.DROP -> Color(0xFF70D6FF); V7SetType.FAILURE -> Color(0xFFFF7D7D) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(43.dp)) {
            Surface(Modifier.size(34.dp).clickable { typeMenu = true }, color = NztSurface2, shape = CircleShape, border = BorderStroke(1.dp, typeColor)) {
                Box(contentAlignment = Alignment.Center) { Text((setIndex + 1).toString(), color = typeColor, fontWeight = FontWeight.Black, fontSize = 11.sp) }
            }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                V7SetType.entries.forEach { t -> DropdownMenuItem(text = { Text(t.name) }, onClick = { s = s.copy(type = t); onChanged(s, false); typeMenu = false }) }
            }
        }
        V7NumberField(s.kg, "kg", Modifier.weight(1f)) { s = s.copy(kg = it); onChanged(s, false) }
        V7NumberField(s.reps, "reps", Modifier.weight(1f)) { s = s.copy(reps = it); onChanged(s, false) }
        Surface(Modifier.width(58.dp).height(46.dp).clickable { s = s.copy(rir = (s.rir + 1) % 6); onChanged(s, false) }, color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(s.rir.toString(), color = if (s.rir >= 2) NztAccent2 else Color(0xFFFFC857), fontWeight = FontWeight.Black) }
        }
        FilledIconButton(onClick = { val was = s.done; s = s.copy(done = !s.done); onChanged(s, !was && s.done) }, modifier = Modifier.size(42.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (s.done) NztAccent else NztSurface2, contentColor = if (s.done) Color.Black else NztText)) {
            Icon(if (s.done) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun V7NumberField(value: String, placeholder: String, modifier: Modifier, onValue: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { raw -> onValue(raw.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) }, modifier = modifier.height(50.dp), placeholder = { Text(placeholder, fontSize = 10.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp))
}

@Composable
private fun V7InfoPill(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(13.dp)) { Column(Modifier.padding(9.dp)) { Text(label, color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold); Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
}

@Composable
private fun V7RestSheet(seconds: Int, onMinus: () -> Unit, onPlus: () -> Unit, onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone, containerColor = NztSurface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REST TIMER", color = NztMuted, fontWeight = FontWeight.Bold); Text(formatTime7(seconds), fontSize = 64.sp, color = NztAccent, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMinus, modifier = Modifier.weight(1f)) { Text("−15") }
                OutlinedButton(onClick = onPlus, modifier = Modifier.weight(1f)) { Text("+15") }
                Button(onClick = onDone, modifier = Modifier.weight(1f)) { Text("SKIP") }
            }
        }
    }
}

@Composable
private fun V7PlateCalculator(onDismiss: () -> Unit) {
    var target by remember { mutableStateOf("60") }
    var bar by remember { mutableStateOf("20") }
    val targetKg = target.replace(',', '.').toDoubleOrNull() ?: 0.0
    val barKg = bar.replace(',', '.').toDoubleOrNull() ?: 20.0
    val side = ((targetKg - barKg) / 2).coerceAtLeast(0.0)
    val plates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    var remaining = side
    val result = buildList { plates.forEach { p -> val n = (remaining / p).toInt(); if (n > 0) { add("${n}×${p} kg"); remaining -= n * p } } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("PLATE CALCULATOR", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(target, { target = it }, label = { Text("Target total kg") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(bar, { bar = it }, label = { Text("Bar kg") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Text("Per side: ${if (result.isEmpty()) "—" else result.joinToString(" + ")}", color = NztAccent, fontWeight = FontWeight.Bold)
    } }, confirmButton = { Button(onClick = onDismiss) { Text("DONE") } }, containerColor = NztSurface)
}

@Composable
private fun V7WarmupDialog(workKg: Double, onDismiss: () -> Unit) {
    val base = workKg.coerceAtLeast(0.0)
    val warmups = if (base <= 0) listOf("Bodyweight / empty bar", "Technique rehearsal") else listOf("${(base * .4).roundToInt()} kg × 8", "${(base * .6).roundToInt()} kg × 5", "${(base * .75).roundToInt()} kg × 3", "${(base * .9).roundToInt()} kg × 1")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("SMART WARM-UP", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { warmups.forEachIndexed { i, w -> Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) { Text("${i + 1}. $w", Modifier.fillMaxWidth().padding(11.dp)) } }; Text("Warm-up sets do not need to approach failure.", color = NztMuted, fontSize = 12.sp) } }, confirmButton = { Button(onClick = onDismiss) { Text("READY") } }, containerColor = NztSurface)
}

private fun estimated1RM(s: V7SetState): Double {
    val kg = s.kg.replace(',', '.').toDoubleOrNull() ?: 0.0
    val reps = s.reps.toIntOrNull() ?: 0
    return if (kg <= 0 || reps <= 0) 0.0 else kg * (1.0 + reps / 30.0)
}

private fun progressionSuggestion7(states: List<V7SetState>): String {
    val done = states.filter { it.done }
    if (done.isEmpty()) return "NEXT: complete the prescribed work with clean technique"
    val avgRir = done.map { it.rir }.average()
    return when {
        done.size == states.size && avgRir >= 3.0 -> "NEXT: increase load 2–5% or use a harder variation"
        done.size == states.size && avgRir >= 1.5 -> "NEXT: keep load and add reps before increasing weight"
        avgRir < 1.0 -> "NEXT: keep or reduce load; preserve technique and recovery"
        else -> "NEXT: complete all work sets before progressing"
    }
}

private fun previousSummary7(store: V7WorkoutStore, ex: String): String {
    val kg = store.previousKg(ex, 0); val reps = store.previousReps(ex, 0)
    return when { kg.isNotBlank() && reps.isNotBlank() -> "$kg × $reps"; reps.isNotBlank() -> "BW × $reps"; else -> "—" }
}

private fun defaultReps7(target: String): Int = Regex("\\d+").find(target)?.value?.toIntOrNull()?.coerceIn(1, 30) ?: 8
private fun formatTime7(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
