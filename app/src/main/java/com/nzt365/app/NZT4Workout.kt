package com.nzt365.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

private data class NZT4SetLog(
    val weight: String = "",
    val reps: String = "",
    val rir: Int = 2,
    val done: Boolean = false
)

private fun wq(lang: AppLanguage, ru: String, en: String, pl: String, uk: String) = when (lang) {
    AppLanguage.RU -> ru
    AppLanguage.EN -> en
    AppLanguage.PL -> pl
    AppLanguage.UK -> uk
}

private class NZT4WorkoutStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_fit_v4", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("nzt_fit_v21", Context.MODE_PRIVATE)

    fun replacement(date: LocalDate): String? = legacy.getString("replacement_$date", null)

    private fun key(date: LocalDate, workout: String, exercise: String) = "sets_${date}_${workout}_${exercise.hashCode()}"

    fun sets(date: LocalDate, workout: String, exercise: String, count: Int): List<NZT4SetLog> {
        val raw = p.getString(key(date, workout, exercise), null)
        if (raw == null) {
            val prev = previous(exercise)
            return List(count) { prev?.copy(done = false) ?: NZT4SetLog() }
        }
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<NZT4SetLog>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out += NZT4SetLog(o.optString("weight"), o.optString("reps"), o.optInt("rir", 2), o.optBoolean("done", false))
            }
            while (out.size < count) out += NZT4SetLog()
            out.take(count)
        } catch (_: Exception) { List(count) { NZT4SetLog() } }
    }

    fun saveSets(date: LocalDate, workout: String, exercise: String, sets: List<NZT4SetLog>) {
        val arr = JSONArray()
        sets.forEach { s -> arr.put(JSONObject().apply { put("weight", s.weight); put("reps", s.reps); put("rir", s.rir); put("done", s.done) }) }
        p.edit().putString(key(date, workout, exercise), arr.toString()).apply()
    }

    fun previous(exercise: String): NZT4SetLog? {
        val raw = p.getString("previous_${exercise.hashCode()}", null) ?: return null
        return try {
            val o = JSONObject(raw)
            NZT4SetLog(o.optString("weight"), o.optString("reps"), o.optInt("rir", 2), false)
        } catch (_: Exception) { null }
    }

    fun savePrevious(exercise: String, set: NZT4SetLog) {
        p.edit().putString("previous_${exercise.hashCode()}", JSONObject().apply {
            put("weight", set.weight); put("reps", set.reps); put("rir", set.rir)
        }.toString()).apply()
    }

    fun addCompletedWorkout(date: LocalDate, id: String, volume: Int, sets: Int) {
        p.edit().putBoolean("workout_${date}_$id", true).putInt("volume_$date", volume).putInt("sets_$date", sets).apply()
    }
}

private fun altSession(id: String?): StathamSession? = when (id) {
    "home" -> StathamSession("HOME", "Дом — всё тело", "Силовая", 42, listOf(
        StathamExercise("Push-up", 4, "8–15", 75, "Корпус одной линией, грудь опускается под контролем.", "Когда 4×15 чисто — усложнить вариант или добавить резинку."),
        StathamExercise("Split squat", 4, "8–12 / нога", 75, "Полная стопа, стабильное колено, контролируемая глубина.", "После 4×12 перейти к болгарскому варианту или добавить вес."),
        StathamExercise("Band row", 4, "10–15", 75, "Плечи вниз, лопатки сводятся, корпус не раскачивается.", "На 4×15 увеличить сопротивление."),
        StathamExercise("Plank", 3, "30–60 сек", 45, "Рёбра вниз, ягодицы активны, поясница нейтральна.", "После 60 сек усложнить вариант.")
    ))
    "bars" -> StathamSession("BARS", "Турник + брусья", "Силовая", 50, listOf(
        StathamExercise("Pull-up", 5, "4–10", 120, "Полная контролируемая амплитуда, без рывков.", "5×10 — добавить вес или уменьшить помощь."),
        StathamExercise("Dip", 4, "6–12", 100, "Плечи стабильны, глубина без боли.", "4×12 — добавить внешний вес."),
        StathamExercise("Hanging knee raise", 4, "8–15", 75, "Таз подкручивается, без раскачки.", "4×15 — прямые ноги."),
        StathamExercise("Australian row", 4, "8–15", 75, "Грудь к перекладине, тело жёсткое.", "4×15 — поднять ноги.")
    ))
    "dumbbells" -> StathamSession("DB", "Гантели — всё тело", "Силовая", 55, listOf(
        StathamExercise("Dumbbell floor press", 4, "8–12", 90, "Лопатки собраны, опускание под контролем.", "4×12 — +1–2 кг на гантель."),
        StathamExercise("One-arm dumbbell row", 4, "8–12 / сторона", 90, "Корпус стабилен, тяга к тазу.", "4×12 — +1–2 кг."),
        StathamExercise("Goblet squat", 4, "8–15", 90, "Корпус жёсткий, полная стопа, глубина под контролем.", "4×15 — тяжелее гантель."),
        StathamExercise("Dumbbell curl", 3, "10–15", 60, "Локоть стабилен, без раскачки.", "3×15 — +1 кг.")
    ))
    "barbell" -> StathamSession("BB", "Штанга — сила", "Силовая", 60, listOf(
        StathamExercise("Barbell squat", 4, "5–8", 150, "Жёсткий корпус, стабильные колени, глубина под контролем.", "4×8 при RIR 2 — +2.5 кг."),
        StathamExercise("Bench press", 4, "5–8", 150, "Лопатки зафиксированы, касание под контролем.", "4×8 при RIR 2 — +2.5 кг."),
        StathamExercise("Barbell row", 4, "6–10", 120, "Нейтральная спина, тяга к нижним рёбрам.", "4×10 — +2.5 кг."),
        StathamExercise("Romanian deadlift", 3, "6–10", 150, "Таз назад, штанга близко к ногам.", "3×10 — +2.5–5 кг.")
    ))
    "run" -> StathamSession("RUN", "Бег — аэробная база", "Бег", 40, listOf(
        StathamExercise("Easy run", 1, "30 мин Z2", 0, "Разговорный темп, спокойное дыхание.", "При хорошем восстановлении +5 минут."),
        StathamExercise("Strides", 6, "20 сек", 60, "Быстро, но расслабленно, с полным восстановлением.", "Дойти до 8 повторов."),
        StathamExercise("Cooldown walk", 1, "5–10 мин", 0, "Плавно снизить пульс.", "Не ускорять.")
    ))
    "bike" -> StathamSession("BIKE", "Велосипед — аэробная база", "Велосипед", 55, listOf(
        StathamExercise("Cycling Z2", 1, "45–60 мин", 0, "Ровный аэробный темп и плавный каденс.", "Сначала +5–10 минут, затем интенсивность."),
        StathamExercise("High cadence", 5, "60 сек", 60, "100–110 rpm без раскачки таза.", "Добавить один интервал при хорошем контроле.")
    ))
    "mobility" -> StathamSession("MOB", "Мобильность + корпус", "Восстановление", 30, listOf(
        StathamExercise("Mobility flow", 1, "12 мин", 0, "Безболезненная контролируемая амплитуда.", "Увеличивать контроль, не интенсивность."),
        StathamExercise("Dead bug", 3, "8–10 / сторона", 45, "Поясница мягко прижата, движение медленное.", "Удлинить рычаг или замедлить темп."),
        StathamExercise("Bird-dog", 3, "8 / сторона", 45, "Таз не разворачивать.", "Пауза 3 секунды в конечной позиции.")
    ))
    else -> null
}

class NZT4WorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        val repo = NZTRepository(this)
        setContent {
            NZTProTheme {
                NZT4WorkoutScreen(date, profile.language(), repo) { finish() }
            }
        }
    }
}

@Composable
private fun NZT4WorkoutScreen(date: LocalDate, lang: AppLanguage, repo: NZTRepository, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { NZT4WorkoutStore(context) }
    val original = remember(date) { StathamEngine.forDate(date) }
    val replacement = remember(date) { store.replacement(date) }
    val session = remember(date, replacement) { altSession(replacement) ?: original.session }
    val workoutId = session.code
    var refresh by remember { mutableIntStateOf(0) }
    var rest by remember { mutableIntStateOf(0) }
    var restRunning by remember { mutableStateOf(false) }
    var finishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(restRunning, rest) {
        if (restRunning && rest > 0) { delay(1000); rest-- }
        else if (restRunning) restRunning = false
    }

    val states = remember(refresh) {
        session.exercises.map { ex -> store.sets(date, workoutId, ex.name, ex.sets) }
    }
    val totalSets = states.sumOf { it.size }
    val doneSets = states.sumOf { list -> list.count { it.done } }
    val volume = states.flatten().filter { it.done }.sumOf { (it.weight.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }.roundToInt()
    val progress = if (totalSets == 0) 0f else doneSets.toFloat() / totalSets

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = NztBg) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                    Column(Modifier.weight(1f)) {
                        Text("NZT TRAINING", color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text(session.title, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                        Text("$doneSets/$totalSets", Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = NztAccent, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF09131C), modifier = Modifier.navigationBarsPadding()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp), color = NztAccent, trackColor = NztLine)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { finishDialog = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                        Text(wq(lang,"Завершить тренировку","Finish workout","Zakończ trening","Завершити тренування"), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) { pad ->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutMetric(Icons.Default.Timer, "${session.minutes} min", Modifier.weight(1f))
                    WorkoutMetric(Icons.Default.CheckCircle, "$doneSets/$totalSets", Modifier.weight(1f))
                    WorkoutMetric(Icons.Default.FitnessCenter, "$volume kg", Modifier.weight(1f))
                }
            }
            items(session.exercises.size) { index ->
                NZT4ExerciseLogger(
                    ex = session.exercises[index],
                    date = date,
                    workoutId = workoutId,
                    index = index,
                    store = store,
                    lang = lang,
                    refresh = refresh,
                    onChanged = { refresh++ },
                    onCompleted = { seconds -> if (seconds > 0) { rest = seconds; restRunning = true }; refresh++ }
                )
            }
            item { Spacer(Modifier.height(10.dp)) }
        }
    }

    if (restRunning) {
        ModalBottomSheet(onDismissRequest = { restRunning = false }, containerColor = NztSurface) {
            Column(Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(wq(lang,"ОТДЫХ","REST","PRZERWA","ВІДПОЧИНОК"), color = NztMuted, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("$rest", color = NztAccent, fontSize = 70.sp, fontWeight = FontWeight.Black)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { rest = (rest - 15).coerceAtLeast(0) }, modifier = Modifier.weight(1f)) { Text("-15") }
                    OutlinedButton(onClick = { rest += 15 }, modifier = Modifier.weight(1f)) { Text("+15") }
                    Button(onClick = { restRunning = false }, modifier = Modifier.weight(1f)) { Text(wq(lang,"Пропустить","Skip","Pomiń","Пропустити")) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (finishDialog) {
        AlertDialog(
            onDismissRequest = { finishDialog = false },
            title = { Text(wq(lang,"Тренировка завершена?","Finish workout?","Zakończyć trening?","Завершити тренування?")) },
            text = { Text("$doneSets / $totalSets ${wq(lang,"подходов","sets","serii","підходів")} • $volume kg") },
            confirmButton = {
                Button(onClick = {
                    session.exercises.forEach { ex ->
                        val done = store.sets(date, workoutId, ex.name, ex.sets).lastOrNull { it.done }
                        if (done != null) store.savePrevious(ex.name, done)
                    }
                    store.addCompletedWorkout(date, workoutId, volume, doneSets)
                    if (date == LocalDate.now()) {
                        val tasks = repo.tasksForToday().map { if (it.id == "body") it.copy(done = it.target) else it }
                        repo.saveTasks(tasks)
                    }
                    onClose()
                }) { Text(wq(lang,"Сохранить","Save","Zapisz","Зберегти")) }
            },
            dismissButton = { TextButton(onClick = { finishDialog = false }) { Text(wq(lang,"Продолжить","Continue","Kontynuuj","Продовжити")) } },
            containerColor = NztSurface
        )
    }
}

@Composable
private fun WorkoutMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = NztSurface, shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NztAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NZT4ExerciseLogger(
    ex: StathamExercise,
    date: LocalDate,
    workoutId: String,
    index: Int,
    store: NZT4WorkoutStore,
    lang: AppLanguage,
    refresh: Int,
    onChanged: () -> Unit,
    onCompleted: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val initial = remember(refresh, date, workoutId, ex.name) { store.sets(date, workoutId, ex.name, ex.sets) }
    var sets by remember(refresh, date, workoutId, ex.name) { mutableStateOf(initial) }
    val previous = store.previous(ex.name)

    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
        Column {
            Box {
                NZT4ExercisePhoto(ex.name, Modifier.fillMaxWidth().height(190.dp), 24)
                Box(Modifier.fillMaxWidth().height(190.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6071018)))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text("${ex.sets} × ${ex.target}  •  ${ex.restSeconds}s", color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(ex.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(ex.technique, color = NztMuted, fontSize = 12.sp, lineHeight = 18.sp)
                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (expanded) wq(lang,"Скрыть подсказку","Hide coaching","Ukryj wskazówkę","Сховати підказку") else "+ ${wq(lang,"Прогрессия","Progression","Progresja","Прогресія")}")
                }
                if (expanded) {
                    Surface(color = NztSurface2, shape = RoundedCornerShape(13.dp)) {
                        Text(ex.progression, Modifier.fillMaxWidth().padding(11.dp), color = NztAccent, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (previous != null && (previous.weight.isNotBlank() || previous.reps.isNotBlank())) {
                    Text(
                        "${wq(lang,"Прошлый результат","Previous","Poprzednio","Минулого разу")}: ${previous.weight.ifBlank { "—" }} kg × ${previous.reps.ifBlank { "—" }} • RIR ${previous.rir}",
                        color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TableHead("#", Modifier.width(34.dp))
                    TableHead("KG", Modifier.weight(1f))
                    TableHead(wq(lang,"ПОВТ","REPS","POWT","ПОВТ"), Modifier.weight(1f))
                    TableHead("RIR", Modifier.width(58.dp))
                    Spacer(Modifier.width(48.dp))
                }
                Spacer(Modifier.height(5.dp))

                sets.forEachIndexed { setIndex, set ->
                    SetInputRow(
                        number = setIndex + 1,
                        state = set,
                        lang = lang,
                        onState = { next ->
                            sets = sets.toMutableList().also { it[setIndex] = next }
                            store.saveSets(date, workoutId, ex.name, sets)
                            onChanged()
                        },
                        onDone = { next ->
                            sets = sets.toMutableList().also { it[setIndex] = next }
                            store.saveSets(date, workoutId, ex.name, sets)
                            if (next.done && !set.done) onCompleted(ex.restSeconds) else onChanged()
                        }
                    )
                    if (setIndex < sets.lastIndex) Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun TableHead(text: String, modifier: Modifier) {
    Text(text, modifier, color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
}

@Composable
private fun SetInputRow(number: Int, state: NZT4SetLog, lang: AppLanguage, onState: (NZT4SetLog) -> Unit, onDone: (NZT4SetLog) -> Unit) {
    val bg = if (state.done) Color(0xFF152920) else NztSurface2
    Surface(color = bg, shape = RoundedCornerShape(14.dp), border = if (state.done) BorderStroke(1.dp, Color(0x5539E6A5)) else null) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { Text("$number", fontWeight = FontWeight.Black, color = if (state.done) NztAccent else NztText) }
            CompactNumberField(state.weight, { onState(state.copy(weight = it)) }, Modifier.weight(1f), "0")
            Spacer(Modifier.width(5.dp))
            CompactNumberField(state.reps, { onState(state.copy(reps = it.filter(Char::isDigit))) }, Modifier.weight(1f), "0")
            Spacer(Modifier.width(5.dp))
            Box(Modifier.width(56.dp).horizontalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                TextButton(onClick = { onState(state.copy(rir = (state.rir + 1) % 6)) }, contentPadding = PaddingValues(0.dp)) { Text("${state.rir}", fontWeight = FontWeight.Black) }
            }
            IconButton(onClick = { onDone(state.copy(done = !state.done)) }, modifier = Modifier.size(44.dp)) {
                Icon(if (state.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (state.done) NztAccent else NztMuted)
            }
        }
    }
}

@Composable
private fun CompactNumberField(value: String, onValue: (String) -> Unit, modifier: Modifier, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValue(raw.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.').take(6)) },
        modifier = modifier.height(48.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold),
        placeholder = { Text(placeholder, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = NztMuted) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(11.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    )
}
