package com.nzt365.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
    val icon: String,
    val minutes: Int,
    val exercises: List<ProExercise>
)

private val alternateWorkouts = listOf(
    AltWorkout("home", "home", "HOME", 42, listOf(
        ProExercise("Push-up", 4, "8–15", 75, "Body in one line; elbows 30–60°.", "15×4 twice → harder variation or band."),
        ProExercise("Split squat", 4, "8–12 / leg", 75, "Stable front foot; knee follows toes.", "12×4 → Bulgarian split squat or added load."),
        ProExercise("Band row", 4, "10–15", 75, "Shoulders down, squeeze shoulder blades.", "15×4 → stronger band."),
        ProExercise("Plank", 3, "30–60 sec", 45, "Ribs down, glutes active.", "60 sec → harder plank variation.")
    )),
    AltWorkout("bars", "pullup_bars", "BARS", 50, listOf(
        ProExercise("Pull-up", 5, "4–10", 120, "Full controlled range, no neck reach.", "10×5 → add weight or reduce assistance."),
        ProExercise("Dip", 4, "6–12", 100, "Shoulders stable; descend only pain-free.", "12×4 → add load."),
        ProExercise("Hanging knee raise", 4, "8–15", 75, "Posterior pelvic tilt, no swing.", "15×4 → straight-leg raise."),
        ProExercise("Australian row", 4, "8–15", 75, "Chest to bar, body rigid.", "15×4 → feet higher.")
    )),
    AltWorkout("dumbbells", "dumbbells", "DB", 55, listOf(
        ProExercise("Dumbbell floor press", 4, "8–12", 90, "Shoulders packed, controlled eccentric.", "12×4 → +1–2 kg per dumbbell."),
        ProExercise("One-arm dumbbell row", 4, "8–12 / side", 90, "Keep torso square.", "12×4 → +1–2 kg."),
        ProExercise("Goblet squat", 4, "8–15", 90, "Brace and keep full foot pressure.", "15×4 → heavier dumbbell."),
        ProExercise("Dumbbell curl", 3, "10–15", 60, "No shoulder swing.", "15×3 → +1 kg.")
    )),
    AltWorkout("barbell", "barbell", "BB", 60, listOf(
        ProExercise("Barbell squat", 4, "5–8", 150, "Brace, controlled depth, knees track toes.", "8×4 with RIR 2 → +2.5 kg."),
        ProExercise("Bench press", 4, "5–8", 150, "Stable upper back and leg drive.", "8×4 with RIR 2 → +2.5 kg."),
        ProExercise("Barbell row", 4, "6–10", 120, "Neutral spine, pull to lower ribs.", "10×4 → +2.5 kg."),
        ProExercise("Romanian deadlift", 3, "6–10", 150, "Hips back, bar close to legs.", "10×3 → +2.5–5 kg.")
    )),
    AltWorkout("run", "running", "RUN", 40, listOf(
        ProExercise("Easy run", 1, "30 min Z2", 0, "Conversational pace, smooth cadence.", "Add 5 minutes when recovery is good."),
        ProExercise("Strides", 6, "20 sec", 60, "Fast but relaxed, full recovery.", "Add 1 rep up to 8."),
        ProExercise("Cooldown walk", 1, "5–10 min", 0, "Breathe down gradually.", "Keep easy.")
    )),
    AltWorkout("bike", "cycling", "BIKE", 55, listOf(
        ProExercise("Cycling Z2", 1, "45–60 min", 0, "Steady aerobic pace.", "Add 5–10 min, then small power increase."),
        ProExercise("High cadence", 5, "60 sec", 60, "Smooth 100–110 rpm, no bouncing.", "Add one interval when easy.")
    )),
    AltWorkout("mobility", "mobility", "MOB", 30, listOf(
        ProExercise("Mobility flow", 1, "12 min", 0, "Pain-free controlled range.", "Improve range, not intensity."),
        ProExercise("Dead bug", 3, "8–10 / side", 45, "Low back gently supported.", "Longer lever or slower tempo."),
        ProExercise("Bird-dog", 3, "8 / side", 45, "Keep pelvis square.", "3-sec pauses.")
    ))
)

private class ProFitStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_fit_v2", Context.MODE_PRIVATE)

    fun replacement(date: LocalDate): String? = p.getString("replacement_$date", null)
    fun setReplacement(date: LocalDate, id: String?) = p.edit().apply { if (id == null) remove("replacement_$date") else putString("replacement_$date", id) }.apply()

    fun done(date: LocalDate, workoutId: String, ex: Int): Int = p.getInt("done_${date}_${workoutId}_$ex", 0)
    fun setDone(date: LocalDate, workoutId: String, ex: Int, value: Int) = p.edit().putInt("done_${date}_${workoutId}_$ex", value).apply()

    fun load(name: String): Double = p.getFloat("load_${name.hashCode()}", 0f).toDouble()
    fun setLoad(name: String, value: Double) = p.edit().putFloat("load_${name.hashCode()}", value.coerceAtLeast(0.0).toFloat()).apply()

    fun level(name: String): Int = p.getInt("level_${name.hashCode()}", 1)
    fun setLevel(name: String, value: Int) = p.edit().putInt("level_${name.hashCode()}", value.coerceIn(1, 5)).apply()

    fun extraSets(name: String): Int = p.getInt("extra_${name.hashCode()}", 0)
    fun setExtraSets(name: String, value: Int) = p.edit().putInt("extra_${name.hashCode()}", value.coerceIn(0, 3)).apply()

    fun successCount(name: String): Int = p.getInt("success_${name.hashCode()}", 0)
    fun setSuccessCount(name: String, value: Int) = p.edit().putInt("success_${name.hashCode()}", value).apply()

    fun markWorkout(date: LocalDate, id: String) = p.edit().putBoolean("workout_${date}_$id", true).apply()
    fun workoutsCompleted(): Int = p.all.keys.count { it.startsWith("workout_") && p.getBoolean(it, false) }
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
    val sourceExercises = alt?.exercises ?: original.session.exercises.map { ProExercise(it.name, it.sets, it.target, it.restSeconds, it.technique, it.progression) }

    var refresh by remember { mutableIntStateOf(0) }
    var restSeconds by remember { mutableIntStateOf(0) }
    var restRunning by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }

    LaunchedEffect(restRunning, restSeconds) {
        if (restRunning && restSeconds > 0) {
            delay(1000)
            restSeconds--
        } else if (restRunning && restSeconds <= 0) restRunning = false
    }

    val totalSets = sourceExercises.sumOf { it.sets + store.extraSets(it.name) }
    val doneSets = sourceExercises.indices.sumOf { i -> store.done(date, workoutId, i).coerceAtMost(sourceExercises[i].sets + store.extraSets(sourceExercises[i].name)) }
    val pct = if (totalSets == 0) 0f else doneSets.toFloat() / totalSets

    Scaffold(
        containerColor = NztBg,
        topBar = {
            Surface(color = NztBg) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                    Column(Modifier.weight(1f)) {
                        Text("STATHAM / NZT", color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("$doneSets/$totalSets", color = NztAccent, fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF09131C)) {
                Column(Modifier.padding(14.dp)) {
                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().height(5.dp), color = NztAccent, trackColor = NztLine)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { showFinish = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text(l.t("finish"), fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip(Icons.Default.Timer, "$minutes min", Modifier.weight(1f))
                    MetricChip(Icons.Default.FitnessCenter, "$totalSets ${l.t("sets")}", Modifier.weight(1f))
                    MetricChip(Icons.Default.TrendingUp, "${(pct * 100).roundToInt()}%", Modifier.weight(1f))
                }
            }
            itemsIndexed(sourceExercises) { index, ex ->
                ExerciseWorkCard(ex, date, workoutId, index, store, refresh) { seconds ->
                    restSeconds = seconds
                    restRunning = seconds > 0
                    refresh++
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (restRunning) {
        ModalBottomSheet(onDismissRequest = { restRunning = false }, containerColor = NztSurface) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(l.t("timer"), color = NztMuted, fontWeight = FontWeight.Bold)
                Text("$restSeconds", fontSize = 64.sp, fontWeight = FontWeight.Black, color = NztAccent)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { restSeconds = (restSeconds - 15).coerceAtLeast(0) }) { Text("−15") }
                    OutlinedButton(onClick = { restSeconds += 15 }) { Text("+15") }
                    Button(onClick = { restRunning = false }) { Text(l.t("skip")) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showFinish) {
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text(l.t("finish")) },
            text = { Text("$doneSets / $totalSets ${l.t("sets")} • ${(pct*100).roundToInt()}%") },
            confirmButton = {
                Button(onClick = {
                    sourceExercises.forEachIndexed { index, ex ->
                        val prescribed = ex.sets + store.extraSets(ex.name)
                        val done = store.done(date, workoutId, index)
                        if (done >= prescribed && prescribed > 0) {
                            val success = store.successCount(ex.name) + 1
                            if (success >= 2) {
                                val currentLoad = store.load(ex.name)
                                if (currentLoad > 0) store.setLoad(ex.name, currentLoad + 1.0)
                                else store.setLevel(ex.name, store.level(ex.name) + 1)
                                store.setSuccessCount(ex.name, 0)
                            } else store.setSuccessCount(ex.name, success)
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
private fun ExerciseWorkCard(ex: ProExercise, date: LocalDate, workoutId: String, index: Int, store: ProFitStore, refresh: Int, onSetDone: (Int) -> Unit) {
    val l = rememberLocalizer()
    var load by remember(refresh, ex.name) { mutableStateOf(store.load(ex.name)) }
    var level by remember(refresh, ex.name) { mutableIntStateOf(store.level(ex.name)) }
    var extra by remember(refresh, ex.name) { mutableIntStateOf(store.extraSets(ex.name)) }
    var expanded by remember { mutableStateOf(false) }
    val prescribed = ex.sets + extra
    var done by remember(refresh, date, workoutId, index) { mutableIntStateOf(store.done(date, workoutId, index).coerceAtMost(prescribed)) }

    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseIllustration(ex.name, Modifier.size(112.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(ex.name, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("$prescribed ${l.t("sets")} • ${ex.target} • ${l.t("rest")} ${ex.rest}s", color = NztMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(7.dp))
                    Surface(color = NztSurface2, shape = RoundedCornerShape(10.dp)) {
                        Text(if (load > 0) "${l.t("load")}: ${formatLoad(load)} kg" else "${l.t("level")}: $level", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(ex.technique, color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "− ${l.t("progression")}" else "+ ${l.t("progression")}")
            }
            if (expanded) Text(ex.progression, color = NztAccent, fontSize = 13.sp, lineHeight = 19.sp)

            Spacer(Modifier.height(8.dp))
            Text(l.t("sets").uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(prescribed.coerceAtMost(6)) { setIndex ->
                    val completed = setIndex < done
                    FilledIconButton(
                        onClick = {
                            done = if (completed && setIndex == done - 1) done - 1 else if (!completed && setIndex == done) done + 1 else done
                            store.setDone(date, workoutId, index, done)
                            if (!completed && setIndex < done) onSetDone(ex.rest)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (completed) NztAccent else NztSurface2, contentColor = if (completed) Color.Black else NztText)
                    ) { Text("${setIndex + 1}", fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { extra = (extra - 1).coerceAtLeast(0); store.setExtraSets(ex.name, extra) }, enabled = extra > 0, modifier = Modifier.weight(1f)) { Text("− ${l.t("sets")}") }
                OutlinedButton(onClick = { extra = (extra + 1).coerceAtMost(3); store.setExtraSets(ex.name, extra) }, modifier = Modifier.weight(1f)) { Text("+ ${l.t("sets")}") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { load = (load - .5).coerceAtLeast(0.0); store.setLoad(ex.name, load) }, modifier = Modifier.weight(1f)) { Text("−0.5 kg") }
                OutlinedButton(onClick = { load += .5; store.setLoad(ex.name, load) }, modifier = Modifier.weight(1f)) { Text("+0.5 kg") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { level = (level - 1).coerceAtLeast(1); store.setLevel(ex.name, level) }, enabled = level > 1, modifier = Modifier.weight(1f)) { Text("${l.t("level")} −") }
                OutlinedButton(onClick = { level = (level + 1).coerceAtMost(5); store.setLevel(ex.name, level) }, enabled = level < 5, modifier = Modifier.weight(1f)) { Text("${l.t("level")} +") }
            }
        }
    }
}

private fun formatLoad(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)

@Composable
private fun MetricChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = NztSurface, shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NztAccent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

class ProTrainingLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent { CompositionLocalProvider(LocalAppLanguage provides profile.language()) { NZTProTheme { ProTrainingLibraryScreen(date) { finish() } } } }
    }
}

@Composable
private fun ProTrainingLibraryScreen(date: LocalDate, onClose: () -> Unit) {
    val l = rememberLocalizer()
    val context = LocalContext.current
    val store = remember { ProFitStore(context) }
    var selected by remember { mutableStateOf(store.replacement(date)) }

    Scaffold(containerColor = NztBg, topBar = {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
            Column(Modifier.weight(1f)) { Text(l.t("exercise_library"), fontSize = 22.sp, fontWeight = FontWeight.Black); Text(l.t("replace_workout"), color = NztMuted, fontSize = 12.sp) }
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                WorkoutOptionCard("ORIGINAL", StathamEngine.forDate(date).session.title, StathamEngine.forDate(date).session.minutes, selected == null, StathamEngine.forDate(date).session.title) {
                    selected = null; store.setReplacement(date, null)
                }
            }
            itemsIndexed(alternateWorkouts) { _, w ->
                val title = l.t(w.titleKey)
                WorkoutOptionCard(w.icon, title, w.minutes, selected == w.id, w.exercises.firstOrNull()?.name ?: title) {
                    selected = w.id; store.setReplacement(date, w.id)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(l.t("custom_workout"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Create a personal session by choosing the closest equipment template, then adjust sets, load and level inside the workout.", color = NztMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("HOME • BARS • DUMBBELLS • BARBELL • RUN • BIKE • MOBILITY", color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(22.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, NztAccent) else null
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseIllustration(artName, Modifier.size(90.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(code, color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("$minutes min", color = NztMuted)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
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
    private val p = context.getSharedPreferences("nzt_food_v2", Context.MODE_PRIVATE)
    fun load(date: LocalDate): MutableList<FoodEntry> {
        val out = mutableListOf<FoodEntry>()
        val arr = try { JSONArray(p.getString("food_$date", "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += FoodEntry(o.getString("id"), o.getString("meal"), o.getString("name"), o.getDouble("grams"), o.getDouble("kcal100"), o.getDouble("p100"), o.getDouble("f100"), o.getDouble("c100"))
        }
        return out
    }
    fun save(date: LocalDate, entries: List<FoodEntry>) {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply { put("id", e.id); put("meal", e.meal); put("name", e.name); put("grams", e.grams); put("kcal100", e.kcal100); put("p100", e.protein100); put("f100", e.fats100); put("c100", e.carbs100) }) }
        p.edit().putString("food_$date", arr.toString()).apply()
    }
}

class ProNutritionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent { CompositionLocalProvider(LocalAppLanguage provides profile.language()) { NZTProTheme { ProNutritionScreen(date) { finish() } } } }
    }
}

@Composable
private fun ProNutritionScreen(date: LocalDate, onClose: () -> Unit) {
    val l = rememberLocalizer()
    val context = LocalContext.current
    val store = remember { FoodStore(context) }
    var entries by remember { mutableStateOf(store.load(date).toList()) }
    val target = remember(date) { StathamEngine.targets(StathamEngine.forDate(date)) }
    var addingMeal by remember { mutableStateOf<String?>(null) }

    val kcal = entries.sumOf { it.kcal }.roundToInt()
    val protein = entries.sumOf { it.protein }.roundToInt()
    val fats = entries.sumOf { it.fats }.roundToInt()
    val carbs = entries.sumOf { it.carbs }.roundToInt()
    val meals = listOf("breakfast" to l.t("meal_breakfast"), "lunch" to l.t("meal_lunch"), "snack" to l.t("meal_snack"), "dinner" to l.t("meal_dinner"))

    Scaffold(containerColor = NztBg, topBar = {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
            Column(Modifier.weight(1f)) { Text(l.t("nutrition"), fontSize = 22.sp, fontWeight = FontWeight.Black); Text(date.toString(), color = NztMuted, fontSize = 12.sp) }
            TextButton(onClick = onClose) { Text(l.t("done")) }
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { NutritionHero(kcal, protein, fats, carbs, target.calories, target.protein, target.fats, target.carbs) }
            meals.forEach { (mealKey, mealTitle) ->
                val mealEntries = entries.filter { it.meal == mealKey }
                item {
                    MealCard(mealKey, mealTitle, mealEntries, target.calories) { addingMeal = mealKey }
                }
            }
        }
    }

    addingMeal?.let { meal ->
        AddFoodDialog(meal, onDismiss = { addingMeal = null }) { entry ->
            entries = entries + entry
            store.save(date, entries)
            addingMeal = null
        }
    }
}

@Composable
private fun NutritionHero(kcal: Int, protein: Int, fats: Int, carbs: Int, targetCal: Int, targetP: Int, targetF: Int, targetC: Int) {
    val l = rememberLocalizer()
    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(l.t("daily_total").uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(kcal.toString(), fontSize = 38.sp, fontWeight = FontWeight.Black, color = if (kcal <= targetCal) NztAccent else NztDanger)
                Text(" / $targetCal kcal", color = NztMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
            LinearProgressIndicator(progress = { (kcal.toFloat() / targetCal.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp), color = NztAccent, trackColor = NztLine)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroPill("P", protein, targetP, Modifier.weight(1f))
                MacroPill("F", fats, targetF, Modifier.weight(1f))
                MacroPill("C", carbs, targetC, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, value: Int, target: Int, modifier: Modifier = Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = NztAccent, fontWeight = FontWeight.Black)
            Text("$value/$target g", fontSize = 11.sp, color = NztMuted)
        }
    }
}

@Composable
private fun MealCard(mealKey: String, title: String, entries: List<FoodEntry>, targetCal: Int, onAdd: () -> Unit) {
    val l = rememberLocalizer()
    val kcal = entries.sumOf { it.kcal }.roundToInt()
    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("$kcal kcal", color = NztAccent, fontWeight = FontWeight.Bold)
            }
            if (entries.isEmpty()) {
                Text(mealSuggestion(mealKey), color = NztMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                entries.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(NztSurface2, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Restaurant, null, tint = NztAccent, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(e.name, fontWeight = FontWeight.SemiBold); Text("${e.grams.roundToInt()} g • ${e.kcal.roundToInt()} kcal • P ${e.protein.roundToInt()} F ${e.fats.roundToInt()} C ${e.carbs.roundToInt()}", color = NztMuted, fontSize = 10.sp) }
                    }
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(l.t("add_food")) }
        }
    }
}

private fun mealSuggestion(key: String): String = when (key) {
    "breakfast" -> "Eggs + oats + berries / fruit"
    "lunch" -> "Chicken / turkey + rice / buckwheat + vegetables"
    "snack" -> "Skyr / cottage cheese + fruit or protein snack"
    else -> "Fish / lean meat + potatoes / grains + vegetables"
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
    fun clean(s: String) = s.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(l.t("add_food")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Product") }, singleLine = true)
                OutlinedTextField(grams, { grams = clean(it) }, label = { Text(l.t("grams")) }, singleLine = true)
                Text("Per 100 g", color = NztMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(kcal, { kcal = clean(it) }, label = { Text("kcal") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(protein, { protein = clean(it) }, label = { Text("P") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(fats, { fats = clean(it) }, label = { Text("F") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(carbs, { carbs = clean(it) }, label = { Text("C") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val entry = FoodEntry(UUID.randomUUID().toString(), meal, name.ifBlank { "Food" }, grams.toDoubleOrNull() ?: 100.0, kcal.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, fats.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0)
                onAdd(entry)
            }) { Text(l.t("add_food")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(l.t("skip")) } }
    )
}
