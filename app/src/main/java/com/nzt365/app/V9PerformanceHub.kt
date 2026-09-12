package com.nzt365.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import java.time.LocalDate
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

private enum class V9HubTab { OVERVIEW, LIBRARY, HISTORY, REVIEW }
private data class V9DayStats(
    val date: LocalDate,
    val title: String,
    val code: String,
    val done: Boolean,
    val planned: Boolean,
    val sets: Int,
    val volume: Int,
    val readiness: Int?,
    val avgRir: Double?
)

private data class V9AdaptiveResult(
    val weekKey: String,
    val completion: Int,
    val readiness: Int?,
    val avgRir: Double?,
    val action: Int,
    val changedExercises: List<String>
)

private class V9HubStore(context: Context) {
    private val workout = context.getSharedPreferences("nzt_workout_v7", Context.MODE_PRIVATE)
    private val product = context.getSharedPreferences("nzt_product_v7", Context.MODE_PRIVATE)
    private val adaptive = context.getSharedPreferences("nzt_adaptive_v9", Context.MODE_PRIVATE)

    private fun setKey(date: LocalDate, exercise: String, set: Int, field: String) =
        "${date}_${exercise.hashCode()}_${set}_$field"

    fun day(date: LocalDate): V9DayStats {
        val plan = StathamEngine.forDate(date)
        val planned = plan.session.code != "REST"
        var sets = 0
        var volume = 0.0
        val rirs = mutableListOf<Int>()
        plan.session.exercises.forEach { exercise ->
            val maxSets = (exercise.sets + workout.getInt("extra_${exercise.name.hashCode()}", 0)).coerceIn(1, 10)
            repeat(maxSets) { i ->
                if (workout.getBoolean(setKey(date, exercise.name, i, "done"), false)) {
                    sets++
                    val kg = workout.getString(setKey(date, exercise.name, i, "kg"), "")?.toDoubleOrNull() ?: 0.0
                    val reps = workout.getString(setKey(date, exercise.name, i, "reps"), "")?.toIntOrNull() ?: 0
                    volume += kg * reps
                    rirs += workout.getInt(setKey(date, exercise.name, i, "rir"), 2)
                }
            }
        }
        val done = workout.getBoolean("workout_${date}_${plan.session.code}", false)
        val rk = date.toString()
        val ready = if (product.getBoolean("ready_$rk", false)) {
            val sleep = product.getInt("sleep_$rk", 3)
            val energy = product.getInt("energy_$rk", 3)
            val soreness = product.getInt("soreness_$rk", 3)
            val stress = product.getInt("stress_$rk", 3)
            (((sleep + energy + (6 - soreness) + (6 - stress)) / 20.0) * 100).roundToInt()
        } else null
        return V9DayStats(date, plan.session.title, plan.session.code, done, planned, sets, volume.roundToInt(), ready, rirs.takeIf { it.isNotEmpty() }?.average())
    }

    fun recent(days: Int): List<V9DayStats> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { day(today.minusDays(it.toLong())) }
    }

    fun uniqueExercises(): List<StathamExercise> {
        val today = LocalDate.now()
        val map = linkedMapOf<String, StathamExercise>()
        for (offset in -30..180) {
            StathamEngine.forDate(today.plusDays(offset.toLong())).session.exercises.forEach { ex ->
                map.putIfAbsent(ex.name, ex)
            }
        }
        return map.values.sortedBy { it.name.lowercase() }
    }

    fun bestE1rm(name: String): Int = workout.getFloat("e1rm_${name.hashCode()}", 0f).roundToInt()
    fun bestVolume(name: String): Int = workout.getFloat("vol_${name.hashCode()}", 0f).roundToInt()

    fun loadTrend14(): List<Int> = recent(14).map { it.volume }

    fun currentAdaptive(): V9AdaptiveResult? {
        val wk = currentWeekKey()
        if (adaptive.getString("week_key", "") != wk) return null
        val changed = adaptive.getString("changed", "")?.split("\u001F")?.filter { it.isNotBlank() } ?: emptyList()
        val rirRaw = adaptive.getString("avg_rir", null)?.toDoubleOrNull()
        val readiness = adaptive.getInt("readiness", -1).takeIf { it >= 0 }
        return V9AdaptiveResult(
            weekKey = wk,
            completion = adaptive.getInt("completion", 0),
            readiness = readiness,
            avgRir = rirRaw,
            action = adaptive.getInt("action", 0),
            changedExercises = changed
        )
    }

    fun applyWeeklyIfNeeded(force: Boolean = false): V9AdaptiveResult {
        val wk = currentWeekKey()
        if (!force) currentAdaptive()?.let { return it }
        val previous = recent(7)
        val planned = previous.count { it.planned }
        val done = previous.count { it.planned && it.done }
        val completion = if (planned == 0) 100 else ((done * 100.0) / planned).roundToInt()
        val readyValues = previous.mapNotNull { it.readiness }
        val readiness = readyValues.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val rirValues = previous.mapNotNull { it.avgRir }
        val avgRir = rirValues.takeIf { it.isNotEmpty() }?.average()
        val action = when {
            readiness != null && readiness < 55 -> -1
            completion < 55 -> -1
            completion >= 80 && (readiness ?: 70) >= 70 && (avgRir ?: 2.0) >= 1.5 -> 1
            else -> 0
        }
        val next = linkedSetOf<String>()
        val today = LocalDate.now()
        for (offset in 0..6) {
            val plan = StathamEngine.forDate(today.plusDays(offset.toLong()))
            if (plan.session.type == "Силовая") {
                plan.session.exercises.take(2).forEach { next += it.name }
            }
        }
        val changed = mutableListOf<String>()
        if (action != 0) {
            val editor = workout.edit()
            next.forEach { name ->
                val key = "extra_${name.hashCode()}"
                val current = workout.getInt(key, 0)
                val updated = (current + action).coerceIn(-1, 2)
                if (updated != current) {
                    editor.putInt(key, updated)
                    changed += name
                }
            }
            editor.apply()
        }
        adaptive.edit()
            .putString("week_key", wk)
            .putInt("completion", completion)
            .putInt("readiness", readiness ?: -1)
            .putString("avg_rir", avgRir?.let { "%.2f".format(it) })
            .putInt("action", action)
            .putString("changed", changed.joinToString("\u001F"))
            .apply()
        return V9AdaptiveResult(wk, completion, readiness, avgRir, action, changed)
    }

    private fun currentWeekKey(): String {
        val d = LocalDate.now()
        val wf = WeekFields.ISO
        return "${d.get(wf.weekBasedYear())}-${d.get(wf.weekOfWeekBasedYear())}"
    }
}

@Composable
fun NZT9Root(repo: NZTRepository, profileStore: ProfileStore) {
    var showHub by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val store = remember { V9HubStore(context) }
    LaunchedEffect(profileStore.isOnboarded()) {
        if (profileStore.isOnboarded()) store.applyWeeklyIfNeeded()
    }

    Box(Modifier.fillMaxSize()) {
        NZT7Root(repo, profileStore)
        if (profileStore.isOnboarded() && !showHub) {
            FloatingActionButton(
                onClick = { showHub = true },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 92.dp).size(50.dp),
                containerColor = NztAccent,
                contentColor = Color.Black,
                shape = RoundedCornerShape(17.dp)
            ) { Icon(Icons.Default.Insights, "Performance Lab", modifier = Modifier.size(24.dp)) }
        }
        if (showHub) {
            Surface(Modifier.fillMaxSize(), color = NztBg) {
                V9Hub(repo, profileStore.language(), store) { showHub = false }
            }
        }
    }
}

@Composable
private fun V9Hub(repo: NZTRepository, lang: AppLanguage, store: V9HubStore, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(V9HubTab.OVERVIEW) }
    var refresh by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
                    Column(Modifier.weight(1f)) {
                        Text("NZT PERFORMANCE HUB", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text(v9t(lang, "Тренировки, история и адаптация", "Training, history & adaptation", "Trening, historia i adaptacja", "Тренування, історія й адаптація"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                        Text("V9", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NztAccent, fontWeight = FontWeight.Black)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    V9HubTab.entries.forEach { item ->
                        FilterChip(
                            selected = tab == item,
                            onClick = { tab = item },
                            label = { Text(v9TabLabel(lang, item), fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            key(refresh) {
                when (tab) {
                    V9HubTab.OVERVIEW -> V9Overview(repo, lang, store)
                    V9HubTab.LIBRARY -> V9ExerciseLibrary(lang, store)
                    V9HubTab.HISTORY -> V9History(lang, store)
                    V9HubTab.REVIEW -> V9WeeklyReview(lang, store) { store.applyWeeklyIfNeeded(force = true); refresh++ }
                }
            }
        }
    }
}

@Composable
private fun V9Overview(repo: NZTRepository, lang: AppLanguage, store: V9HubStore) {
    val days = remember { store.recent(14) }
    val last7 = days.takeLast(7)
    val workouts = last7.count { it.done }
    val sets = last7.sumOf { it.sets }
    val volume = last7.sumOf { it.volume }
    val readiness = last7.mapNotNull { it.readiness }.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    val adaptive = remember { store.currentAdaptive() ?: store.applyWeeklyIfNeeded() }
    val scores = repo.lastDailyScores(7).map { it.second }
    val nzt = scores.takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: 0

    LazyColumn(contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10222A)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = NztAccent)
                        Spacer(Modifier.width(9.dp))
                        Text("NZT ADAPTIVE COACH", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(v9CoachText(lang, adaptive, readiness), fontSize = 15.sp, lineHeight = 21.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V9Metric(v9t(lang,"Тренировок","Workouts","Treningi","Тренувань"), workouts.toString(), Modifier.weight(1f))
                V9Metric(v9t(lang,"Подходов","Sets","Serie","Підходів"), sets.toString(), Modifier.weight(1f))
                V9Metric("NZT", nzt.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v9t(lang,"Нагрузка · 14 дней","Training load · 14 days","Obciążenie · 14 dni","Навантаження · 14 днів"), color = NztMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(14.dp))
                    V9LoadBars(days.map { it.volume })
                    Spacer(Modifier.height(10.dp))
                    Text("$volume kg · ${v9t(lang,"объём за 7 дней","7-day volume","objętość 7 dni","обсяг за 7 днів")}", color = NztAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = NztAccent)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v9t(lang,"Готовность","Readiness","Gotowość","Готовність"), fontWeight = FontWeight.Black)
                        Text(readiness?.let { "$it / 100" } ?: "—", color = NztMuted)
                    }
                    Text(when (adaptive.action) { 1 -> "+1 SET"; -1 -> "−1 SET"; else -> "HOLD" }, color = NztAccent, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun V9ExerciseLibrary(lang: AppLanguage, store: V9HubStore) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ALL") }
    var selected by remember { mutableStateOf<StathamExercise?>(null) }
    val all = remember { store.uniqueExercises() }
    val cats = listOf("ALL", "PUSH", "PULL", "LEGS", "CORE", "CARDIO", "MOBILITY")
    val filtered = all.filter { ex ->
        val q = query.trim().lowercase()
        (q.isBlank() || ex.name.lowercase().contains(q) || ex.technique.lowercase().contains(q)) &&
            (category == "ALL" || v9Category(ex.name) == category)
    }

    LazyColumn(contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(v9t(lang,"Найти упражнение","Search exercise","Szukaj ćwiczenia","Знайти вправу")) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                cats.forEach { c -> FilterChip(selected = c == category, onClick = { category = c }, label = { Text(c, fontSize = 10.sp) }) }
            }
        }
        item {
            Text("${filtered.size} ${v9t(lang,"упражнений","exercises","ćwiczeń","вправ")}", color = NztMuted, fontSize = 11.sp)
        }
        items(filtered, key = { it.name }) { ex ->
            Card(
                Modifier.fillMaxWidth().clickable { selected = ex },
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.size(92.dp)) {
                        ExercisePhoto(ex.name, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${ex.sets} × ${ex.target} · ${ex.restSeconds}s", color = NztMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(onClick = { selected = ex }, label = { Text(v9Category(ex.name), fontSize = 9.sp) })
                            val best = store.bestE1rm(ex.name)
                            if (best > 0) AssistChip(onClick = { selected = ex }, label = { Text("e1RM $best", fontSize = 9.sp) })
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = NztMuted)
                }
            }
        }
    }

    selected?.let { ex ->
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = NztSurface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(18.dp)) {
                ExercisePhoto(ex.name, Modifier.fillMaxWidth().height(220.dp))
                Spacer(Modifier.height(14.dp))
                Text(ex.name, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("${ex.sets} × ${ex.target} · rest ${ex.restSeconds}s", color = NztAccent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text(v9t(lang,"Техника","Technique","Technika","Техніка"), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(ex.technique, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                Text(v9t(lang,"Прогрессия","Progression","Progresja","Прогресія"), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(ex.progression, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V9Metric("e1RM", store.bestE1rm(ex.name).takeIf { it > 0 }?.let { "$it kg" } ?: "—", Modifier.weight(1f))
                    V9Metric(v9t(lang,"Лучший объём","Best volume","Najl. objętość","Найкр. обсяг"), store.bestVolume(ex.name).takeIf { it > 0 }?.let { "$it kg" } ?: "—", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun V9History(lang: AppLanguage, store: V9HubStore) {
    val today = LocalDate.now()
    var selected by remember { mutableStateOf(today) }
    val days = remember { (34 downTo 0).map { store.day(today.minusDays(it.toLong())) } }
    val detail = remember(selected) { store.day(selected) }
    LazyColumn(contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(v9t(lang,"Последние 5 недель","Last 5 weeks","Ostatnie 5 tygodni","Останні 5 тижнів"), fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        items(days.chunked(7)) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { d ->
                    Surface(
                        modifier = Modifier.weight(1f).height(64.dp).clickable { selected = d.date },
                        color = when { d.date == selected -> NztAccent; d.done -> Color(0xFF173B35); else -> NztSurface },
                        contentColor = if (d.date == selected) Color.Black else NztText,
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(d.date.dayOfMonth.toString(), fontWeight = FontWeight.Black)
                            Text(d.date.dayOfWeek.name.take(2), fontSize = 8.sp)
                            if (d.done) Text("●", color = if (d.date == selected) Color.Black else NztAccent, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(detail.date.toString(), color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Text(detail.title, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(detail.code, color = NztMuted)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V9Metric(v9t(lang,"Статус","Status","Status","Статус"), if (detail.done) "DONE" else if (detail.planned) "PLANNED" else "REST", Modifier.weight(1f))
                        V9Metric(v9t(lang,"Подходы","Sets","Serie","Підходи"), detail.sets.toString(), Modifier.weight(1f))
                        V9Metric(v9t(lang,"Объём","Volume","Objętość","Обсяг"), detail.volume.toString(), Modifier.weight(1f))
                    }
                    detail.readiness?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Readiness $it/100", color = NztMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V9WeeklyReview(lang: AppLanguage, store: V9HubStore, onReapply: () -> Unit) {
    val result = remember { store.currentAdaptive() ?: store.applyWeeklyIfNeeded() }
    val actionText = when (result.action) {
        1 -> v9t(lang,"Прогрессия: +1 рабочий подход в ключевых упражнениях следующей недели.","Progression: +1 working set on key lifts next week.","Progresja: +1 seria robocza w kluczowych ćwiczeniach w przyszłym tygodniu.","Прогресія: +1 робочий підхід у ключових вправах наступного тижня.")
        -1 -> v9t(lang,"Разгрузка: −1 рабочий подход в ключевых упражнениях следующей недели.","Deload: −1 working set on key lifts next week.","Deload: −1 seria robocza w kluczowych ćwiczeniach w przyszłym tygodniu.","Розвантаження: −1 робочий підхід у ключових вправах наступного тижня.")
        else -> v9t(lang,"Нагрузка сохранена: текущий объём выглядит адекватно.","Load held: current volume looks appropriate.","Obciążenie bez zmian: obecna objętość wygląda odpowiednio.","Навантаження без змін: поточний обсяг виглядає доречно.")
    }
    LazyColumn(contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10222A)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("AUTO WEEKLY REVIEW", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(actionText, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(v9t(lang,"Этот результат уже применён к плану. Экран тренировки автоматически использует изменённое количество подходов.","This result is already applied to the plan. The workout screen automatically uses the adjusted set count.","Wynik jest już zastosowany do planu. Ekran treningu automatycznie używa zmienionej liczby serii.","Результат уже застосовано до плану. Екран тренування автоматично використовує змінену кількість підходів."), color = NztMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V9Metric(v9t(lang,"Выполнение","Completion","Realizacja","Виконання"), "${result.completion}%", Modifier.weight(1f))
                V9Metric(v9t(lang,"Готовность","Readiness","Gotowość","Готовність"), result.readiness?.toString() ?: "—", Modifier.weight(1f))
                V9Metric("RIR", result.avgRir?.let { "%.1f".format(it) } ?: "—", Modifier.weight(1f))
            }
        }
        if (result.changedExercises.isNotEmpty()) {
            item { Text(v9t(lang,"Изменённые упражнения","Adjusted exercises","Zmienione ćwiczenia","Змінені вправи"), fontWeight = FontWeight.Black) }
            items(result.changedExercises) { name ->
                Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (result.action > 0) Icons.Default.TrendingUp else Icons.Default.SouthEast, null, tint = NztAccent)
                        Spacer(Modifier.width(10.dp))
                        Text(name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(if (result.action > 0) "+1" else "−1", color = NztAccent, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onReapply, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(v9t(lang,"Пересчитать сейчас","Recalculate now","Przelicz teraz","Перерахувати зараз"))
            }
        }
    }
}

@Composable
private fun V9Metric(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(value, color = NztAccent, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun V9LoadBars(values: List<Int>) {
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { index, v ->
            val ratio = v.toFloat() / max
            Box(
                Modifier.weight(1f).fillMaxHeight((0.08f + ratio * 0.92f).coerceIn(0.08f, 1f))
                    .background(if (index == values.lastIndex) NztAccent else Color(0xFF2B4653), RoundedCornerShape(6.dp))
            )
        }
    }
}

private fun v9Category(name: String): String {
    val n = name.lowercase()
    fun has(vararg x: String) = x.any { n.contains(it) }
    return when {
        has("bike", "run", "бег", "вел", "кардио") -> "CARDIO"
        has("mob", "stretch", "размин", "мобил") -> "MOBILITY"
        has("plank", "core", "press", "crunch", "пресс", "планк") -> "CORE"
        has("squat", "lunge", "deadlift", "hinge", "присед", "выпад", "станов") -> "LEGS"
        has("pull", "row", "chin", "тяга", "подтяг") -> "PULL"
        else -> "PUSH"
    }
}

private fun v9CoachText(lang: AppLanguage, r: V9AdaptiveResult, readiness: Int?): String = when {
    readiness != null && readiness < 50 -> v9t(lang,
        "Сегодня готовность низкая. Работай технично, держи 2–3 RIR и не гонись за рекордами.",
        "Readiness is low today. Keep clean technique, 2–3 RIR and skip record chasing.",
        "Gotowość jest dziś niska. Trzymaj technikę, 2–3 RIR i nie poluj na rekordy.",
        "Готовність сьогодні низька. Тримай техніку, 2–3 RIR і не женись за рекордами.")
    r.action > 0 -> v9t(lang,
        "Ты стабильно закрываешь план и сохраняешь запас. Система повысила объём следующей недели точечно.",
        "You are completing the plan with reserve. The system increased next week's volume selectively.",
        "Regularnie realizujesz plan z zapasem. System punktowo zwiększył objętość przyszłego tygodnia.",
        "Ти стабільно закриваєш план із запасом. Система точково підвищила обсяг наступного тижня.")
    r.action < 0 -> v9t(lang,
        "Система увидела накопление усталости или низкую регулярность и снизила объём следующей недели.",
        "The system detected fatigue or low consistency and reduced next week's volume.",
        "System wykrył zmęczenie lub niską regularność i zmniejszył objętość przyszłego tygodnia.",
        "Система побачила втому або низьку регулярність і зменшила обсяг наступного тижня.")
    else -> v9t(lang,
        "Нагрузка сбалансирована. Закрой следующую тренировку и прогрессируй вес только при чистой технике.",
        "Load is balanced. Complete the next session and add weight only with clean form.",
        "Obciążenie jest zbalansowane. Ukończ następny trening i zwiększaj ciężar tylko przy czystej technice.",
        "Навантаження збалансоване. Заверши наступне тренування і додавай вагу лише з чистою технікою.")
}

private fun v9TabLabel(lang: AppLanguage, tab: V9HubTab): String = when (tab) {
    V9HubTab.OVERVIEW -> v9t(lang,"Обзор","Overview","Przegląd","Огляд")
    V9HubTab.LIBRARY -> v9t(lang,"Упражнения","Exercises","Ćwiczenia","Вправи")
    V9HubTab.HISTORY -> v9t(lang,"История","History","Historia","Історія")
    V9HubTab.REVIEW -> v9t(lang,"Адаптация","Adaptive","Adaptacja","Адаптація")
}

private fun v9t(lang: AppLanguage, ru: String, en: String, pl: String, uk: String): String = when (lang) {
    AppLanguage.RU -> ru
    AppLanguage.EN -> en
    AppLanguage.PL -> pl
    AppLanguage.UK -> uk
}
