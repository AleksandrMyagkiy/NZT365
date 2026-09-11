package com.nzt365.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.roundToInt

private enum class V8LabTab { OVERVIEW, RECOVERY, TRENDS }

private data class MuscleRecovery(val name: String, val score: Int, val sets7: Int)
private data class DayLoad(val date: LocalDate, val volume: Int, val sets: Int, val done: Boolean)

private class V8LabStore(private val context: Context) {
    private val workout = context.getSharedPreferences("nzt_workout_v7", Context.MODE_PRIVATE)
    private val product = context.getSharedPreferences("nzt_product_v7", Context.MODE_PRIVATE)

    private fun setKey(date: LocalDate, exercise: String, set: Int, field: String) =
        "${date}_${exercise.hashCode()}_${set}_$field"

    fun workoutDone(date: LocalDate): Boolean {
        val code = StathamEngine.forDate(date).session.code
        return workout.getBoolean("workout_${date}_$code", false)
    }

    fun dayLoad(date: LocalDate): DayLoad {
        val plan = StathamEngine.forDate(date)
        var volume = 0.0
        var sets = 0
        plan.session.exercises.forEach { exercise ->
            val maxSets = (exercise.sets + workout.getInt("extra_${exercise.name.hashCode()}", 0)).coerceIn(1, 10)
            repeat(maxSets) { i ->
                val done = workout.getBoolean(setKey(date, exercise.name, i, "done"), false)
                if (done) {
                    val kg = workout.getString(setKey(date, exercise.name, i, "kg"), "")?.toDoubleOrNull() ?: 0.0
                    val reps = workout.getString(setKey(date, exercise.name, i, "reps"), "")?.toIntOrNull() ?: 0
                    volume += kg * reps
                    sets++
                }
            }
        }
        return DayLoad(date, volume.roundToInt(), sets, workoutDone(date))
    }

    fun readiness(date: LocalDate): Int? {
        val k = date.toString()
        if (!product.getBoolean("ready_$k", false)) return null
        val sleep = product.getInt("sleep_$k", 3)
        val energy = product.getInt("energy_$k", 3)
        val soreness = product.getInt("soreness_$k", 3)
        val stress = product.getInt("stress_$k", 3)
        return (((sleep + energy + (6 - soreness) + (6 - stress)) / 20.0) * 100).roundToInt().coerceIn(0, 100)
    }

    fun bestE1rm(exercise: String): Double = workout.getFloat("e1rm_${exercise.hashCode()}", 0f).toDouble()

    fun recentLoads(days: Int): List<DayLoad> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { dayLoad(today.minusDays(it.toLong())) }
    }

    fun muscleRecovery(): List<MuscleRecovery> {
        val groups = linkedMapOf(
            "Chest" to 0.0,
            "Back" to 0.0,
            "Shoulders" to 0.0,
            "Quads" to 0.0,
            "Hamstrings" to 0.0,
            "Arms" to 0.0,
            "Core" to 0.0
        )
        val sets = groups.keys.associateWith { 0 }.toMutableMap()
        val today = LocalDate.now()

        for (offset in 0..6) {
            val date = today.minusDays(offset.toLong())
            val plan = StathamEngine.forDate(date)
            val freshnessFactor = 1.0 - (offset / 7.0)
            plan.session.exercises.forEach { exercise ->
                val maxSets = (exercise.sets + workout.getInt("extra_${exercise.name.hashCode()}", 0)).coerceIn(1, 10)
                var completed = 0
                repeat(maxSets) { i ->
                    if (workout.getBoolean(setKey(date, exercise.name, i, "done"), false)) completed++
                }
                if (completed > 0) {
                    muscleGroups(exercise.name).forEach { group ->
                        groups[group] = (groups[group] ?: 0.0) + completed * 9.0 * freshnessFactor
                        sets[group] = (sets[group] ?: 0) + completed
                    }
                }
            }
        }

        return groups.map { (name, debt) ->
            MuscleRecovery(name, (100 - debt).roundToInt().coerceIn(0, 100), sets[name] ?: 0)
        }
    }

    fun discoveredExercises(): List<Pair<String, Double>> {
        val names = linkedSetOf<String>()
        val today = LocalDate.now()
        for (offset in 0..90) {
            StathamEngine.forDate(today.minusDays(offset.toLong())).session.exercises.forEach { names += it.name }
        }
        return names.mapNotNull { name ->
            val best = bestE1rm(name)
            if (best > 0) name to best else null
        }.sortedByDescending { it.second }.take(8)
    }

    private fun muscleGroups(name: String): Set<String> {
        val n = name.lowercase()
        val result = linkedSetOf<String>()
        fun has(vararg terms: String) = terms.any { n.contains(it) }
        if (has("press", "push", "chest", "отжим", "жим")) result += "Chest"
        if (has("row", "pull", "chin", "тяга", "подтяг")) result += "Back"
        if (has("shoulder", "lateral", "overhead", "дельт", "плеч")) result += "Shoulders"
        if (has("squat", "lunge", "split", "step", "присед", "выпад")) result += "Quads"
        if (has("deadlift", "hinge", "rdl", "romanian", "hamstring", "станов", "ягод")) result += "Hamstrings"
        if (has("curl", "triceps", "dip", "biceps", "бицеп", "трицеп")) result += "Arms"
        if (has("plank", "crunch", "core", "dead bug", "raise", "пресс", "планк")) result += "Core"
        if (result.isEmpty() && has("bike", "run", "cycling", "бег", "вел")) {
            result += "Quads"; result += "Hamstrings"; result += "Core"
        }
        if (result.isEmpty()) result += "Core"
        return result
    }
}

@Composable
fun NZT8Root(repo: NZTRepository, profileStore: ProfileStore) {
    var showLab by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        NZT7Root(repo, profileStore)
        if (!showLab && profileStore.isOnboarded()) {
            ExtendedFloatingActionButton(
                onClick = { showLab = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 86.dp),
                containerColor = NztAccent,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Insights, null) },
                text = { Text("LAB", fontWeight = FontWeight.Black) }
            )
        }
        if (showLab) {
            Surface(Modifier.fillMaxSize(), color = NztBg) {
                V8PerformanceLab(repo, profileStore.language()) { showLab = false }
            }
        }
    }
}

@Composable
private fun V8PerformanceLab(repo: NZTRepository, lang: AppLanguage, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { V8LabStore(context) }
    var tab by remember { mutableStateOf(V8LabTab.OVERVIEW) }
    val loads = remember { store.recentLoads(28) }
    val muscles = remember { store.muscleRecovery() }
    val bests = remember { store.discoveredExercises() }
    val readiness = store.readiness(LocalDate.now())
    val workouts7 = loads.takeLast(7).count { it.done }
    val volume7 = loads.takeLast(7).sumOf { it.volume }
    val sets7 = loads.takeLast(7).sumOf { it.sets }
    val avgRecovery = muscles.map { it.score }.average().roundToInt()
    val score7 = repo.lastDailyScores(7).map { it.second }.average().roundToInt()

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) }
                Column(Modifier.weight(1f)) {
                    Text("NZT PERFORMANCE LAB", color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(v8t(lang, "Аналитика и адаптация", "Analytics & adaptation", "Analityka i adaptacja", "Аналітика й адаптація"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                    Text("V8", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = NztAccent, fontWeight = FontWeight.Black)
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                V8LabTab.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = tab == item,
                        onClick = { tab = item },
                        shape = SegmentedButtonDefaults.itemShape(index, V8LabTab.entries.size)
                    ) {
                        Text(when (item) {
                            V8LabTab.OVERVIEW -> v8t(lang, "Обзор", "Overview", "Przegląd", "Огляд")
                            V8LabTab.RECOVERY -> v8t(lang, "Восст.", "Recovery", "Regener.", "Відновл.")
                            V8LabTab.TRENDS -> v8t(lang, "Тренды", "Trends", "Trendy", "Тренди")
                        }, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                V8LabTab.OVERVIEW -> V8Overview(lang, workouts7, volume7, sets7, score7, readiness, avgRecovery, muscles)
                V8LabTab.RECOVERY -> V8Recovery(lang, muscles, store)
                V8LabTab.TRENDS -> V8Trends(lang, loads, bests)
            }
        }
    }
}

@Composable
private fun V8Overview(
    lang: AppLanguage,
    workouts7: Int,
    volume7: Int,
    sets7: Int,
    score7: Int,
    readiness: Int?,
    avgRecovery: Int,
    muscles: List<MuscleRecovery>
) {
    val weak = muscles.minByOrNull { it.score }
    val coach = when {
        readiness != null && readiness < 45 -> v8t(lang,
            "Снизь интенсивность: техника, лёгкая работа и восстановление важнее рекордов.",
            "Reduce intensity: technique, light work and recovery matter more than records today.",
            "Zmniejsz intensywność: technika i regeneracja są dziś ważniejsze niż rekordy.",
            "Знизь інтенсивність: техніка й відновлення сьогодні важливіші за рекорди.")
        avgRecovery < 55 -> v8t(lang,
            "Накопилась усталость. Сократи рабочие подходы примерно на 20% и оставь 2–3 RIR.",
            "Fatigue is building. Cut working sets by about 20% and keep 2–3 RIR.",
            "Narasta zmęczenie. Ogranicz serie robocze o ok. 20% i zostaw 2–3 RIR.",
            "Накопичується втома. Скороти робочі підходи приблизно на 20% і залиш 2–3 RIR.")
        workouts7 >= 4 -> v8t(lang,
            "Неделя идёт по плану. Если техника чистая, прогрессируй только в 1–2 ключевых упражнениях.",
            "The week is on track. If form is clean, progress only 1–2 key lifts.",
            "Tydzień idzie zgodnie z planem. Progresuj tylko w 1–2 kluczowych ćwiczeniach.",
            "Тиждень іде за планом. Прогресуй лише в 1–2 ключових вправах.")
        else -> v8t(lang,
            "Сейчас главный рычаг — регулярность. Закрой следующую тренировку полностью, без погони за весом.",
            "Consistency is the main lever now. Complete the next session fully before chasing load.",
            "Najważniejsza jest teraz regularność. Ukończ następny trening w całości, zanim zwiększysz ciężar.",
            "Головний важіль зараз — регулярність. Заверши наступне тренування повністю, перш ніж гнатися за вагою.")
    }

    LazyColumn(contentPadding = PaddingValues(14.dp, 10.dp, 14.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10222A)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = NztAccent)
                        Spacer(Modifier.width(10.dp))
                        Text("NZT ADAPTIVE COACH", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(coach, fontSize = 14.sp, lineHeight = 20.sp)
                    weak?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("${v8t(lang, "Самая уставшая зона", "Most fatigued area", "Najbardziej zmęczona strefa", "Найвтомленіша зона")}: ${it.name} • ${it.score}%", color = NztMuted, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V8Metric(v8t(lang,"Тренировок","Workouts","Treningi","Тренувань"), "$workouts7 / 7", Modifier.weight(1f))
                V8Metric(v8t(lang,"Подходов","Sets","Serie","Підходів"), sets7.toString(), Modifier.weight(1f))
                V8Metric(v8t(lang,"Объём","Volume","Objętość","Обсяг"), if (volume7 > 0) "${volume7 / 1000.0}k" else "—", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V8Metric("NZT 7D", "$score7", Modifier.weight(1f))
                V8Metric(v8t(lang,"Готовн.","Ready","Gotow.","Готовн."), readiness?.let { "$it%" } ?: "—", Modifier.weight(1f))
                V8Metric(v8t(lang,"Восст.","Recovery","Regener.","Відновл."), "$avgRecovery%", Modifier.weight(1f))
            }
        }
        item { V8MiniRecovery(lang, muscles) }
        item {
            Surface(color = NztSurface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v8t(lang,"Почему это сильнее обычного трекера","Why this goes beyond a basic tracker","Dlaczego to więcej niż zwykły tracker","Чому це більше за звичайний трекер"), fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(v8t(lang,
                        "NZT объединяет фактическую нагрузку, восстановление, готовность и дисциплину дня в одной рекомендации — а не просто хранит подходы.",
                        "NZT combines actual training load, muscle recovery, readiness and daily discipline into one recommendation instead of only storing sets.",
                        "NZT łączy realne obciążenie, regenerację mięśni, gotowość i dyscyplinę dnia w jedną rekomendację.",
                        "NZT поєднує фактичне навантаження, відновлення м'язів, готовність і дисципліну дня в одну рекомендацію."), color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun V8Recovery(lang: AppLanguage, muscles: List<MuscleRecovery>, store: V8LabStore) {
    val plan = StathamEngine.forDate(LocalDate.now())
    val targetGroups = plan.session.exercises.flatMap { ex ->
        listOf(ex.name)
    }
    val avg = muscles.map { it.score }.average().roundToInt()

    LazyColumn(contentPadding = PaddingValues(14.dp, 10.dp, 14.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Surface(color = NztSurface, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v8t(lang,"Карта восстановления","Recovery map","Mapa regeneracji","Карта відновлення"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(v8t(lang,"Оценка NZT по фактически выполненным подходам за последние 7 дней.","NZT estimate based on completed sets over the last 7 days.","Szacunek NZT na podstawie wykonanych serii z ostatnich 7 dni.","Оцінка NZT на основі виконаних підходів за останні 7 днів."), color = NztMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { avg / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = NztAccent, trackColor = NztLine)
                    Spacer(Modifier.height(5.dp))
                    Text("$avg%", color = NztAccent, fontWeight = FontWeight.Black)
                }
            }
        }
        muscles.forEach { item ->
            item {
                Surface(color = NztSurface, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("${item.score}%", color = when {
                                item.score >= 75 -> NztAccent2
                                item.score >= 50 -> NztAccent
                                else -> Color(0xFFFF8A80)
                            }, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(7.dp))
                        LinearProgressIndicator(progress = { item.score / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = when {
                            item.score >= 75 -> NztAccent2
                            item.score >= 50 -> NztAccent
                            else -> Color(0xFFFF8A80)
                        }, trackColor = NztLine)
                        Spacer(Modifier.height(5.dp))
                        Text("${item.sets7} ${v8t(lang,"подходов за 7 дней","sets in 7 days","serii w 7 dni","підходів за 7 днів")}", color = NztMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Surface(color = Color(0xFF10222A), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, tint = NztAccent)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(v8t(lang,"Сегодняшний план","Today's plan","Dzisiejszy plan","Сьогоднішній план"), color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("${plan.session.title} • ${plan.session.exercises.size} exercises", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun V8Trends(lang: AppLanguage, loads: List<DayLoad>, bests: List<Pair<String, Double>>) {
    val recent = loads.takeLast(14)
    val maxVol = recent.maxOfOrNull { it.volume }?.coerceAtLeast(1) ?: 1
    LazyColumn(contentPadding = PaddingValues(14.dp, 10.dp, 14.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(color = NztSurface, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v8t(lang,"Нагрузка · 14 дней","Training load · 14 days","Obciążenie · 14 dni","Навантаження · 14 днів"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        recent.forEach { day ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Box(Modifier.fillMaxWidth().height(((day.volume.toFloat() / maxVol) * 100f).coerceAtLeast(if (day.done) 8f else 2f).dp).background(if (day.done) NztAccent else NztLine, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                                Spacer(Modifier.height(5.dp))
                                Text(day.date.dayOfMonth.toString(), color = NztMuted, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(color = NztSurface, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v8t(lang,"Лучшие силовые показатели","Top strength markers","Najlepsze wskaźniki siły","Найкращі силові показники"), fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    if (bests.isEmpty()) {
                        Text(v8t(lang,"Заполни несколько силовых тренировок — здесь появятся e1RM и лидеры прогресса.","Log a few strength sessions and your e1RM leaders will appear here.","Zapisz kilka treningów siłowych, a pojawią się tu liderzy e1RM.","Запиши кілька силових тренувань — тут з'являться лідери e1RM."), color = NztMuted, fontSize = 13.sp)
                    } else {
                        bests.forEachIndexed { index, (name, value) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = NztSurface2, shape = RoundedCornerShape(10.dp)) { Text("${index + 1}", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = NztAccent, fontWeight = FontWeight.Black) }
                                Spacer(Modifier.width(10.dp))
                                Text(name, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${value.roundToInt()} kg", color = NztAccent2, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(color = Color(0xFF10222A), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("NZT SIGNAL", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    val last7 = loads.takeLast(7).sumOf { it.volume }
                    val prev7 = loads.dropLast(7).takeLast(7).sumOf { it.volume }
                    val delta = if (prev7 > 0) ((last7 - prev7) * 100.0 / prev7).roundToInt() else 0
                    Text(when {
                        delta > 20 -> v8t(lang,"Нагрузка резко выросла. Следи за сном, RIR и восстановлением.","Load jumped sharply. Watch sleep, RIR and recovery.","Obciążenie mocno wzrosło. Pilnuj snu, RIR i regeneracji.","Навантаження різко зросло. Стеж за сном, RIR і відновленням.")
                        delta < -20 -> v8t(lang,"Нагрузка заметно снизилась. Если это не deload — верни регулярность.","Load dropped noticeably. If this is not a deload, rebuild consistency.","Obciążenie wyraźnie spadło. Jeśli to nie deload, wróć do regularności.","Навантаження помітно знизилося. Якщо це не deload — поверни регулярність.")
                        else -> v8t(lang,"Нагрузка стабильна. Прогрессируй постепенно и сохраняй качество техники.","Load is stable. Progress gradually and protect movement quality.","Obciążenie jest stabilne. Progresuj stopniowo i dbaj o technikę.","Навантаження стабільне. Прогресуй поступово й бережи якість техніки.")
                    }, fontSize = 13.sp, lineHeight = 19.sp)
                    if (prev7 > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text("7D vs previous 7D: ${if (delta >= 0) "+" else ""}$delta%", color = NztMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V8Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = NztSurface, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(5.dp))
            Text(value, color = NztAccent, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun V8MiniRecovery(lang: AppLanguage, muscles: List<MuscleRecovery>) {
    Surface(color = NztSurface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(v8t(lang,"Восстановление мышц","Muscle recovery","Regeneracja mięśni","Відновлення м'язів"), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            muscles.sortedBy { it.score }.take(4).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, Modifier.width(86.dp), fontSize = 12.sp)
                    LinearProgressIndicator(progress = { item.score / 100f }, modifier = Modifier.weight(1f).height(6.dp), color = if (item.score >= 70) NztAccent2 else NztAccent, trackColor = NztLine)
                    Spacer(Modifier.width(8.dp))
                    Text("${item.score}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun v8t(lang: AppLanguage, ru: String, en: String, pl: String, uk: String): String = when (lang) {
    AppLanguage.RU -> ru
    AppLanguage.EN -> en
    AppLanguage.PL -> pl
    AppLanguage.UK -> uk
}
