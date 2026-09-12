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

    private fun key(date: LocalDate, exercise: String, set: Int, field: String): String =
        "${date}_${exercise.hashCode()}_${set}_$field"

    fun load(date: LocalDate, exercise: String, set: Int, defaultReps: Int): V7SetState {
        val typeName = p.getString(key(date, exercise, set, "type"), V7SetType.NORMAL.name)
        return V7SetState(
            kg = p.getString(key(date, exercise, set, "kg"), previousKg(exercise, set)) ?: "",
            reps = p.getString(key(date, exercise, set, "reps"), previousReps(exercise, set).ifBlank { defaultReps.toString() }) ?: defaultReps.toString(),
            rir = p.getInt(key(date, exercise, set, "rir"), 2),
            done = p.getBoolean(key(date, exercise, set, "done"), false),
            type = runCatching { V7SetType.valueOf(typeName ?: V7SetType.NORMAL.name) }.getOrDefault(V7SetType.NORMAL)
        )
    }

    fun save(date: LocalDate, exercise: String, set: Int, state: V7SetState) {
        p.edit()
            .putString(key(date, exercise, set, "kg"), state.kg)
            .putString(key(date, exercise, set, "reps"), state.reps)
            .putInt(key(date, exercise, set, "rir"), state.rir)
            .putBoolean(key(date, exercise, set, "done"), state.done)
            .putString(key(date, exercise, set, "type"), state.type.name)
            .apply()
    }

    fun extraSets(exercise: String): Int = p.getInt("extra_${exercise.hashCode()}", 0)
    fun setExtraSets(exercise: String, value: Int) {
        p.edit().putInt("extra_${exercise.hashCode()}", value.coerceIn(-2, 4)).apply()
    }

    fun previousKg(exercise: String, set: Int): String = p.getString("prevkg_${exercise.hashCode()}_$set", "") ?: ""
    fun previousReps(exercise: String, set: Int): String = p.getString("prevreps_${exercise.hashCode()}_$set", "") ?: ""
    fun bestE1rm(exercise: String): Double = p.getFloat("e1rm_${exercise.hashCode()}", 0f).toDouble()
    fun bestVolume(exercise: String): Double = p.getFloat("vol_${exercise.hashCode()}", 0f).toDouble()
    fun note(exercise: String): String = p.getString("note_${exercise.hashCode()}", "") ?: ""
    fun setNote(exercise: String, value: String) { p.edit().putString("note_${exercise.hashCode()}", value).apply() }

    fun finishExercise(exercise: String, states: List<V7SetState>) {
        states.forEachIndexed { index, state ->
            if (state.done) {
                p.edit()
                    .putString("prevkg_${exercise.hashCode()}_$index", state.kg)
                    .putString("prevreps_${exercise.hashCode()}_$index", state.reps)
                    .apply()
            }
        }
        val e1rm = states.filter { it.done }.maxOfOrNull { estimated1rm(it) } ?: 0.0
        val volume = states.filter { it.done }.sumOf { setVolume(it) }
        if (e1rm > bestE1rm(exercise)) p.edit().putFloat("e1rm_${exercise.hashCode()}", e1rm.toFloat()).apply()
        if (volume > bestVolume(exercise)) p.edit().putFloat("vol_${exercise.hashCode()}", volume.toFloat()).apply()
    }

    fun markWorkout(date: LocalDate, code: String) {
        val count = p.getInt("workout_count", 0) + 1
        p.edit().putBoolean("workout_${date}_$code", true).putInt("workout_count", count).apply()
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
    val store = remember { V7WorkoutStore(context) }
    val plan = remember(date) { StathamEngine.forDate(date) }
    var refresh by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    var restSeconds by remember { mutableIntStateOf(0) }
    var showRest by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsed++
        }
    }
    LaunchedEffect(showRest, restSeconds) {
        if (showRest && restSeconds > 0) {
            delay(1000)
            restSeconds--
        } else if (showRest && restSeconds <= 0) {
            showRest = false
        }
    }

    val allStates = plan.session.exercises.flatMap { exercise ->
        val count = (exercise.sets + store.extraSets(exercise.name)).coerceAtLeast(1)
        (0 until count).map { index -> store.load(date, exercise.name, index, defaultReps(exercise.target)) }
    }
    val totalSets = allStates.size
    val doneSets = allStates.count { it.done }
    val volume = allStates.filter { it.done }.sumOf { setVolume(it) }
    val e1rm = allStates.filter { it.done }.maxOfOrNull { estimated1rm(it) } ?: 0.0
    val progress = if (totalSets == 0) 0f else doneSets.toFloat() / totalSets

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = NztBg) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                        Column(Modifier.weight(1f)) {
                            Text("NZT PERFORMANCE", color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                            Text(plan.session.title, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                            Text(formatTime(elapsed), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = NztAccent, fontWeight = FontWeight.Black)
                        }
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp), color = NztAccent, trackColor = NztLine)
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF08121A)) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { restSeconds = 90; showRest = true },
                        modifier = Modifier.weight(.38f).height(54.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Icon(Icons.Default.Timer, null)
                        Spacer(Modifier.width(6.dp))
                        Text("ОТДЫХ")
                    }
                    Button(
                        onClick = { showFinish = true },
                        modifier = Modifier.weight(.62f).height(54.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Icon(Icons.Default.Flag, null)
                        Spacer(Modifier.width(8.dp))
                        Text("ЗАВЕРШИТЬ", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V7WorkoutMetric("SETS", "$doneSets/$totalSets", Modifier.weight(1f))
                    V7WorkoutMetric("VOLUME", if (volume > 0) "${volume.roundToInt()} kg" else "—", Modifier.weight(1f))
                    V7WorkoutMetric("e1RM", if (e1rm > 0) "${e1rm.roundToInt()} kg" else "—", Modifier.weight(1f))
                }
            }
            item {
                Surface(color = Color(0xFF10202B), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NztLine)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = NztAccent)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("NZT COACH", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Text("Чистая техника, каждый подход в журнале и 1–3 RIR в базовых упражнениях. Прогрессируй только после качественного выполнения плана.", color = NztMuted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
            itemsIndexed(plan.session.exercises) { index, exercise ->
                V7ExerciseCard(
                    date = date,
                    index = index,
                    exercise = exercise,
                    store = store,
                    refresh = refresh,
                    onRefresh = { refresh++ },
                    onSetCompleted = { seconds ->
                        restSeconds = seconds
                        showRest = seconds > 0
                        refresh++
                    }
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (showRest) {
        ModalBottomSheet(onDismissRequest = { showRest = false }, containerColor = NztSurface) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ТАЙМЕР ОТДЫХА", color = NztMuted, fontWeight = FontWeight.Bold)
                Text(formatTime(restSeconds), fontSize = 64.sp, color = NztAccent, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { restSeconds = (restSeconds - 15).coerceAtLeast(0) }, modifier = Modifier.weight(1f)) { Text("−15") }
                    OutlinedButton(onClick = { restSeconds += 15 }, modifier = Modifier.weight(1f)) { Text("+15") }
                    Button(onClick = { showRest = false }, modifier = Modifier.weight(1f)) { Text("ПРОПУСТИТЬ") }
                }
            }
        }
    }

    if (showFinish) {
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text("ТРЕНИРОВКА ЗАВЕРШЕНА", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$doneSets / $totalSets sets • ${(progress * 100).roundToInt()}%")
                    Text("Объём: ${volume.roundToInt()} kg")
                    Text("Лучший расчётный 1RM: ${e1rm.roundToInt()} kg")
                    Text("Результаты подходов и личные рекорды будут сохранены.", color = NztMuted)
                }
            },
            confirmButton = {
                Button(onClick = {
                    plan.session.exercises.forEach { exercise ->
                        val count = (exercise.sets + store.extraSets(exercise.name)).coerceAtLeast(1)
                        val states = (0 until count).map { idx -> store.load(date, exercise.name, idx, defaultReps(exercise.target)) }
                        store.finishExercise(exercise.name, states)
                    }
                    store.markWorkout(date, plan.session.code)
                    onClose()
                }) { Text("ГОТОВО") }
            },
            dismissButton = { TextButton(onClick = { showFinish = false }) { Text("НАЗАД") } },
            containerColor = NztSurface
        )
    }
}

@Composable
private fun V7WorkoutMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun V7ExerciseCard(
    date: LocalDate,
    index: Int,
    exercise: StathamExercise,
    store: V7WorkoutStore,
    refresh: Int,
    onRefresh: () -> Unit,
    onSetCompleted: (Int) -> Unit
) {
    val defaultReps = defaultReps(exercise.target)
    val setCount = (exercise.sets + store.extraSets(exercise.name)).coerceAtLeast(1)
    val states = remember(refresh, date, exercise.name, setCount) {
        (0 until setCount).map { set -> store.load(date, exercise.name, set, defaultReps) }
    }
    val currentE1rm = states.filter { it.done }.maxOfOrNull { estimated1rm(it) } ?: 0.0
    val isPr = currentE1rm > store.bestE1rm(exercise.name) && currentE1rm > 0
    var showInfo by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showPlateCalc by remember { mutableStateOf(false) }
    var showWarmup by remember { mutableStateOf(false) }
    var note by remember(exercise.name) { mutableStateOf(store.note(exercise.name)) }

    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.size(92.dp)) {
                    ExerciseThumbnail(exercise.name, Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("УПРАЖНЕНИЕ ${index + 1}", color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(exercise.name, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Text("${exercise.target} • отдых ${exercise.restSeconds} с", color = NztMuted, fontSize = 11.sp)
                }
                Surface(
                    color = if (isPr) NztAccent else NztSurface2,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isPr) "PR" else "${states.count { it.done }}/$setCount",
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = if (isPr) Color.Black else NztText,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
            HorizontalDivider(color = NztLine.copy(alpha = .65f))
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V7InfoPill("PREVIOUS", previousSummary(store, exercise.name), Modifier.weight(1f))
                    V7InfoPill("BEST e1RM", store.bestE1rm(exercise.name).takeIf { it > 0 }?.let { "${it.roundToInt()} kg" } ?: "—", Modifier.weight(1f))
                    V7InfoPill("BEST VOL", store.bestVolume(exercise.name).takeIf { it > 0 }?.roundToInt()?.toString() ?: "—", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                V7SetHeader()
                states.forEachIndexed { setIndex, state ->
                    V7SetRow(
                        state = state,
                        setIndex = setIndex,
                        onState = { next, justCompleted ->
                            store.save(date, exercise.name, setIndex, next)
                            onRefresh()
                            if (justCompleted) onSetCompleted(exercise.restSeconds)
                        }
                    )
                    Spacer(Modifier.height(7.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { store.setExtraSets(exercise.name, store.extraSets(exercise.name) - 1); onRefresh() },
                        modifier = Modifier.weight(1f),
                        enabled = setCount > 1
                    ) { Text("− SET") }
                    OutlinedButton(
                        onClick = { store.setExtraSets(exercise.name, store.extraSets(exercise.name) + 1); onRefresh() },
                        modifier = Modifier.weight(1f)
                    ) { Text("+ SET") }
                    OutlinedButton(onClick = { showPlateCalc = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Calculate, null)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(color = Color(0xFF10252B), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = NztAccent2, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(progressionSuggestion(states), color = NztAccent2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showInfo = !showInfo }, modifier = Modifier.weight(1f)) { Text("FORM") }
                    TextButton(onClick = { showNote = !showNote }, modifier = Modifier.weight(1f)) { Text("NOTE") }
                    TextButton(onClick = { showWarmup = true }, modifier = Modifier.weight(1f)) { Text("WARMUP") }
                }
                if (showInfo) {
                    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("TECHNIQUE", color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(exercise.technique, fontSize = 13.sp, lineHeight = 19.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("PROGRESSION", color = NztAccent2, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(exercise.progression, color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
                if (showNote) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(220) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Exercise note") },
                        minLines = 2
                    )
                    TextButton(onClick = { store.setNote(exercise.name, note) }) { Text("SAVE NOTE") }
                }
            }
        }
    }

    if (showPlateCalc) V7PlateCalculator { showPlateCalc = false }
    if (showWarmup) {
        val workKg = states.firstOrNull()?.kg?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        V7WarmupDialog(workKg) { showWarmup = false }
    }
}

@Composable
private fun V7SetHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp)) {
        Text("SET", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(43.dp))
        Text("KG", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("REPS", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("RIR", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
        Spacer(Modifier.width(46.dp))
    }
}

@Composable
private fun V7SetRow(state: V7SetState, setIndex: Int, onState: (V7SetState, Boolean) -> Unit) {
    var current by remember(state, setIndex) { mutableStateOf(state) }
    var menu by remember { mutableStateOf(false) }
    val typeColor = when (current.type) {
        V7SetType.NORMAL -> NztMuted
        V7SetType.WARMUP -> Color(0xFFFFC857)
        V7SetType.DROP -> Color(0xFF70D6FF)
        V7SetType.FAILURE -> Color(0xFFFF7D7D)
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(43.dp)) {
            Surface(
                Modifier.size(34.dp).clickable { menu = true },
                color = NztSurface2,
                shape = CircleShape,
                border = BorderStroke(1.dp, typeColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text((setIndex + 1).toString(), color = typeColor, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                V7SetType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            current = current.copy(type = type)
                            onState(current, false)
                            menu = false
                        }
                    )
                }
            }
        }
        V7NumberField(current.kg, "kg", Modifier.weight(1f)) {
            current = current.copy(kg = it)
            onState(current, false)
        }
        V7NumberField(current.reps, "reps", Modifier.weight(1f)) {
            current = current.copy(reps = it)
            onState(current, false)
        }
        Surface(
            Modifier.width(58.dp).height(46.dp).clickable {
                current = current.copy(rir = (current.rir + 1) % 6)
                onState(current, false)
            },
            color = NztSurface2,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(current.rir.toString(), color = if (current.rir >= 2) NztAccent2 else Color(0xFFFFC857), fontWeight = FontWeight.Black)
            }
        }
        FilledIconButton(
            onClick = {
                val wasDone = current.done
                current = current.copy(done = !current.done)
                onState(current, !wasDone && current.done)
            },
            modifier = Modifier.size(42.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (current.done) NztAccent else NztSurface2,
                contentColor = if (current.done) Color.Black else NztText
            )
        ) {
            Icon(if (current.done) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun V7NumberField(value: String, placeholder: String, modifier: Modifier, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw: String ->
            onValue(raw.filter { ch: Char -> ch.isDigit() || ch == '.' || ch == ',' }.take(6))
        },
        modifier = modifier.height(50.dp),
        placeholder = { Text(placeholder, fontSize = 10.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
    )
}

@Composable
private fun V7InfoPill(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(9.dp)) {
            Text(label, color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun V7PlateCalculator(onDismiss: () -> Unit) {
    var target by remember { mutableStateOf("60") }
    var bar by remember { mutableStateOf("20") }
    val targetKg = target.replace(',', '.').toDoubleOrNull() ?: 0.0
    val barKg = bar.replace(',', '.').toDoubleOrNull() ?: 20.0
    val perSide = ((targetKg - barKg) / 2).coerceAtLeast(0.0)
    val plates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    var remaining = perSide
    val result = mutableListOf<String>()
    plates.forEach { plate ->
        val count = (remaining / plate).toInt()
        if (count > 0) {
            result += "${count}×${plate} kg"
            remaining -= count * plate
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PLATE CALCULATOR", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(target, { target = it }, label = { Text("Target total kg") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(bar, { bar = it }, label = { Text("Bar kg") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text("Per side: ${if (result.isEmpty()) "—" else result.joinToString(" + ")}", color = NztAccent, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("DONE") } },
        containerColor = NztSurface
    )
}

@Composable
private fun V7WarmupDialog(workKg: Double, onDismiss: () -> Unit) {
    val warmups = if (workKg <= 0) {
        listOf("Bodyweight / empty bar", "Technique rehearsal")
    } else {
        listOf(
            "${(workKg * .4).roundToInt()} kg × 8",
            "${(workKg * .6).roundToInt()} kg × 5",
            "${(workKg * .75).roundToInt()} kg × 3",
            "${(workKg * .9).roundToInt()} kg × 1"
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMART WARM-UP", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                warmups.forEachIndexed { index, text ->
                    Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
                        Text("${index + 1}. $text", Modifier.fillMaxWidth().padding(11.dp))
                    }
                }
                Text("Warm-up sets should stay far from failure.", color = NztMuted, fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("READY") } },
        containerColor = NztSurface
    )
}

private fun estimated1rm(state: V7SetState): Double {
    val kg = state.kg.replace(',', '.').toDoubleOrNull() ?: 0.0
    val reps = state.reps.toIntOrNull() ?: 0
    return if (kg <= 0 || reps <= 0) 0.0 else kg * (1.0 + reps / 30.0)
}

private fun setVolume(state: V7SetState): Double {
    val kg = state.kg.replace(',', '.').toDoubleOrNull() ?: 0.0
    val reps = state.reps.toIntOrNull() ?: 0
    return kg * reps
}

private fun progressionSuggestion(states: List<V7SetState>): String {
    val done = states.filter { it.done }
    if (done.isEmpty()) return "ДАЛЬШЕ: выполни назначенный объём с чистой техникой"
    val averageRir = done.map { it.rir }.average()
    return when {
        done.size == states.size && averageRir >= 3.0 -> "ДАЛЬШЕ: увеличь нагрузку на 2–5% или выбери более сложный вариант"
        done.size == states.size && averageRir >= 1.5 -> "ДАЛЬШЕ: сохрани вес и сначала добавь повторения"
        averageRir < 1.0 -> "ДАЛЬШЕ: сохрани или снизь нагрузку, приоритет — техника и восстановление"
        else -> "ДАЛЬШЕ: выполни все рабочие подходы перед прогрессией"
    }
}

private fun previousSummary(store: V7WorkoutStore, exercise: String): String {
    val kg = store.previousKg(exercise, 0)
    val reps = store.previousReps(exercise, 0)
    return when {
        kg.isNotBlank() && reps.isNotBlank() -> "$kg × $reps"
        reps.isNotBlank() -> "BW × $reps"
        else -> "—"
    }
}

private fun defaultReps(target: String): Int = Regex("\\d+").find(target)?.value?.toIntOrNull()?.coerceIn(1, 30) ?: 8
private fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
