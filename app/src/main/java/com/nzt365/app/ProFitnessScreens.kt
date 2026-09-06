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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

private data class ProExercise(
    val name: String,
    val sets: Int,
    val target: String,
    val rest: Int,
    val technique: String,
    val progression: String
)

private data class AltWorkout(
    val id: String,
    val titleKey: String,
    val code: String,
    val minutes: Int,
    val exercises: List<ProExercise>
)

private val alternateWorkouts = listOf(
    AltWorkout("home", "home", "HOME", 42, listOf(
        ProExercise("Push-up", 4, "8-15", 75, "Keep the body in one line and lower under control.", "When 4 x 15 is comfortable, use a harder variation or resistance band."),
        ProExercise("Split squat", 4, "8-12 / leg", 75, "Stable front foot, controlled knee path and full balance.", "At 4 x 12, move to Bulgarian split squat or add load."),
        ProExercise("Band row", 4, "10-15", 75, "Keep shoulders down and squeeze shoulder blades.", "At 4 x 15, increase band resistance."),
        ProExercise("Plank", 3, "30-60 sec", 45, "Ribs down, glutes active, no lumbar sag.", "At 60 sec, switch to a harder plank variation.")
    )),
    AltWorkout("bars", "pullup_bars", "BARS", 50, listOf(
        ProExercise("Pull-up", 5, "4-10", 120, "Use a full controlled range and avoid reaching with the neck.", "At 5 x 10, add weight or reduce assistance."),
        ProExercise("Dip", 4, "6-12", 100, "Keep shoulders stable and stay in a pain-free range.", "At 4 x 12, add external load."),
        ProExercise("Hanging knee raise", 4, "8-15", 75, "Control the pelvis and avoid swinging.", "At 4 x 15, progress to straight-leg raise."),
        ProExercise("Australian row", 4, "8-15", 75, "Chest toward the bar, body rigid.", "At 4 x 15, raise the feet.")
    )),
    AltWorkout("dumbbells", "dumbbells", "DB", 55, listOf(
        ProExercise("Dumbbell floor press", 4, "8-12", 90, "Pack the shoulders and control the eccentric phase.", "At 4 x 12, add 1-2 kg per dumbbell."),
        ProExercise("One-arm dumbbell row", 4, "8-12 / side", 90, "Keep the torso square and pull toward the hip.", "At 4 x 12, add 1-2 kg."),
        ProExercise("Goblet squat", 4, "8-15", 90, "Brace, keep whole-foot pressure and controlled depth.", "At 4 x 15, use a heavier dumbbell."),
        ProExercise("Dumbbell curl", 3, "10-15", 60, "Keep upper arm quiet and avoid body swing.", "At 3 x 15, add 1 kg.")
    )),
    AltWorkout("barbell", "barbell", "BB", 60, listOf(
        ProExercise("Barbell squat", 4, "5-8", 150, "Brace hard, use controlled depth and stable knee tracking.", "At 4 x 8 with 2 RIR, add 2.5 kg."),
        ProExercise("Bench press", 4, "5-8", 150, "Stable upper back, controlled touch and leg drive.", "At 4 x 8 with 2 RIR, add 2.5 kg."),
        ProExercise("Barbell row", 4, "6-10", 120, "Neutral spine and pull to the lower ribs.", "At 4 x 10, add 2.5 kg."),
        ProExercise("Romanian deadlift", 3, "6-10", 150, "Push hips back and keep the bar close to the legs.", "At 3 x 10, add 2.5-5 kg.")
    )),
    AltWorkout("run", "running", "RUN", 40, listOf(
        ProExercise("Easy run", 1, "30 min Z2", 0, "Use conversational pace and relaxed cadence.", "Add 5 minutes when recovery is good."),
        ProExercise("Strides", 6, "20 sec", 60, "Fast but relaxed with full recovery.", "Add one repetition up to 8."),
        ProExercise("Cooldown walk", 1, "5-10 min", 0, "Bring breathing down gradually.", "Keep this easy.")
    )),
    AltWorkout("bike", "cycling", "BIKE", 55, listOf(
        ProExercise("Cycling Z2", 1, "45-60 min", 0, "Steady aerobic pace with smooth pedaling.", "Add 5-10 minutes before increasing intensity."),
        ProExercise("High cadence", 5, "60 sec", 60, "Ride 100-110 rpm without bouncing.", "Add one interval when all reps feel controlled.")
    )),
    AltWorkout("mobility", "mobility", "MOB", 30, listOf(
        ProExercise("Mobility flow", 1, "12 min", 0, "Use a pain-free controlled range.", "Progress range and control, not intensity."),
        ProExercise("Dead bug", 3, "8-10 / side", 45, "Keep the lower back gently supported.", "Use a longer lever or slower tempo."),
        ProExercise("Bird-dog", 3, "8 / side", 45, "Keep the pelvis square.", "Add 3-second pauses.")
    ))
)

private class ProFitStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_fit_v21", Context.MODE_PRIVATE)

    fun replacement(date: LocalDate): String? = p.getString("replacement_$date", null)
    fun setReplacement(date: LocalDate, id: String?) = p.edit().apply {
        if (id == null) remove("replacement_$date") else putString("replacement_$date", id)
    }.apply()

    fun done(date: LocalDate, workoutId: String, ex: Int): Int = p.getInt("done_${date}_${workoutId}_$ex", 0)
    fun setDone(date: LocalDate, workoutId: String, ex: Int, value: Int) = p.edit().putInt("done_${date}_${workoutId}_$ex", value).apply()

    fun load(name: String): Double = p.getFloat("load_${name.hashCode()}", 0f).toDouble()
    fun setLoad(name: String, value: Double) = p.edit().putFloat("load_${name.hashCode()}", value.coerceAtLeast(0.0).toFloat()).apply()

    fun level(name: String): Int = p.getInt("level_${name.hashCode()}", 1)
    fun setLevel(name: String, value: Int) = p.edit().putInt("level_${name.hashCode()}", value.coerceIn(1, 5)).apply()

    fun extraSets(name: String): Int = p.getInt("extra_${name.hashCode()}", 0)
    fun setExtraSets(name: String, value: Int) = p.edit().putInt("extra_${name.hashCode()}", value.coerceIn(0, 3)).apply()

    fun rir(name: String): Int = p.getInt("rir_${name.hashCode()}", 2)
    fun setRir(name: String, value: Int) = p.edit().putInt("rir_${name.hashCode()}", value.coerceIn(0, 5)).apply()

    fun successCount(name: String): Int = p.getInt("success_${name.hashCode()}", 0)
    fun setSuccessCount(name: String, value: Int) = p.edit().putInt("success_${name.hashCode()}", value.coerceAtLeast(0)).apply()

    fun bestLoad(name: String): Double = p.getFloat("best_${name.hashCode()}", 0f).toDouble()
    fun setBestLoad(name: String, value: Double) = p.edit().putFloat("best_${name.hashCode()}", value.toFloat()).apply()

    fun markWorkout(date: LocalDate, id: String) = p.edit().putBoolean("workout_${date}_$id", true).apply()
}

class ProWorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides profile.language()) {
                NZTProTheme { ProWorkoutScreen(date) { finish() } }
            }
        }
    }
}

@Composable
private fun ProWorkoutScreen(date: LocalDate, onClose: () -> Unit) {
    val l = rememberLocalizer()
    val context = LocalContext.current
    val store = remember { ProFitStore(context) }
    val original = remember(date) { StathamEngine.forDate(date) }
    val replacementId = remember { store.replacement(date) }
    val alt = alternateWorkouts.firstOrNull { it.id == replacementId }
    val workoutId = alt?.id ?: original.session.code
    val title = alt?.let { l.t(it.titleKey) } ?: original.session.title
    val minutes = alt?.minutes ?: original.session.minutes
    val exercises = alt?.exercises ?: original.session.exercises.map {
        ProExercise(it.name, it.sets, it.target, it.restSeconds, it.technique, it.progression)
    }

    var refresh by remember { mutableIntStateOf(0) }
    var restSeconds by remember { mutableIntStateOf(0) }
    var restRunning by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }

    LaunchedEffect(restRunning, restSeconds) {
        if (restRunning && restSeconds > 0) {
            delay(1000)
            restSeconds--
        } else if (restRunning && restSeconds <= 0) {
            restRunning = false
        }
    }

    val totalSets = exercises.sumOf { it.sets + store.extraSets(it.name) }
    val doneSets = exercises.indices.sumOf { i ->
        store.done(date, workoutId, i).coerceAtMost(exercises[i].sets + store.extraSets(exercises[i].name))
    }
    val pct = if (totalSets == 0) 0f else doneSets.toFloat() / totalSets

    Scaffold(
        containerColor = NztBg,
        topBar = {
            Surface(color = NztBg) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                    Column(Modifier.weight(1f)) {
                        Text("NZT TRAINING", color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
                        Text("$doneSets/$totalSets", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NztAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF09131C)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().height(5.dp), color = NztAccent, trackColor = NztLine)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showFinish = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(l.t("finish"), fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ResponsiveWorkoutMetrics(minutes, totalSets, (pct * 100).roundToInt(), l)
            }
            items(exercises.size) { index ->
                ExerciseWorkCard(exercises[index], date, workoutId, index, store, refresh) { seconds ->
                    restSeconds = seconds
                    restRunning = seconds > 0
                    refresh++
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (restRunning) {
        ModalBottomSheet(onDismissRequest = { restRunning = false }, containerColor = NztSurface) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(l.t("timer"), color = NztMuted, fontWeight = FontWeight.Bold)
                Text(restSeconds.toString(), fontSize = 62.sp, fontWeight = FontWeight.Black, color = NztAccent)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionButton("-15", Modifier.weight(1f)) { restSeconds = (restSeconds - 15).coerceAtLeast(0) }
                    SmallActionButton("+15", Modifier.weight(1f)) { restSeconds += 15 }
                    Button(onClick = { restRunning = false }, modifier = Modifier.weight(1f)) { Text(l.t("skip"), maxLines = 1) }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showFinish) {
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text(l.t("finish")) },
            text = { Text("$doneSets / $totalSets ${l.t("sets")} - ${(pct * 100).roundToInt()}%") },
            confirmButton = {
                Button(onClick = {
                    exercises.forEachIndexed { index, ex ->
                        val prescribed = ex.sets + store.extraSets(ex.name)
                        val done = store.done(date, workoutId, index)
                        if (done >= prescribed && prescribed > 0) {
                            val currentLoad = store.load(ex.name)
                            if (currentLoad > store.bestLoad(ex.name)) store.setBestLoad(ex.name, currentLoad)
                            val success = store.successCount(ex.name) + 1
                            if (success >= 2 && store.rir(ex.name) >= 2) {
                                if (currentLoad > 0) store.setLoad(ex.name, currentLoad + 0.5)
                                else store.setLevel(ex.name, store.level(ex.name) + 1)
                                store.setSuccessCount(ex.name, 0)
                            } else {
                                store.setSuccessCount(ex.name, success)
                            }
                        }
                    }
                    store.markWorkout(date, workoutId)
                    onClose()
                }) { Text(l.t("done")) }
            },
            dismissButton = { TextButton(onClick = { showFinish = false }) { Text(l.t("continue")) } }
        )
    }
}

@Composable
private fun ResponsiveWorkoutMetrics(minutes: Int, totalSets: Int, percent: Int, l: Localizer) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 350.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(Icons.Default.Timer, "$minutes min", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricChip(Icons.Default.FitnessCenter, "$totalSets ${l.t("sets")}", Modifier.weight(1f))
                    MetricChip(Icons.Default.TrendingUp, "$percent%", Modifier.weight(1f))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(Icons.Default.Timer, "$minutes min", Modifier.weight(1f))
                MetricChip(Icons.Default.FitnessCenter, "$totalSets ${l.t("sets")}", Modifier.weight(1f))
                MetricChip(Icons.Default.TrendingUp, "$percent%", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExerciseWorkCard(
    ex: ProExercise,
    date: LocalDate,
    workoutId: String,
    index: Int,
    store: ProFitStore,
    refresh: Int,
    onSetDone: (Int) -> Unit
) {
    val l = rememberLocalizer()
    var load by remember(refresh, ex.name) { mutableStateOf(store.load(ex.name)) }
    var level by remember(refresh, ex.name) { mutableIntStateOf(store.level(ex.name)) }
    var extra by remember(refresh, ex.name) { mutableIntStateOf(store.extraSets(ex.name)) }
    var rir by remember(refresh, ex.name) { mutableIntStateOf(store.rir(ex.name)) }
    var expanded by remember { mutableStateOf(false) }
    val prescribed = ex.sets + extra
    var done by remember(refresh, date, workoutId, index) {
        mutableIntStateOf(store.done(date, workoutId, index).coerceAtMost(prescribed))
    }
    val best = store.bestLoad(ex.name)

    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 340.dp) {
                    Column {
                        ExerciseIllustration(ex.name, Modifier.fillMaxWidth().height(150.dp))
                        Spacer(Modifier.height(12.dp))
                        ExerciseHeaderText(ex, prescribed, load, level, best, l)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExerciseIllustration(ex.name, Modifier.size(94.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { ExerciseHeaderText(ex, prescribed, load, level, best, l) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(ex.technique, color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) l.t("progression") else "+ ${l.t("progression")}")
            }
            if (expanded) {
                Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
                    Text(ex.progression, Modifier.fillMaxWidth().padding(10.dp), color = NztAccent, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(l.t("sets").uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(prescribed.coerceAtMost(8)) { setIndex ->
                    val completed = setIndex < done
                    FilledIconButton(
                        onClick = {
                            done = if (completed && setIndex == done - 1) done - 1 else if (!completed && setIndex == done) done + 1 else done
                            store.setDone(date, workoutId, index, done)
                            if (!completed && setIndex < done) onSetDone(ex.rest)
                        },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (completed) NztAccent else NztSurface2,
                            contentColor = if (completed) Color.Black else NztText
                        )
                    ) { Text((setIndex + 1).toString(), fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("RIR", color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..5).forEach { value ->
                    FilterChip(
                        selected = rir == value,
                        onClick = { rir = value; store.setRir(ex.name, value) },
                        label = { Text(value.toString(), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            ResponsiveControlGrid(
                onSetMinus = { extra = (extra - 1).coerceAtLeast(0); store.setExtraSets(ex.name, extra) },
                onSetPlus = { extra = (extra + 1).coerceAtMost(3); store.setExtraSets(ex.name, extra) },
                canSetMinus = extra > 0,
                onLoadMinus = { load = (load - .5).coerceAtLeast(0.0); store.setLoad(ex.name, load) },
                onLoadPlus = { load += .5; store.setLoad(ex.name, load) },
                onLevelMinus = { level = (level - 1).coerceAtLeast(1); store.setLevel(ex.name, level) },
                onLevelPlus = { level = (level + 1).coerceAtMost(5); store.setLevel(ex.name, level) },
                canLevelMinus = level > 1,
                canLevelPlus = level < 5
            )

            Spacer(Modifier.height(10.dp))
            val suggestion = when {
                done >= prescribed && rir >= 3 && load > 0 -> "NEXT: +0.5 kg"
                done >= prescribed && rir >= 3 -> "NEXT: level +1"
                done < prescribed && done > 0 -> "NEXT: keep load and complete all sets"
                else -> "NEXT: hit the prescribed work with clean technique"
            }
            Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoGraph, null, tint = NztAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(suggestion, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseHeaderText(ex: ProExercise, prescribed: Int, load: Double, level: Int, best: Double, l: Localizer) {
    Text(ex.name, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text("$prescribed ${l.t("sets")} | ${ex.target} | ${ex.rest}s", color = NztMuted, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = NztSurface2, shape = RoundedCornerShape(10.dp)) {
            Text(
                if (load > 0) "${formatLoad(load)} kg" else "${l.t("level")} $level",
                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                color = NztAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (best > 0) {
            Surface(color = NztSurface2, shape = RoundedCornerShape(10.dp)) {
                Text("PR ${formatLoad(best)}", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = NztAccent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResponsiveControlGrid(
    onSetMinus: () -> Unit,
    onSetPlus: () -> Unit,
    canSetMinus: Boolean,
    onLoadMinus: () -> Unit,
    onLoadPlus: () -> Unit,
    onLevelMinus: () -> Unit,
    onLevelPlus: () -> Unit,
    canLevelMinus: Boolean,
    canLevelPlus: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CompactControl("SET -", Modifier.weight(1f), enabled = canSetMinus, onClick = onSetMinus)
            CompactControl("SET +", Modifier.weight(1f), onClick = onSetPlus)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CompactControl("-0.5 KG", Modifier.weight(1f), onClick = onLoadMinus)
            CompactControl("+0.5 KG", Modifier.weight(1f), onClick = onLoadPlus)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CompactControl("LEVEL -", Modifier.weight(1f), enabled = canLevelMinus, onClick = onLevelMinus)
            CompactControl("LEVEL +", Modifier.weight(1f), enabled = canLevelPlus, onClick = onLevelPlus)
        }
    }
}

@Composable
private fun CompactControl(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        shape = RoundedCornerShape(13.dp)
    ) { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

private fun formatLoad(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)

@Composable
private fun MetricChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = NztSurface, shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NztAccent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SmallActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 6.dp)) {
        Text(text, maxLines = 1)
    }
}

class ProTrainingLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides profile.language()) {
                NZTProTheme { ProTrainingLibraryScreen(date) { finish() } }
            }
        }
    }
}

@Composable
private fun ProTrainingLibraryScreen(date: LocalDate, onClose: () -> Unit) {
    val l = rememberLocalizer()
    val context = LocalContext.current
    val store = remember { ProFitStore(context) }
    var selected by remember { mutableStateOf(store.replacement(date)) }

    Scaffold(containerColor = NztBg, topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
            Column(Modifier.weight(1f)) {
                Text(l.t("exercise_library"), fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(l.t("replace_workout"), color = NztMuted, fontSize = 12.sp)
            }
        }
    }) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val original = StathamEngine.forDate(date)
                WorkoutOptionCard("ORIGINAL", original.session.title, original.session.minutes, selected == null, original.session.title) {
                    selected = null
                    store.setReplacement(date, null)
                }
            }
            items(alternateWorkouts.size) { i ->
                val w = alternateWorkouts[i]
                val title = l.t(w.titleKey)
                WorkoutOptionCard(w.code, title, w.minutes, selected == w.id, w.exercises.firstOrNull()?.name ?: title) {
                    selected = w.id
                    store.setReplacement(date, w.id)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = NztAccent)
                            Spacer(Modifier.width(8.dp))
                            Text(l.t("custom_workout"), fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Choose the closest equipment template, then tune sets, load, RIR and level inside the workout.", color = NztMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutOptionCard(code: String, title: String, minutes: Int, selected: Boolean, artName: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) NztSurface2 else NztSurface),
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, NztAccent) else null
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            if (maxWidth < 330.dp) {
                Column {
                    ExerciseIllustration(artName, Modifier.fillMaxWidth().height(130.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WorkoutOptionText(code, title, minutes, Modifier.weight(1f))
                        RadioButton(selected = selected, onClick = onClick)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseIllustration(artName, Modifier.size(78.dp))
                    Spacer(Modifier.width(10.dp))
                    WorkoutOptionText(code, title, minutes, Modifier.weight(1f))
                    RadioButton(selected = selected, onClick = onClick)
                }
            }
        }
    }
}

@Composable
private fun WorkoutOptionText(code: String, title: String, minutes: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(code, color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("$minutes min", color = NztMuted, fontSize = 12.sp)
    }
}

private data class FoodEntry(
    val id: String,
    val meal: String,
    val name: String,
    val grams: Double,
    val kcal100: Double,
    val protein100: Double,
    val fats100: Double,
    val carbs100: Double
) {
    val factor get() = grams / 100.0
    val kcal get() = kcal100 * factor
    val protein get() = protein100 * factor
    val fats get() = fats100 * factor
    val carbs get() = carbs100 * factor
}

private class FoodStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_food_v21", Context.MODE_PRIVATE)

    fun load(date: LocalDate): MutableList<FoodEntry> {
        val out = mutableListOf<FoodEntry>()
        val arr = try { JSONArray(p.getString("food_$date", "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += FoodEntry(
                o.getString("id"), o.getString("meal"), o.getString("name"), o.getDouble("grams"),
                o.getDouble("kcal100"), o.getDouble("p100"), o.getDouble("f100"), o.getDouble("c100")
            )
        }
        return out
    }

    fun save(date: LocalDate, entries: List<FoodEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("meal", e.meal); put("name", e.name); put("grams", e.grams)
                put("kcal100", e.kcal100); put("p100", e.protein100); put("f100", e.fats100); put("c100", e.carbs100)
            })
        }
        p.edit().putString("food_$date", arr.toString()).apply()
    }

    fun water(date: LocalDate): Int = p.getInt("water_$date", 0)
    fun setWater(date: LocalDate, ml: Int) = p.edit().putInt("water_$date", ml.coerceAtLeast(0)).apply()
}

private data class QuickFood(val name: String, val grams: Double, val kcal: Double, val p: Double, val f: Double, val c: Double)
private val quickFoods = listOf(
    QuickFood("Eggs", 100.0, 143.0, 13.0, 9.5, 0.7),
    QuickFood("Oats", 80.0, 370.0, 13.0, 7.0, 60.0),
    QuickFood("Chicken breast", 180.0, 165.0, 31.0, 3.6, 0.0),
    QuickFood("Rice cooked", 200.0, 130.0, 2.7, 0.3, 28.0),
    QuickFood("Skyr", 200.0, 63.0, 11.0, 0.2, 4.0),
    QuickFood("Banana", 120.0, 89.0, 1.1, 0.3, 23.0)
)

class ProNutritionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides profile.language()) {
                NZTProTheme { ProNutritionScreen(date) { finish() } }
            }
        }
    }
}

@Composable
private fun ProNutritionScreen(date: LocalDate, onClose: () -> Unit) {
    val l = rememberLocalizer()
    val context = LocalContext.current
    val store = remember { FoodStore(context) }
    val repo = remember { NZTRepository(context) }
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(date) { StathamEngine.targets(plan) }
    var entries by remember(date) { mutableStateOf(store.load(date).toList()) }
    var water by remember(date) { mutableIntStateOf(store.water(date)) }
    var addMeal by remember { mutableStateOf<String?>(null) }

    val kcal = entries.sumOf { it.kcal }.roundToInt()
    val protein = entries.sumOf { it.protein }.roundToInt()
    val fats = entries.sumOf { it.fats }.roundToInt()
    val carbs = entries.sumOf { it.carbs }.roundToInt()

    LaunchedEffect(kcal, protein) { repo.setNutrition(kcal, protein) }

    Scaffold(containerColor = NztBg, topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
            Column(Modifier.weight(1f)) {
                Text(l.t("nutrition"), fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(date.toString(), color = NztMuted, fontSize = 12.sp)
            }
        }
    }) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NutritionSummary(kcal, protein, fats, carbs, targets.calories, targets.protein)
            }
            item {
                WaterCard(water) { delta ->
                    water = (water + delta).coerceAtLeast(0)
                    store.setWater(date, water)
                }
            }
            item {
                Text("QUICK ADD", color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    quickFoods.forEach { q ->
                        AssistChip(onClick = {
                            val meal = addMeal ?: "breakfast"
                            val updated = entries + FoodEntry(UUID.randomUUID().toString(), meal, q.name, q.grams, q.kcal, q.p, q.f, q.c)
                            entries = updated
                            store.save(date, updated)
                        }, label = { Text(q.name) })
                    }
                }
            }
            item { MealCard("breakfast", l.t("meal_breakfast"), entries, targets.calories * 25 / 100, onAdd = { addMeal = "breakfast" }, onDelete = { id -> entries = entries.filterNot { it.id == id }; store.save(date, entries) }) }
            item { MealCard("lunch", l.t("meal_lunch"), entries, targets.calories * 35 / 100, onAdd = { addMeal = "lunch" }, onDelete = { id -> entries = entries.filterNot { it.id == id }; store.save(date, entries) }) }
            item { MealCard("snack", l.t("meal_snack"), entries, targets.calories * 15 / 100, onAdd = { addMeal = "snack" }, onDelete = { id -> entries = entries.filterNot { it.id == id }; store.save(date, entries) }) }
            item { MealCard("dinner", l.t("meal_dinner"), entries, targets.calories * 25 / 100, onAdd = { addMeal = "dinner" }, onDelete = { id -> entries = entries.filterNot { it.id == id }; store.save(date, entries) }) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    addMeal?.let { meal ->
        AddFoodDialog(
            meal = meal,
            onDismiss = { addMeal = null },
            onAdd = { e ->
                val updated = entries + e
                entries = updated
                store.save(date, updated)
                addMeal = null
            }
        )
    }
}

@Composable
private fun NutritionSummary(kcal: Int, protein: Int, fats: Int, carbs: Int, targetKcal: Int, targetProtein: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("DAILY FUEL", color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("$kcal / $targetKcal kcal", fontSize = 24.sp, fontWeight = FontWeight.Black, color = NztAccent)
                }
                Text("P $protein/$targetProtein", color = NztText, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { (kcal.toFloat() / targetKcal.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = NztAccent, trackColor = NztLine)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MacroMini("PROTEIN", "${protein}g", Modifier.weight(1f))
                MacroMini("FATS", "${fats}g", Modifier.weight(1f))
                MacroMini("CARBS", "${carbs}g", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroMini(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 9.dp)) {
            Text(label, color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun WaterCard(water: Int, onChange: (Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WaterDrop, null, tint = NztAccent2)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("WATER", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text("$water ml", fontWeight = FontWeight.Bold)
            }
            SmallActionButton("-250") { onChange(-250) }
            Spacer(Modifier.width(6.dp))
            Button(onClick = { onChange(250) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("+250") }
        }
    }
}

@Composable
private fun MealCard(mealId: String, title: String, all: List<FoodEntry>, target: Int, onAdd: () -> Unit, onDelete: (String) -> Unit) {
    val meal = all.filter { it.meal == mealId }
    val kcal = meal.sumOf { it.kcal }.roundToInt()
    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("$kcal / $target kcal", color = NztMuted, fontSize = 12.sp)
                }
                FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Default.Add, null) }
            }
            if (meal.isEmpty()) {
                Text("No foods yet", color = NztMuted, fontSize = 12.sp)
            } else {
                Spacer(Modifier.height(6.dp))
                meal.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${e.grams.roundToInt()} g | ${e.kcal.roundToInt()} kcal | P ${e.protein.roundToInt()} F ${e.fats.roundToInt()} C ${e.carbs.roundToInt()}", color = NztMuted, fontSize = 10.sp)
                        }
                        IconButton(onClick = { onDelete(e.id) }) { Icon(Icons.Default.DeleteOutline, null, tint = NztMuted) }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(rememberLocalizer().t("add_food"), maxLines = 1)
            }
        }
    }
}

@Composable
private fun AddFoodDialog(meal: String, onDismiss: () -> Unit, onAdd: (FoodEntry) -> Unit) {
    val l = rememberLocalizer()
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(l.t("add_food")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Food") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NumericField(grams, { grams = it }, "Grams")
                NumericField(kcal, { kcal = it }, "kcal / 100 g")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumericField(protein, { protein = it }, "P", Modifier.weight(1f))
                    NumericField(fats, { fats = it }, "F", Modifier.weight(1f))
                    NumericField(carbs, { carbs = it }, "C", Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val g = grams.toDoubleOrNull() ?: 0.0
                val k = kcal.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && g > 0 && k >= 0) {
                    onAdd(FoodEntry(UUID.randomUUID().toString(), meal, name.trim(), g, k, protein.toDoubleOrNull() ?: 0.0, fats.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0))
                }
            }) { Text(l.t("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(l.t("skip")) } }
    )
}

@Composable
private fun NumericField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.replace(',', '.').take(8)) },
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}
