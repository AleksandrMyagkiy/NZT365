package com.nzt365.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

private enum class V7Tab { TODAY, BODY, GROWTH, PROGRESS, SETTINGS }

private data class V7Readiness(
    val sleep: Int = 3,
    val energy: Int = 3,
    val soreness: Int = 3,
    val stress: Int = 3,
    val saved: Boolean = false
) {
    val score: Int
        get() = (((sleep + energy + (6 - soreness) + (6 - stress)) / 20.0) * 100)
            .roundToInt().coerceIn(0, 100)
}

private class V7ProductStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_product_v7", Context.MODE_PRIVATE)
    private val w = context.getSharedPreferences("nzt_workout_v7", Context.MODE_PRIVATE)

    fun readiness(date: LocalDate): V7Readiness {
        val k = date.toString()
        return V7Readiness(
            sleep = p.getInt("sleep_$k", 3),
            energy = p.getInt("energy_$k", 3),
            soreness = p.getInt("soreness_$k", 3),
            stress = p.getInt("stress_$k", 3),
            saved = p.getBoolean("ready_$k", false)
        )
    }

    fun saveReadiness(date: LocalDate, r: V7Readiness) {
        val k = date.toString()
        p.edit()
            .putInt("sleep_$k", r.sleep)
            .putInt("energy_$k", r.energy)
            .putInt("soreness_$k", r.soreness)
            .putInt("stress_$k", r.stress)
            .putBoolean("ready_$k", true)
            .apply()
    }

    fun workoutCount(): Int = w.getInt("workout_count", 0)
    fun water(date: LocalDate): Int = p.getInt("water_$date", 0)
    fun setWater(date: LocalDate, value: Int) {
        p.edit().putInt("water_$date", value.coerceIn(0, 16)).apply()
    }
}

@Composable
fun NZT7Root(repo: NZTRepository, profileStore: ProfileStore) {
    var onboarded by remember { mutableStateOf(profileStore.isOnboarded()) }
    var language by remember { mutableStateOf(profileStore.language()) }
    var profileTick by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalAppLanguage provides language) {
        NZTProTheme {
            if (!onboarded) {
                V7Onboarding(language) { name, lang ->
                    profileStore.save(name, lang)
                    language = lang
                    onboarded = true
                    profileTick++
                }
            } else {
                V7Shell(
                    repo = repo,
                    profile = profileStore,
                    lang = language,
                    profileTick = profileTick,
                    onLanguage = {
                        profileStore.setLanguage(it)
                        language = it
                        profileTick++
                    },
                    onProfileChanged = { profileTick++ }
                )
            }
        }
    }
}

@Composable
private fun V7Onboarding(initial: AppLanguage, onDone: (String, AppLanguage) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(initial) }

    Scaffold(containerColor = NztBg, contentWindowInsets = WindowInsets.safeDrawing) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.height(22.dp))
                Surface(color = NztAccent, shape = RoundedCornerShape(24.dp)) {
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        Text("N", color = Color.Black, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("NZT 365", fontSize = 43.sp, fontWeight = FontWeight.Black)
                Text("Performance OS for body, mind and progress", color = NztMuted, fontSize = 17.sp)
                Spacer(Modifier.height(30.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 28) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(v7Text(lang, "Имя", "Name", "Imię", "Ім'я")) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(v7Text(lang, "Язык", "Language", "Język", "Мова"), color = NztMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AppLanguage.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item ->
                            FilterChip(
                                selected = item == lang,
                                onClick = { lang = item },
                                label = { Text(item.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Button(
                onClick = { onDone(name.trim(), lang) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(v7Text(lang, "Начать", "Start", "Start", "Почати"), fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun V7Shell(
    repo: NZTRepository,
    profile: ProfileStore,
    lang: AppLanguage,
    profileTick: Int,
    onLanguage: (AppLanguage) -> Unit,
    onProfileChanged: () -> Unit
) {
    var tab by remember { mutableStateOf(V7Tab.TODAY) }
    val name = remember(profileTick) { profile.name() }

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF071018), tonalElevation = 0.dp) {
                V7Nav(tab, V7Tab.TODAY, Icons.Default.Home, v7Text(lang, "Сегодня", "Today", "Dzisiaj", "Сьогодні")) { tab = it }
                V7Nav(tab, V7Tab.BODY, Icons.Default.FitnessCenter, v7Text(lang, "Тело", "Body", "Ciało", "Тіло")) { tab = it }
                V7Nav(tab, V7Tab.GROWTH, Icons.Default.AutoGraph, v7Text(lang, "Рост", "Growth", "Rozwój", "Ріст")) { tab = it }
                V7Nav(tab, V7Tab.PROGRESS, Icons.Default.BarChart, v7Text(lang, "Прогресс", "Progress", "Postęp", "Прогрес")) { tab = it }
                V7Nav(tab, V7Tab.SETTINGS, Icons.Default.Settings, v7Text(lang, "Настр.", "Settings", "Ustaw.", "Налашт.")) { tab = it }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(NztBg)) {
            when (tab) {
                V7Tab.TODAY -> V7Today(repo, name, lang)
                V7Tab.BODY -> V7Body(repo, lang)
                V7Tab.GROWTH -> V5BooksScreen(lang, Modifier.fillMaxSize())
                V7Tab.PROGRESS -> V7Progress(repo, lang)
                V7Tab.SETTINGS -> V7Settings(profile, lang, onLanguage, onProfileChanged)
            }
        }
    }
}

@Composable
private fun RowScope.V7Nav(
    current: V7Tab,
    target: V7Tab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: (V7Tab) -> Unit
) {
    NavigationBarItem(
        selected = current == target,
        onClick = { onClick(target) },
        icon = { Icon(icon, label) },
        label = { Text(label, fontSize = 9.sp, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            indicatorColor = NztAccent,
            selectedTextColor = NztText,
            unselectedIconColor = NztMuted,
            unselectedTextColor = NztMuted
        )
    )
}

@Composable
private fun V7Header(name: String?, lang: AppLanguage, badge: String) {
    val hour = LocalDateTime.now().hour
    val greeting = when (lang) {
        AppLanguage.RU -> if (hour < 11) "Доброе утро" else if (hour < 18) "Добрый день" else "Добрый вечер"
        AppLanguage.EN -> if (hour < 11) "Good morning" else if (hour < 18) "Good afternoon" else "Good evening"
        AppLanguage.PL -> if (hour < 18) "Dzień dobry" else "Dobry wieczór"
        AppLanguage.UK -> if (hour < 11) "Доброго ранку" else if (hour < 18) "Добрий день" else "Добрий вечір"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("NZT 365", fontSize = 31.sp, fontWeight = FontWeight.Black)
            if (!name.isNullOrBlank()) Text("$greeting, $name", color = NztMuted, fontSize = 13.sp)
        }
        Surface(color = NztSurface2, shape = RoundedCornerShape(16.dp)) {
            Text(badge, Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = NztAccent, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}

@Composable
private fun V7Today(repo: NZTRepository, name: String, lang: AppLanguage) {
    val context = LocalContext.current
    val store = remember { V7ProductStore(context) }
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(plan) { StathamEngine.targets(plan) }
    var tasks by remember { mutableStateOf(repo.tasksForToday()) }
    var readiness by remember { mutableStateOf(store.readiness(date)) }
    var showReadiness by remember { mutableStateOf(false) }
    val score = repo.completionPercent(tasks)

    val coach = when {
        !readiness.saved -> v7Text(lang,
            "Сначала оцени готовность — тренировка адаптируется под твой день.",
            "Check readiness first — training adapts to your day.",
            "Najpierw oceń gotowość — trening dopasuje się do dnia.",
            "Спочатку оціни готовність — тренування адаптується до дня.")
        readiness.score < 45 -> v7Text(lang,
            "Сегодня приоритет техника, мобильность и восстановление. Не гонись за PR.",
            "Prioritize technique and recovery today. Skip PR chasing.",
            "Dziś priorytetem jest technika i regeneracja. Bez pogoni za PR.",
            "Сьогодні пріоритет — техніка й відновлення. Без гонитви за PR.")
        readiness.score < 70 -> v7Text(lang,
            "Работай по плану, но оставляй 2–3 RIR в основных упражнениях.",
            "Train as planned, keep 2–3 RIR on main lifts.",
            "Trenuj zgodnie z planem, zostaw 2–3 RIR.",
            "Тренуйся за планом, залишай 2–3 RIR.")
        else -> v7Text(lang,
            "Высокая готовность: можно атаковать прогрессию при чистой технике.",
            "High readiness: push progression with clean form.",
            "Wysoka gotowość: możesz atakować progresję przy dobrej technice.",
            "Висока готовність: можна атакувати прогресію з чистою технікою.")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { V7Header(name, lang, "DAY ${repo.dayNumber()}/365") }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V7ScoreCard("NZT SCORE", score, Modifier.weight(1f))
                V7ScoreCard(
                    v7Text(lang, "ГОТОВНОСТЬ", "READINESS", "GOTOWOŚĆ", "ГОТОВНІСТЬ"),
                    if (readiness.saved) readiness.score else -1,
                    Modifier.weight(1f)
                ) { showReadiness = true }
            }
        }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10222A)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = NztAccent, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("NZT COACH", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text(coach, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column {
                    Box {
                        ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(220.dp))
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD071018)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                            Text("TODAY'S SESSION", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            Text(plan.session.title, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 2)
                            Text("${plan.session.minutes} min • ${plan.session.exercises.size} exercises", color = Color.White.copy(alpha = .78f))
                        }
                    }
                    Column(Modifier.padding(16.dp)) {
                        Button(
                            onClick = { context.startActivity(Intent(context, V7WorkoutActivity::class.java).putExtra("date", date.toString())) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(17.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(v7Text(lang, "Начать тренировку", "Start workout", "Rozpocznij trening", "Почати тренування"), fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString())) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restaurant, null)
                                Spacer(Modifier.width(5.dp))
                                Text("${targets.calories} kcal")
                            }
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(context, ProTrainingLibraryActivity::class.java).putExtra("date", date.toString())) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SwapHoriz, null)
                                Spacer(Modifier.width(5.dp))
                                Text(v7Text(lang, "Заменить", "Replace", "Zamień", "Замінити"))
                            }
                        }
                    }
                }
            }
        }
        item { V7Section(v7Text(lang, "Действия дня", "Daily actions", "Działania dnia", "Дії дня"), "02") }
        items(tasks, key = { it.id }) { task ->
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.done >= task.target,
                        onCheckedChange = { checked ->
                            tasks = tasks.map { if (it.id == task.id) it.copy(done = if (checked) it.target else 0) else it }
                            repo.saveTasks(tasks)
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        Text(task.category, color = NztMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    if (showReadiness) {
        V7ReadinessDialog(
            initial = readiness,
            onDismiss = { showReadiness = false },
            onSave = {
                readiness = it.copy(saved = true)
                store.saveReadiness(date, readiness)
                showReadiness = false
            }
        )
    }
}

@Composable
private fun V7ScoreCard(label: String, value: Int, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier.then(clickModifier),
        colors = CardDefaults.cardColors(containerColor = NztSurface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(if (value < 0) "—" else value.toString(), fontSize = 31.sp, fontWeight = FontWeight.Black, color = if (value >= 70) NztAccent else NztText)
            Text(if (value < 0) "CHECK-IN" else "/ 100", color = NztMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun V7Section(title: String, code: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(code, color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V7ReadinessDialog(initial: V7Readiness, onDismiss: () -> Unit, onSave: (V7Readiness) -> Unit) {
    var r by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("READINESS", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                V7Scale("Sleep", r.sleep) { r = r.copy(sleep = it) }
                V7Scale("Energy", r.energy) { r = r.copy(energy = it) }
                V7Scale("Soreness", r.soreness) { r = r.copy(soreness = it) }
                V7Scale("Stress", r.stress) { r = r.copy(stress = it) }
                Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                    Text("${r.score}/100", Modifier.fillMaxWidth().padding(14.dp), color = NztAccent, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(r) }) { Text("SAVE") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
        containerColor = NztSurface
    )
}

@Composable
private fun V7Scale(label: String, value: Int, onValue: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(value.toString(), color = NztAccent, fontWeight = FontWeight.Black)
        }
        Slider(value = value.toFloat(), onValueChange = { onValue(it.roundToInt().coerceIn(1, 5)) }, valueRange = 1f..5f, steps = 3)
    }
}

@Composable
private fun V7Body(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current
    val store = remember { V7ProductStore(context) }
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val last = repo.bodyLogs().firstOrNull()
    var water by remember { mutableIntStateOf(store.water(date)) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { V7Header(null, lang, v7Text(lang, "ТЕЛО", "BODY", "CIAŁO", "ТІЛО")) }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column {
                    ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(210.dp))
                    Column(Modifier.padding(16.dp)) {
                        Text(plan.session.title, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text(plan.phase.focus, color = NztMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { context.startActivity(Intent(context, V7WorkoutActivity::class.java).putExtra("date", date.toString())) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(v7Text(lang, "ТРЕНИРОВАТЬСЯ", "TRAIN NOW", "TRENUJ", "ТРЕНУВАТИСЯ"), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        item { V7Section(v7Text(lang, "Инструменты производительности", "Performance tools", "Narzędzia wydajności", "Інструменти продуктивності"), "01") }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                V7ToolCard(Icons.Default.Calculate, "Plate calc", "Inside workout", Modifier.weight(1f))
                V7ToolCard(Icons.Default.Whatshot, "Smart warm-up", "40/60/75/90%", Modifier.weight(1f))
                V7ToolCard(Icons.Default.Timer, "Rest timer", "Per exercise", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                V7ToolCard(Icons.Default.TrendingUp, "e1RM", "Live estimate", Modifier.weight(1f))
                V7ToolCard(Icons.Default.Bolt, "RIR", "0–5 per set", Modifier.weight(1f))
                V7ToolCard(Icons.Default.Flag, "PR", "Live detection", Modifier.weight(1f))
            }
        }
        item { V7Section(v7Text(lang, "Питание и восстановление", "Fuel & recovery", "Odżywianie i regeneracja", "Харчування й відновлення"), "02") }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, null, tint = Color(0xFF70D6FF))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v7Text(lang, "Вода", "Water", "Woda", "Вода"), fontWeight = FontWeight.Black)
                            Text("$water / 8", color = NztMuted)
                        }
                        IconButton(onClick = { water = (water - 1).coerceAtLeast(0); store.setWater(date, water) }) { Icon(Icons.Default.Remove, null) }
                        IconButton(onClick = { water = (water + 1).coerceAtMost(16); store.setWater(date, water) }) { Icon(Icons.Default.Add, null) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString())) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restaurant, null)
                        Spacer(Modifier.width(7.dp))
                        Text(v7Text(lang, "Открыть питание", "Open nutrition", "Otwórz odżywianie", "Відкрити харчування"))
                    }
                }
            }
        }
        item { V7Section(v7Text(lang, "Замеры", "Measurements", "Pomiary", "Вимірювання"), "03") }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonitorWeight, null, tint = NztAccent)
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(v7Text(lang, "Последние данные", "Latest data", "Ostatnie dane", "Останні дані"), fontWeight = FontWeight.Bold)
                        Text(
                            last?.let { "${it.weight ?: "—"} kg • waist ${it.waist ?: "—"} cm" }
                                ?: v7Text(lang, "Нет данных", "No data", "Brak danych", "Немає даних"),
                            color = NztMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V7ToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = NztAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NztMuted, fontSize = 9.sp, maxLines = 2)
        }
    }
}

@Composable
private fun V7Progress(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current
    val store = remember { V7ProductStore(context) }
    val score = repo.completionPercent(repo.tasksForToday())
    val logs = repo.bodyLogs()
    val startWeight = logs.lastOrNull()?.weight
    val currentWeight = logs.firstOrNull()?.weight
    val delta = if (startWeight != null && currentWeight != null) currentWeight - startWeight else null

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { V7Header(null, lang, v7Text(lang, "АНАЛИТИКА", "ANALYTICS", "ANALITYKA", "АНАЛІТИКА")) }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                V7MetricCard("NZT", score.toString(), "/100", Modifier.weight(1f))
                V7MetricCard("WORKOUTS", store.workoutCount().toString(), "total", Modifier.weight(1f))
                V7MetricCard("DAY", repo.dayNumber().toString(), "/365", Modifier.weight(1f))
            }
        }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(v7Text(lang, "ТВОЯ СИСТЕМА", "YOUR SYSTEM", "TWÓJ SYSTEM", "ТВОЯ СИСТЕМА"), color = NztAccent, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    V7ProgressLine(v7Text(lang, "Дневной протокол", "Daily protocol", "Protokół dnia", "Щоденний протокол"), score)
                    V7ProgressLine(v7Text(lang, "Тренировочная дисциплина", "Training consistency", "Regularność treningów", "Регулярність тренувань"), (store.workoutCount() * 10).coerceAtMost(100))
                    V7ProgressLine(v7Text(lang, "Данные тела", "Body data", "Dane ciała", "Дані тіла"), if (logs.isNotEmpty()) 70 else 20)
                }
            }
        }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(v7Text(lang, "Изменение веса", "Weight change", "Zmiana masy", "Зміна ваги"), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        delta?.let { "${if (it > 0) "+" else ""}${"%.1f".format(it)} kg" } ?: "—",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun V7MetricCard(label: String, value: String, subtitle: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = NztAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NztMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun V7ProgressLine(label: String, value: Int) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("$value%", color = NztAccent, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(progress = { value / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = NztAccent, trackColor = NztLine)
        Spacer(Modifier.height(13.dp))
    }
}

@Composable
private fun V7Settings(
    profile: ProfileStore,
    lang: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    onProfileChanged: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name()) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(v7Text(lang, "Настройки", "Settings", "Ustawienia", "Налаштування"), fontSize = 30.sp, fontWeight = FontWeight.Black) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(name, { name = it.take(28) }, modifier = Modifier.fillMaxWidth(), label = { Text(v7Text(lang, "Имя", "Name", "Imię", "Ім'я")) })
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { profile.setName(name); onProfileChanged() }, modifier = Modifier.fillMaxWidth()) {
                        Text(v7Text(lang, "Сохранить", "Save", "Zapisz", "Зберегти"))
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v7Text(lang, "Язык", "Language", "Język", "Мова"), fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    AppLanguage.entries.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onLanguage(item) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = item == lang, onClick = { onLanguage(item) })
                            Text(item.label)
                        }
                    }
                }
            }
        }
        item {
            Surface(color = NztSurface2, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("NZT 365 v7.0", color = NztAccent, fontWeight = FontWeight.Black)
                    Text(
                        v7Text(lang,
                            "Данные хранятся локально на устройстве.",
                            "Data is stored locally on this device.",
                            "Dane są przechowywane lokalnie na urządzeniu.",
                            "Дані зберігаються локально на пристрої."),
                        color = NztMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun v7Text(lang: AppLanguage, ru: String, en: String, pl: String, uk: String): String = when (lang) {
    AppLanguage.RU -> ru
    AppLanguage.EN -> en
    AppLanguage.PL -> pl
    AppLanguage.UK -> uk
}
