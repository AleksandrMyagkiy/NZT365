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

private data class V4SetState(val kg: String = "", val reps: String = "", val rir: Int = 2, val done: Boolean = false)

private class V4WorkoutStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_workout_v4", Context.MODE_PRIVATE)
    private fun k(date: LocalDate, ex: String, set: Int, field: String) = "${date}_${ex.hashCode()}_${set}_$field"

    fun load(date: LocalDate, ex: String, set: Int, defaultReps: Int): V4SetState = V4SetState(
        kg = p.getString(k(date, ex, set, "kg"), "") ?: "",
        reps = p.getString(k(date, ex, set, "reps"), defaultReps.toString()) ?: defaultReps.toString(),
        rir = p.getInt(k(date, ex, set, "rir"), 2),
        done = p.getBoolean(k(date, ex, set, "done"), false)
    )

    fun save(date: LocalDate, ex: String, set: Int, s: V4SetState) {
        p.edit()
            .putString(k(date, ex, set, "kg"), s.kg)
            .putString(k(date, ex, set, "reps"), s.reps)
            .putInt(k(date, ex, set, "rir"), s.rir)
            .putBoolean(k(date, ex, set, "done"), s.done)
            .apply()
    }

    fun previous(ex: String): String = p.getString("last_${ex.hashCode()}", "—") ?: "—"
    fun setPrevious(ex: String, value: String) = p.edit().putString("last_${ex.hashCode()}", value).apply()
    fun best(ex: String): Double = p.getFloat("best_${ex.hashCode()}", 0f).toDouble()
    fun setBest(ex: String, value: Double) = p.edit().putFloat("best_${ex.hashCode()}", value.toFloat()).apply()
    fun markWorkout(date: LocalDate, code: String) = p.edit().putBoolean("workout_${date}_$code", true).apply()
}

class V4WorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val profile = ProfileStore(this)
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides profile.language()) {
                NZTProTheme { V4WorkoutScreen(date) { finish() } }
            }
        }
    }
}

@Composable
private fun V4WorkoutScreen(date: LocalDate, onClose: () -> Unit) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val plan = remember(date) { StathamEngine.forDate(date) }
    val store = remember { V4WorkoutStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var rest by remember { mutableIntStateOf(0) }
    var restOpen by remember { mutableStateOf(false) }
    var finishDialog by remember { mutableStateOf(false) }

    val totalSets = plan.session.exercises.sumOf { it.sets }
    val doneSets = plan.session.exercises.sumOf { ex ->
        (0 until ex.sets).count { store.load(date, ex.name, it, defaultReps(ex.target)).done }
    }
    val pct = if (totalSets == 0) 0f else doneSets.toFloat() / totalSets

    LaunchedEffect(restOpen, rest) {
        if (restOpen && rest > 0) {
            delay(1000)
            rest--
        } else if (restOpen && rest <= 0) {
            restOpen = false
        }
    }

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = NztBg, modifier = Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                    Column(Modifier.weight(1f)) {
                        Text("NZT TRAINING", color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                        Text(plan.session.title, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                        Text(
                            "$doneSets/$totalSets",
                            Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            color = NztAccent,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF09131C), tonalElevation = 0.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = NztAccent,
                        trackColor = NztLine
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { finishDialog = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Icon(Icons.Default.Flag, null)
                        Spacer(Modifier.width(8.dp))
                        Text(V3Strings.t(lang, "finish"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V4WorkoutMetric(Icons.Default.Timer, "${plan.session.minutes} min", Modifier.weight(1f))
                        V4WorkoutMetric(Icons.Default.FitnessCenter, exerciseCountLabel(lang, plan.session.exercises.size), Modifier.weight(1f))
                        V4WorkoutMetric(Icons.Default.TrendingUp, "${(pct * 100).roundToInt()}%", Modifier.weight(1f))
                    }
                }
            }
            itemsIndexed(plan.session.exercises) { index, ex ->
                V4ExerciseCard(date, index, ex, store, refresh) { seconds ->
                    rest = seconds
                    restOpen = seconds > 0
                    refresh++
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (restOpen) {
        ModalBottomSheet(onDismissRequest = { restOpen = false }, containerColor = NztSurface) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(V3Strings.t(lang, "timer"), color = NztMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(formatSeconds(rest), color = NztAccent, fontSize = 58.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { rest = (rest - 15).coerceAtLeast(0) }, modifier = Modifier.weight(1f)) { Text("-15s") }
                    OutlinedButton(onClick = { rest += 15 }, modifier = Modifier.weight(1f)) { Text("+15s") }
                    Button(onClick = { restOpen = false }, modifier = Modifier.weight(1f)) { Text(V3Strings.t(lang, "done")) }
                }
            }
        }
    }

    if (finishDialog) {
        AlertDialog(
            onDismissRequest = { finishDialog = false },
            title = { Text(V3Strings.t(lang, "finish"), fontWeight = FontWeight.Black) },
            text = { Text("$doneSets / $totalSets • ${(pct * 100).roundToInt()}%") },
            confirmButton = {
                Button(onClick = {
                    plan.session.exercises.forEach { ex ->
                        val states = (0 until ex.sets).map { store.load(date, ex.name, it, defaultReps(ex.target)) }
                        if (isTimedTarget(ex.target)) {
                            if (states.any { it.done }) store.setPrevious(ex.name, ex.target)
                        } else {
                            val best = states.mapNotNull { it.kg.replace(',', '.').toDoubleOrNull() }.maxOrNull() ?: 0.0
                            if (best > store.best(ex.name)) store.setBest(ex.name, best)
                            val last = states.firstOrNull { it.done }?.let { s -> "${s.kg.ifBlank { "BW" }} × ${s.reps}" } ?: "—"
                            store.setPrevious(ex.name, last)
                        }
                    }
                    store.markWorkout(date, plan.session.code)
                    onClose()
                }) { Text(V3Strings.t(lang, "done")) }
            },
            dismissButton = { TextButton(onClick = { finishDialog = false }) { Text(V3Strings.t(lang, "back")) } },
            containerColor = NztSurface
        )
    }
}

@Composable
private fun V4WorkoutMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = NztAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(5.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun V4ExerciseCard(
    date: LocalDate,
    index: Int,
    ex: StathamExercise,
    store: V4WorkoutStore,
    refresh: Int,
    onSetDone: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    var infoOpen by remember { mutableStateOf(false) }
    val default = defaultReps(ex.target)
    val states = remember(refresh, date, ex.name) { (0 until ex.sets).map { store.load(date, ex.name, it, default) } }
    val done = states.count { it.done }
    val best = store.best(ex.name)
    val timed = isTimedTarget(ex.target)

    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth()) {
            ExercisePhoto(ex.name, Modifier.fillMaxWidth().height(190.dp))
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${index + 1}. ${ex.name}", fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            exerciseMeta(lang, ex, timed),
                            color = NztMuted,
                            fontSize = 12.sp
                        )
                    }
                    Surface(color = if (done == ex.sets) NztAccent else NztSurface2, shape = CircleShape) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Text("$done/${ex.sets}", color = if (done == ex.sets) Color.Black else NztText, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V4InfoPill(V3Strings.t(lang, "previous"), store.previous(ex.name), Modifier.weight(1f))
                    if (!timed) {
                        V4InfoPill("PR", if (best > 0) "${formatKg(best)} kg" else "—", Modifier.weight(1f))
                    } else {
                        V4InfoPill(targetLabel(lang), ex.target, Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (timed) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text("SET", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(46.dp))
                        Text(targetLabel(lang).uppercase(), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(50.dp))
                    }
                    Spacer(Modifier.height(5.dp))
                    states.forEachIndexed { setIndex, initial ->
                        V4TimedSetRow(date, ex, setIndex, initial, store) { changed, justDone ->
                            store.save(date, ex.name, setIndex, changed)
                            if (justDone) onSetDone(ex.restSeconds)
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text("SET", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                        Text("KG", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(V3Strings.t(lang, "reps").uppercase(), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("RIR", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(66.dp))
                        Spacer(Modifier.width(46.dp))
                    }
                    Spacer(Modifier.height(5.dp))
                    states.forEachIndexed { setIndex, initial ->
                        V4SetRow(date, ex, setIndex, initial, store) { changed, justDone ->
                            store.save(date, ex.name, setIndex, changed)
                            if (justDone) onSetDone(ex.restSeconds)
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                }

                TextButton(onClick = { infoOpen = !infoOpen }, contentPadding = PaddingValues(0.dp)) {
                    Icon(if (infoOpen) Icons.Default.ExpandLess else Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (infoOpen) V3Strings.t(lang, "close") else "${V3Strings.t(lang, "technique")} / ${V3Strings.t(lang, "progression")}")
                }

                if (infoOpen) {
                    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(V3Strings.t(lang, "technique").uppercase(), color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(ex.technique, color = NztText, fontSize = 13.sp, lineHeight = 19.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(V3Strings.t(lang, "progression").uppercase(), color = NztAccent2, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(ex.progression, color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V4TimedSetRow(
    date: LocalDate,
    ex: StathamExercise,
    setIndex: Int,
    initial: V4SetState,
    store: V4WorkoutStore,
    onSave: (V4SetState, Boolean) -> Unit
) {
    var done by remember(date, ex.name, setIndex, initial) { mutableStateOf(initial.done) }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Text((setIndex + 1).toString(), fontWeight = FontWeight.Bold)
            }
        }
        Surface(
            modifier = Modifier.weight(1f).height(50.dp),
            color = NztSurface2,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NztLine)
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, null, tint = NztAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(ex.target, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        FilledIconButton(
            onClick = {
                val wasDone = done
                done = !done
                onSave(initial.copy(done = done), !wasDone && done)
            },
            modifier = Modifier.size(46.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (done) NztAccent else NztSurface2,
                contentColor = if (done) Color.Black else NztText
            )
        ) { Icon(Icons.Default.Check, null) }
    }
}

@Composable
private fun V4SetRow(
    date: LocalDate,
    ex: StathamExercise,
    setIndex: Int,
    initial: V4SetState,
    store: V4WorkoutStore,
    onSave: (V4SetState, Boolean) -> Unit
) {
    var kg by remember(date, ex.name, setIndex, initial) { mutableStateOf(initial.kg) }
    var reps by remember(date, ex.name, setIndex, initial) { mutableStateOf(initial.reps) }
    var rir by remember(date, ex.name, setIndex, initial) { mutableIntStateOf(initial.rir) }
    var done by remember(date, ex.name, setIndex, initial) { mutableStateOf(initial.done) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text((setIndex + 1).toString(), fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = kg,
            onValueChange = {
                kg = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6)
                onSave(V4SetState(kg, reps, rir, done), false)
            },
            placeholder = { Text("BW", fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).height(52.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
        )
        OutlinedTextField(
            value = reps,
            onValueChange = {
                reps = it.filter(Char::isDigit).take(3)
                onSave(V4SetState(kg, reps, rir, done), false)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(52.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
        )
        Surface(
            color = NztSurface2,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(60.dp).height(48.dp).clickable {
                rir = (rir + 1) % 6
                onSave(V4SetState(kg, reps, rir, done), false)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(rir.toString(), color = NztAccent2, fontWeight = FontWeight.Bold)
            }
        }
        FilledIconButton(
            onClick = {
                val wasDone = done
                done = !done
                onSave(V4SetState(kg, reps, rir, done), !wasDone && done)
                if (done) {
                    val v = kg.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (v > store.best(ex.name)) store.setBest(ex.name, v)
                }
            },
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (done) NztAccent else NztSurface2,
                contentColor = if (done) Color.Black else NztText
            )
        ) { Icon(Icons.Default.Check, null) }
    }
}

@Composable
private fun V4InfoPill(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, NztLine)) {
        Column(Modifier.padding(10.dp)) {
            Text(label.uppercase(), color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = NztText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun isTimedTarget(target: String): Boolean {
    val s = target.lowercase()
    return listOf("min", "мин", "sec", "сек", "z1", "z2", "z3", "rpm", "час", "хв").any { it in s }
}

private fun defaultReps(target: String): Int {
    val nums = Regex("\\d+").findAll(target).mapNotNull { it.value.toIntOrNull() }.toList()
    return when {
        nums.size >= 2 -> nums[1]
        nums.isNotEmpty() -> nums.first()
        else -> 10
    }.coerceIn(1, 99)
}

private fun exerciseCountLabel(lang: AppLanguage, count: Int): String = when (lang) {
    AppLanguage.RU -> "$count упр."
    AppLanguage.EN -> "$count exercises"
    AppLanguage.PL -> "$count ćw."
    AppLanguage.UK -> "$count вправ"
}

private fun targetLabel(lang: AppLanguage): String = when (lang) {
    AppLanguage.RU -> "Цель"
    AppLanguage.EN -> "Target"
    AppLanguage.PL -> "Cel"
    AppLanguage.UK -> "Ціль"
}

private fun exerciseMeta(lang: AppLanguage, ex: StathamExercise, timed: Boolean): String {
    val rest = when (lang) {
        AppLanguage.RU -> "отдых"
        AppLanguage.EN -> "rest"
        AppLanguage.PL -> "przerwa"
        AppLanguage.UK -> "відпочинок"
    }
    val sets = when (lang) {
        AppLanguage.RU -> if (ex.sets == 1) "1 блок" else "${ex.sets} блока"
        AppLanguage.EN -> if (ex.sets == 1) "1 block" else "${ex.sets} blocks"
        AppLanguage.PL -> if (ex.sets == 1) "1 blok" else "${ex.sets} bloki"
        AppLanguage.UK -> if (ex.sets == 1) "1 блок" else "${ex.sets} блоки"
    }
    return if (timed) "$sets • ${ex.target} • ${ex.restSeconds}s $rest"
    else "${ex.sets} × ${ex.target} • ${ex.restSeconds}s $rest"
}

private fun formatSeconds(s: Int): String = "%d:%02d".format(s / 60, s % 60)
private fun formatKg(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
