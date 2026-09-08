package com.nzt365.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class ProductTab { TODAY, BODY, GROWTH, PROGRESS, SETTINGS }
private enum class GrowthMode { BOOKS, IMPACT }

private data class Readiness(
    val sleep: Int = 3,
    val energy: Int = 3,
    val soreness: Int = 3,
    val stress: Int = 3,
    val saved: Boolean = false
) {
    val score: Int
        get() {
            val sleepPts = sleep * 25
            val energyPts = energy * 25
            val sorenessPts = (6 - soreness) * 25
            val stressPts = (6 - stress) * 25
            return ((sleepPts + energyPts + sorenessPts + stressPts) / 4.0).roundToInt().coerceIn(0, 100)
        }
}

private data class ProductSnapshot(
    val score: Int,
    val readiness: Int?,
    val avg7: Int,
    val workouts7: Int,
    val streak: Int,
    val proteinDays7: Int
)

private class ProductStore(private val context: Context) {
    private val p = context.getSharedPreferences("nzt_product_v5", Context.MODE_PRIVATE)
    private val nzt = context.getSharedPreferences("nzt365", Context.MODE_PRIVATE)
    private val workout = context.getSharedPreferences("nzt_workout_v4", Context.MODE_PRIVATE)
    private val books = context.getSharedPreferences("nzt_books_v3", Context.MODE_PRIVATE)

    fun readiness(date: LocalDate): Readiness {
        val key = date.toString()
        return Readiness(
            sleep = p.getInt("sleep_$key", 3),
            energy = p.getInt("energy_$key", 3),
            soreness = p.getInt("soreness_$key", 3),
            stress = p.getInt("stress_$key", 3),
            saved = p.getBoolean("readiness_$key", false)
        )
    }

    fun saveReadiness(date: LocalDate, r: Readiness) {
        val key = date.toString()
        p.edit()
            .putInt("sleep_$key", r.sleep)
            .putInt("energy_$key", r.energy)
            .putInt("soreness_$key", r.soreness)
            .putInt("stress_$key", r.stress)
            .putBoolean("readiness_$key", true)
            .apply()
    }

    fun addPhoto(date: LocalDate, uri: Uri) {
        val old = photoUris().toMutableList()
        val entry = "${date}|${uri}"
        old.removeAll { it.substringAfter('|', "") == uri.toString() }
        old.add(0, entry)
        p.edit().putString("progress_photos", old.take(24).joinToString("\n")).apply()
    }

    fun photoUris(): List<String> = p.getString("progress_photos", "")
        .orEmpty().lines().filter { it.contains('|') }

    fun completedBooks(): Int = (1..12).count { books.getInt("progress_$it", 0) >= 100 }
    fun currentBookProgress(): Int = (1..12).firstOrNull { books.getInt("progress_$it", 0) < 100 }?.let { books.getInt("progress_$it", 0) } ?: 100

    fun workoutDone(date: LocalDate): Boolean {
        val code = StathamEngine.forDate(date).session.code
        return workout.getBoolean("workout_${date}_$code", false)
    }

    fun workoutsIn(days: Int): Int = (0 until days).count { workoutDone(LocalDate.now().minusDays(it.toLong())) }

    fun proteinHit(date: LocalDate): Boolean {
        val plan = StathamEngine.forDate(date)
        val target = StathamEngine.targets(plan).protein
        return nzt.getInt("protein_$date", 0) >= target
    }

    fun proteinDays(days: Int): Int = (0 until days).count { proteinHit(LocalDate.now().minusDays(it.toLong())) }

    fun score(date: LocalDate): Int = nzt.getInt("score_$date", 0)
    fun averageScore(days: Int): Int = (0 until days).map { score(LocalDate.now().minusDays(it.toLong())) }.average().roundToInt()

    fun lastReview(): String = p.getString("last_weekly_review", "") ?: ""
    fun markReview() = p.edit().putString("last_weekly_review", LocalDate.now().toString()).apply()

    fun snapshot(repo: NZTRepository): ProductSnapshot {
        val todayReadiness = readiness(LocalDate.now())
        return ProductSnapshot(
            score = repo.completionPercent(repo.tasksForToday()),
            readiness = if (todayReadiness.saved) todayReadiness.score else null,
            avg7 = averageScore(7),
            workouts7 = workoutsIn(7),
            streak = repo.scoreStreak(),
            proteinDays7 = proteinDays(7)
        )
    }
}

@Composable
fun NZT6Root(repo: NZTRepository, profileStore: ProfileStore) {
    var onboarded by remember { mutableStateOf(profileStore.isOnboarded()) }
    var lang by remember { mutableStateOf(profileStore.language()) }
    var tick by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalAppLanguage provides lang) {
        NZTProTheme {
            if (!onboarded) {
                ProductOnboarding(lang) { name, selected ->
                    profileStore.save(name, selected)
                    lang = selected
                    onboarded = true
                    tick++
                }
            } else {
                ProductShell(
                    repo = repo,
                    profile = profileStore,
                    lang = lang,
                    profileTick = tick,
                    onLanguage = { profileStore.setLanguage(it); lang = it; tick++ },
                    onProfileChanged = { tick++ }
                )
            }
        }
    }
}

@Composable
private fun ProductOnboarding(initial: AppLanguage, onDone: (String, AppLanguage) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(initial) }
    val l = remember(lang) { Localizer(lang) }
    Scaffold(containerColor = NztBg, contentWindowInsets = WindowInsets.safeDrawing) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Spacer(Modifier.height(18.dp))
                Surface(color = NztAccent, shape = RoundedCornerShape(22.dp)) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { Text("N", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 34.sp) }
                }
                Spacer(Modifier.height(26.dp))
                Text("NZT 365", fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text(l.t("tagline"), color = NztMuted, fontSize = 17.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(name, { if (it.length <= 28) name = it }, label = { Text(l.t("name_hint")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.height(20.dp))
                Text(l.t("choose_language"), color = NztMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AppLanguage.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item -> FilterChip(selected = item == lang, onClick = { lang = item }, label = { Text(item.label) }, modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Button(onClick = { onDone(name.trim(), lang) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) {
                Text(l.t("continue"), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductShell(repo: NZTRepository, profile: ProfileStore, lang: AppLanguage, profileTick: Int, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var tab by remember { mutableStateOf(ProductTab.TODAY) }
    val name = remember(profileTick) { profile.name() }
    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF08121A), tonalElevation = 0.dp) {
                ProductNav(tab, ProductTab.TODAY, Icons.Default.Home, p(lang, "Сегодня", "Today", "Dzisiaj", "Сьогодні")) { tab = it }
                ProductNav(tab, ProductTab.BODY, Icons.Default.FitnessCenter, p(lang, "Тело", "Body", "Ciało", "Тіло")) { tab = it }
                ProductNav(tab, ProductTab.GROWTH, Icons.Default.AutoGraph, p(lang, "Рост", "Growth", "Rozwój", "Ріст")) { tab = it }
                ProductNav(tab, ProductTab.PROGRESS, Icons.Default.BarChart, p(lang, "Прогресс", "Progress", "Postęp", "Прогрес")) { tab = it }
                ProductNav(tab, ProductTab.SETTINGS, Icons.Default.Settings, p(lang, "Настройки", "Settings", "Ustawienia", "Налаштування")) { tab = it }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(NztBg)) {
            when (tab) {
                ProductTab.TODAY -> ProductToday(repo, name, lang, onGrowth = { tab = ProductTab.GROWTH })
                ProductTab.BODY -> ProductBody(repo, lang)
                ProductTab.GROWTH -> ProductGrowth(repo, lang)
                ProductTab.PROGRESS -> ProductProgress(repo, lang, onBody = { tab = ProductTab.BODY }, onGrowth = { tab = ProductTab.GROWTH })
                ProductTab.SETTINGS -> ProductSettings(repo, profile, lang, onLanguage, onProfileChanged)
            }
        }
    }
}

@Composable
private fun RowScope.ProductNav(current: ProductTab, target: ProductTab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (ProductTab) -> Unit) {
    NavigationBarItem(
        selected = current == target,
        onClick = { onClick(target) },
        icon = { Icon(icon, label, modifier = Modifier.size(24.dp)) },
        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, indicatorColor = NztAccent, selectedTextColor = NztText, unselectedIconColor = NztMuted, unselectedTextColor = NztMuted)
    )
}

@Composable
private fun ProductHeader(name: String?, lang: AppLanguage, badge: String? = null) {
    val hour = LocalDateTime.now().hour
    val greeting = when (lang) {
        AppLanguage.RU -> if (hour < 11) "Доброе утро" else if (hour < 18) "Добрый день" else "Добрый вечер"
        AppLanguage.EN -> if (hour < 11) "Good morning" else if (hour < 18) "Good afternoon" else "Good evening"
        AppLanguage.PL -> if (hour < 18) "Dzień dobry" else "Dobry wieczór"
        AppLanguage.UK -> if (hour < 11) "Доброго ранку" else if (hour < 18) "Добрий день" else "Добрий вечір"
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("NZT 365", fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            if (!name.isNullOrBlank()) Text("$greeting, $name", color = NztMuted, fontSize = 14.sp)
        }
        if (!badge.isNullOrBlank()) Surface(color = NztSurface2, shape = RoundedCornerShape(16.dp)) {
            Text(badge, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, code: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(code, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProductToday(repo: NZTRepository, name: String, lang: AppLanguage, onGrowth: () -> Unit) {
    val context = LocalContext.current
    val product = remember { ProductStore(context) }
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(plan) { StathamEngine.targets(plan) }
    var tasks by remember { mutableStateOf(repo.tasksForToday()) }
    var readiness by remember { mutableStateOf(product.readiness(date)) }
    var showCheckIn by remember { mutableStateOf(false) }
    val score = repo.completionPercent(tasks)
    val snapshot = product.snapshot(repo)
    val coach = coachMessage(lang, readiness, plan, snapshot)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ProductHeader(name, lang, "DAY ${repo.dayNumber()} / 365") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(28.dp)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF102532), Color(0xFF0D1821), Color(0xFF142019)))).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p(lang, "ИНДЕКС ДНЯ", "DAILY INDEX", "INDEKS DNIA", "ІНДЕКС ДНЯ"), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(score.toString(), color = NztAccent, fontSize = 44.sp, fontWeight = FontWeight.Black)
                                    Text(" / 100", color = NztMuted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(p(lang, "ГОТОВНОСТЬ", "READINESS", "GOTOWOŚĆ", "ГОТОВНІСТЬ"), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(if (readiness.saved) "${readiness.score}%" else "—", color = if (readiness.saved) NztAccent2 else NztMuted, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = NztAccent, trackColor = NztLine)
                    }
                }
            }
        }
        item {
            CoachCard(coach, readiness.saved, lang) { showCheckIn = true }
        }
        item { SectionTitle(p(lang, "План на сегодня", "Today's plan", "Plan na dziś", "План на сьогодні"), "01") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(14.dp)) {
                    ExercisePhoto(plan.session.title, Modifier.fillMaxWidth().height(190.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(plan.session.title, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(plan.phase.focus, color = NztMuted, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniMetric(p(lang, "Время", "Time", "Czas", "Час"), "${plan.session.minutes} min", Modifier.weight(1f))
                        MiniMetric(p(lang, "Упр.", "Exercises", "Ćwiczenia", "Вправи"), plan.session.exercises.size.toString(), Modifier.weight(1f))
                        MiniMetric(p(lang, "Неделя", "Week", "Tydzień", "Тиждень"), plan.week.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { context.startActivity(Intent(context, V4WorkoutActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(p(lang, "Начать тренировку", "Start workout", "Rozpocznij trening", "Почати тренування"), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(Icons.Default.Restaurant, p(lang, "Питание", "Nutrition", "Odżywianie", "Харчування"), "${targets.calories} kcal • P ${targets.protein} g", Modifier.weight(1f)) {
                    context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString()))
                }
                QuickCard(Icons.Default.MenuBook, p(lang, "Книги", "Books", "Książki", "Книги"), "${product.completedBooks()}/12", Modifier.weight(1f), onGrowth)
            }
        }
        item { SectionTitle(p(lang, "Протокол", "Protocol", "Protokół", "Протокол"), "02") }
        items(tasks, key = { it.id }) { task ->
            val done = task.done.coerceAtMost(task.target)
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(onClick = {
                        val next = if (done >= task.target) 0 else done + 1
                        tasks = tasks.map { if (it.id == task.id) it.copy(done = next) else it }
                        repo.saveTasks(tasks)
                    }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (done >= task.target) NztAccent else NztSurface2, contentColor = if (done >= task.target) Color.Black else NztText)) {
                        Icon(if (done >= task.target) Icons.Default.Check else Icons.Default.Add, null)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(categoryLabel(lang, task.category), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(taskLabel(lang, task), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (task.target > 1) Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
                        Text("$done/${task.target}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = if (done >= task.target) NztAccent else NztMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showCheckIn) ReadinessDialog(lang, readiness, onDismiss = { showCheckIn = false }) {
        readiness = it.copy(saved = true)
        product.saveReadiness(date, readiness)
        showCheckIn = false
    }
}

@Composable
private fun CoachCard(message: String, hasCheckIn: Boolean, lang: AppLanguage, onCheckIn: () -> Unit) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF102432)), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = NztAccent, shape = CircleShape) { Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("NZT COACH", color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(p(lang, "Адаптивная рекомендация", "Adaptive recommendation", "Rekomendacja adaptacyjna", "Адаптивна рекомендація"), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(message, color = NztText, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCheckIn, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (hasCheckIn) p(lang, "Обновить check-in", "Update check-in", "Aktualizuj check-in", "Оновити check-in") else p(lang, "20-секундный check-in", "20-second check-in", "20-sekundowy check-in", "20-секундний check-in"))
            }
        }
    }
}

@Composable
private fun ReadinessDialog(lang: AppLanguage, initial: Readiness, onDismiss: () -> Unit, onSave: (Readiness) -> Unit) {
    var sleep by remember { mutableIntStateOf(initial.sleep) }
    var energy by remember { mutableIntStateOf(initial.energy) }
    var soreness by remember { mutableIntStateOf(initial.soreness) }
    var stress by remember { mutableIntStateOf(initial.stress) }
    val current = Readiness(sleep, energy, soreness, stress, true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p(lang, "Готовность сегодня", "Today's readiness", "Gotowość dziś", "Готовність сьогодні"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${current.score}%", color = NztAccent, fontSize = 34.sp, fontWeight = FontWeight.Black)
                RatingRow(p(lang, "Сон", "Sleep", "Sen", "Сон"), sleep) { sleep = it }
                RatingRow(p(lang, "Энергия", "Energy", "Energia", "Енергія"), energy) { energy = it }
                RatingRow(p(lang, "Крепатура", "Soreness", "Ból mięśni", "Крепатура"), soreness) { soreness = it }
                RatingRow(p(lang, "Стресс", "Stress", "Stres", "Стрес"), stress) { stress = it }
                Text(p(lang, "1 = минимум, 5 = максимум. Для крепатуры и стресса высокий балл снижает готовность.", "1 = minimum, 5 = maximum. High soreness and stress lower readiness.", "1 = minimum, 5 = maksimum. Wysoki ból i stres obniżają gotowość.", "1 = мінімум, 5 = максимум. Висока крепатура і стрес знижують готовність."), color = NztMuted, fontSize = 11.sp)
            }
        },
        confirmButton = { Button(onClick = { onSave(current) }) { Text(p(lang, "Сохранить", "Save", "Zapisz", "Зберегти")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(p(lang, "Отмена", "Cancel", "Anuluj", "Скасувати")) } },
        containerColor = NztSurface
    )
}

@Composable
private fun RatingRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label, color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { n -> FilterChip(selected = value == n, onClick = { onChange(n) }, label = { Text(n.toString()) }, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ProductBody(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current
    val product = remember { ProductStore(context) }
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(plan) { StathamEngine.targets(plan) }
    var showMeasure by remember { mutableStateOf(false) }
    var photoTick by remember { mutableIntStateOf(0) }
    val photos = remember(photoTick) { product.photoUris() }
    val last = repo.bodyLogs().firstOrNull()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            product.addPhoto(date, uri)
            photoTick++
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ProductHeader(null, lang, p(lang, "Тело", "Body", "Ciało", "Тіло")) }
        item { SectionTitle(p(lang, "Тренировка", "Workout", "Trening", "Тренування"), "01") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(14.dp)) {
                    ExercisePhoto(plan.session.title, Modifier.fillMaxWidth().height(190.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(plan.session.title, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(plan.phase.focus, color = NztMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniMetric(p(lang, "Время", "Time", "Czas", "Час"), "${plan.session.minutes} min", Modifier.weight(1f))
                        MiniMetric(p(lang, "Ккал", "Kcal", "Kcal", "Ккал"), targets.calories.toString(), Modifier.weight(1f))
                        MiniMetric(p(lang, "Белок", "Protein", "Białko", "Білок"), "${targets.protein} g", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { context.startActivity(Intent(context, V4WorkoutActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(p(lang, "Начать", "Start", "Start", "Почати"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { context.startActivity(Intent(context, ProTrainingLibraryActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text(p(lang, "Тренировки / библиотека", "Workouts / library", "Treningi / biblioteka", "Тренування / бібліотека"))
                    }
                }
            }
        }
        item { SectionTitle(p(lang, "Питание", "Nutrition", "Odżywianie", "Харчування"), "02") }
        item { ActionCard(Icons.Default.Restaurant, p(lang, "Питание дня", "Daily nutrition", "Odżywianie dnia", "Харчування дня"), "${targets.calories} kcal • P ${targets.protein} g") { context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString())) } }
        item { SectionTitle(p(lang, "Контроль тела", "Body check-in", "Kontrola ciała", "Контроль тіла"), "03") }
        item { ActionCard(Icons.Default.MonitorWeight, p(lang, "Замеры", "Measurements", "Pomiary", "Заміри"), last?.let { "${it.weight ?: "—"} kg • ${it.waist ?: "—"} cm" } ?: p(lang, "Пока нет данных", "No data yet", "Brak danych", "Поки немає даних")) { showMeasure = true } }
        item { ActionCard(Icons.Default.AddAPhoto, p(lang, "Фото прогресса", "Progress photo", "Zdjęcie postępu", "Фото прогресу"), if (photos.isEmpty()) p(lang, "Добавить первое фото", "Add first photo", "Dodaj pierwsze zdjęcie", "Додати перше фото") else p(lang, "${photos.size} фото на устройстве", "${photos.size} photos on device", "${photos.size} zdjęć na urządzeniu", "${photos.size} фото на пристрої")) { picker.launch(arrayOf("image/*")) } }
        if (photos.isNotEmpty()) item { PhotoStrip(photos) }
    }
    if (showMeasure) MeasurementDialog(repo, lang) { showMeasure = false }
}

@Composable
private fun PhotoStrip(entries: List<String>) {
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(entries.take(8)) { entry ->
            val date = entry.substringBefore('|')
            val uri = entry.substringAfter('|')
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.width(130.dp)) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)))
                    Text(date, Modifier.padding(10.dp), color = NztMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MeasurementDialog(repo: NZTRepository, lang: AppLanguage, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf("") }; var waist by remember { mutableStateOf("") }; var chest by remember { mutableStateOf("") }; var arm by remember { mutableStateOf("") }; var thigh by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p(lang, "Замеры тела", "Body measurements", "Pomiary ciała", "Заміри тіла")) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(p(lang,"Вес","Weight","Waga","Вага") to weight, p(lang,"Талия","Waist","Talia","Талія") to waist, p(lang,"Грудь","Chest","Klatka","Груди") to chest, p(lang,"Рука","Arm","Ramię","Рука") to arm, p(lang,"Бедро","Thigh","Udo","Стегно") to thigh).forEachIndexed { i, pair ->
                OutlinedTextField(pair.second, { v -> when(i){0->weight=v;1->waist=v;2->chest=v;3->arm=v;else->thigh=v} }, label={Text(pair.first)}, singleLine=true, modifier=Modifier.fillMaxWidth())
            }
        } },
        confirmButton = { Button(onClick = { repo.saveBody(BodyLog(LocalDate.now().toString(), weight.toDoubleOrNull(), waist.toDoubleOrNull(), chest.toDoubleOrNull(), arm.toDoubleOrNull(), thigh.toDoubleOrNull())); onDismiss() }) { Text(p(lang,"Сохранить","Save","Zapisz","Зберегти")) } },
        dismissButton = { TextButton(onClick=onDismiss){Text(p(lang,"Закрыть","Close","Zamknij","Закрити"))} },
        containerColor = NztSurface
    )
}

@Composable
private fun ProductGrowth(repo: NZTRepository, lang: AppLanguage) {
    var mode by remember { mutableStateOf(GrowthMode.BOOKS) }
    Column(Modifier.fillMaxSize()) {
        ProductHeader(null, lang, p(lang, "Рост", "Growth", "Rozwój", "Ріст"))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            SegmentedButton(selected = mode == GrowthMode.BOOKS, onClick = { mode = GrowthMode.BOOKS }, shape = SegmentedButtonDefaults.itemShape(0,2)) { Text(p(lang,"Книги","Books","Książki","Книги")) }
            SegmentedButton(selected = mode == GrowthMode.IMPACT, onClick = { mode = GrowthMode.IMPACT }, shape = SegmentedButtonDefaults.itemShape(1,2)) { Text("Impact Log") }
        }
        Spacer(Modifier.height(6.dp))
        if (mode == GrowthMode.BOOKS) V5BooksScreen(lang, Modifier.fillMaxSize()) else ProductImpact(repo, lang)
    }
}

@Composable
private fun ProductImpact(repo: NZTRepository, lang: AppLanguage) {
    var type by remember { mutableStateOf("Career") }
    var note by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val logs = remember(refresh) { repo.growthLogs() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Impact Log", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(p(lang,"Фиксируй доказательства роста, а не намерения.","Log evidence of growth, not intentions.","Zapisuj dowody rozwoju, nie intencje.","Фіксуй докази росту, а не наміри."), color=NztMuted, fontSize=12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Career","Mind","Influence","Money").forEach { v -> FilterChip(selected=type==v,onClick={type=v},label={Text(v,fontSize=10.sp)},modifier=Modifier.weight(1f)) }
                    }
                    OutlinedTextField(note,{note=it},label={Text(p(lang,"Что изменилось?","What changed?","Co się zmieniło?","Що змінилося?"))},modifier=Modifier.fillMaxWidth(),minLines=3)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick={if(note.isNotBlank()){repo.addGrowth(type,note.trim());note="";refresh++}},modifier=Modifier.fillMaxWidth()){Text(p(lang,"Сохранить","Save","Zapisz","Зберегти"))}
                }
            }
        }
        items(logs.take(30)) { log -> Card(colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp)){Text(log.type.uppercase(),color=NztAccent,fontSize=9.sp,fontWeight=FontWeight.Bold);Text(log.note,fontWeight=FontWeight.SemiBold);Text(log.date,color=NztMuted,fontSize=10.sp)}} }
    }
}

@Composable
private fun ProductProgress(repo: NZTRepository, lang: AppLanguage, onBody: () -> Unit, onGrowth: () -> Unit) {
    val context = LocalContext.current
    val product = remember { ProductStore(context) }
    var reviewTick by remember { mutableIntStateOf(0) }
    val snapshot = remember(reviewTick) { product.snapshot(repo) }
    val body = repo.bodyLogs()
    val growth = repo.growthLogs()
    val scores = (27 downTo 0).map { off -> LocalDate.now().minusDays(off.toLong()) to repo.scoreForDate(LocalDate.now().minusDays(off.toLong())) }
    val achievements = achievements(repo, product, body.size, growth.size)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ProductHeader(null, lang, p(lang,"Прогресс","Progress","Postęp","Прогрес")) }
        item {
            Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                BigMetric("NZT", "${snapshot.score}/100", Modifier.weight(1f))
                BigMetric(p(lang,"Серия","Streak","Seria","Серія"), "${snapshot.streak} d", Modifier.weight(1f))
            }
        }
        item { SectionTitle(p(lang,"28 дней","28 days","28 dni","28 днів"),"28D") }
        item { HeatmapCard(scores, lang) }
        item { SectionTitle(p(lang,"Неделя","Week","Tydzień","Тиждень"),"07D") }
        item {
            Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    WeeklyStat(p(lang,"Средний NZT","Average NZT","Średni NZT","Середній NZT"), snapshot.avg7, 100)
                    WeeklyStat(p(lang,"Тренировки","Workouts","Treningi","Тренування"), snapshot.workouts7, 7)
                    WeeklyStat(p(lang,"Белок по цели","Protein target","Cel białka","Білок за ціллю"), snapshot.proteinDays7, 7)
                    snapshot.readiness?.let { WeeklyStat(p(lang,"Готовность сегодня","Readiness today","Gotowość dziś","Готовність сьогодні"), it, 100) }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick={product.markReview();reviewTick++},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Icon(Icons.Default.AssignmentTurnedIn,null);Spacer(Modifier.width(6.dp));Text(p(lang,"Закрыть недельный обзор","Complete weekly review","Zamknij przegląd tygodnia","Завершити тижневий огляд"))}
                }
            }
        }
        item { SectionTitle(p(lang,"Достижения","Achievements","Osiągnięcia","Досягнення"),"XP") }
        item {
            LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                items(achievements) { a -> AchievementCard(a.first,a.second) }
            }
        }
        item { ActionCard(Icons.Default.MonitorWeight,p(lang,"Замеры и фото","Measurements & photos","Pomiary i zdjęcia","Заміри та фото"),"${body.size} • ${product.photoUris().size}",onBody) }
        item { ActionCard(Icons.Default.AutoGraph,"Impact Log","${growth.size} ${p(lang,"действий","actions","działań","дій")}",onGrowth) }
    }
}

@Composable
private fun HeatmapCard(scores: List<Pair<LocalDate,Int>>, lang: AppLanguage) {
    Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(p(lang,"Стабильность важнее идеального дня","Consistency beats a perfect day","Regularność wygrywa z idealnym dniem","Стабільність важливіша за ідеальний день"),fontWeight=FontWeight.Bold,fontSize=13.sp)
            Spacer(Modifier.height(12.dp))
            scores.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    week.forEach { (date,score) ->
                        val color = when { score >= 80 -> NztAccent; score >= 50 -> NztAccent2; score > 0 -> Color(0xFF4D5B69); else -> NztLine }
                        Surface(color=color,shape=RoundedCornerShape(7.dp),modifier=Modifier.weight(1f).height(34.dp)) { Box(contentAlignment=Alignment.Center){Text(if(score>0) score.toString() else "",color=if(score>=80)Color.Black else NztText,fontSize=8.sp,fontWeight=FontWeight.Bold)} }
                    }
                    repeat(7-week.size){Spacer(Modifier.weight(1f))}
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun WeeklyStat(label:String,value:Int,max:Int){
    Column {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=NztMuted,fontSize=11.sp);Text("$value/$max",fontWeight=FontWeight.Bold,fontSize=11.sp)}
        Spacer(Modifier.height(5.dp));LinearProgressIndicator(progress={if(max==0)0f else (value.toFloat()/max).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(7.dp),color=NztAccent,trackColor=NztLine)
    }
}

@Composable
private fun AchievementCard(title:String,subtitle:String){
    Card(colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(20.dp),modifier=Modifier.width(170.dp)){
        Column(Modifier.padding(16.dp)){Surface(color=NztSurface2,shape=CircleShape){Box(Modifier.size(42.dp),contentAlignment=Alignment.Center){Icon(Icons.Default.EmojiEvents,null,tint=NztAccent)}};Spacer(Modifier.height(12.dp));Text(title,fontWeight=FontWeight.Black);Text(subtitle,color=NztMuted,fontSize=11.sp)}
    }
}

@Composable
private fun ProductSettings(repo: NZTRepository, profile: ProfileStore, lang: AppLanguage, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var name by remember { mutableStateOf(profile.name()) }
    var targets by remember { mutableStateOf(repo.targets()) }
    var cal by remember { mutableStateOf(targets.first.toString()) }
    var protein by remember { mutableStateOf(targets.second.toString()) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { ProductHeader(null,lang,p(lang,"Настройки","Settings","Ustawienia","Налаштування")) }
        item {
            Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)){
                Column(Modifier.padding(16.dp)){Text(p(lang,"ПРОФИЛЬ","PROFILE","PROFIL","ПРОФІЛЬ"),color=NztMuted,fontSize=10.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));OutlinedTextField(name,{name=it.take(28)},label={Text(p(lang,"Имя","Name","Imię","Ім'я"))},modifier=Modifier.fillMaxWidth(),singleLine=true);Spacer(Modifier.height(8.dp));Button(onClick={profile.setName(name);onProfileChanged()},modifier=Modifier.fillMaxWidth()){Text(p(lang,"Сохранить","Save","Zapisz","Зберегти"))}}
            }
        }
        item { SectionTitle(p(lang,"Язык","Language","Język","Мова"),"04") }
        item {
            Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)){
                Column(Modifier.padding(14.dp)){AppLanguage.entries.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{item->FilterChip(selected=item==lang,onClick={onLanguage(item)},label={Text(item.label)},modifier=Modifier.weight(1f))}};Spacer(Modifier.height(5.dp))}}
            }
        }
        item { SectionTitle(p(lang,"Цели питания","Nutrition targets","Cele żywieniowe","Цілі харчування"),"05") }
        item {
            Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)){
                Column(Modifier.padding(16.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(cal,{cal=it.filter(Char::isDigit).take(4)},label={Text("kcal")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(protein,{protein=it.filter(Char::isDigit).take(3)},label={Text("protein g")},modifier=Modifier.weight(1f),singleLine=true)};Spacer(Modifier.height(8.dp));Button(onClick={val c=cal.toIntOrNull()?:2200;val pr=protein.toIntOrNull()?:140;repo.setTargets(c,pr);targets=c to pr},modifier=Modifier.fillMaxWidth()){Text(p(lang,"Сохранить цели","Save targets","Zapisz cele","Зберегти цілі"))}}
            }
        }
        item {
            Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)){
                Column(Modifier.padding(16.dp)){Text("NZT 365",fontWeight=FontWeight.Black,fontSize=18.sp);Text("v5.0.0",color=NztAccent,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text(p(lang,"Данные профиля, тренировок и прогресса хранятся локально на устройстве.","Profile, workout and progress data are stored locally on this device.","Dane profilu, treningów i postępu są przechowywane lokalnie na urządzeniu.","Дані профілю, тренувань і прогресу зберігаються локально на пристрої."),color=NztMuted,fontSize=12.sp)}
            }
        }
    }
}

@Composable
private fun MiniMetric(label:String,value:String,modifier:Modifier){Surface(color=NztSurface2,shape=RoundedCornerShape(14.dp),modifier=modifier){Column(Modifier.padding(11.dp)){Text(label.uppercase(),color=NztMuted,fontSize=8.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(4.dp));Text(value,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}}}

@Composable
private fun QuickCard(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,modifier:Modifier,onClick:()->Unit){Card(modifier.clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Icon(icon,null,tint=NztAccent);Spacer(Modifier.height(10.dp));Text(title,fontWeight=FontWeight.Bold,maxLines=1);Text(subtitle,color=NztMuted,fontSize=11.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}}}

@Composable
private fun ActionCard(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Card(Modifier.padding(horizontal=20.dp).fillMaxWidth().clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Surface(color=NztSurface2,shape=RoundedCornerShape(15.dp)){Box(Modifier.size(52.dp),contentAlignment=Alignment.Center){Icon(icon,null,tint=NztAccent)}};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold,fontSize=17.sp);Text(subtitle,color=NztMuted,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)};Icon(Icons.Default.ChevronRight,null,tint=NztMuted)}}}

@Composable
private fun BigMetric(label:String,value:String,modifier:Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){Text(label.uppercase(),color=NztMuted,fontSize=10.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text(value,color=NztAccent,fontSize=26.sp,fontWeight=FontWeight.Black)}}}

private fun coachMessage(lang:AppLanguage,r:Readiness,plan:StathamDayPlan,s:ProductSnapshot):String{
    if(!r.saved) return p(lang,"Сделай короткий check-in. После него я скорректирую интенсивность сегодняшней тренировки.","Do a quick check-in. I will use it to adjust today's training intensity.","Zrób krótki check-in. Na jego podstawie dopasuję intensywność dzisiejszego treningu.","Зроби короткий check-in. Після нього я скоригую інтенсивність сьогоднішнього тренування.")
    return when{
        r.score < 45 -> p(lang,"Готовность низкая (${r.score}%). Сегодня приоритет — восстановление: сократи объём на 30–40% или выбери mobility/лёгкое Z2.","Readiness is low (${r.score}%). Prioritize recovery: reduce volume by 30–40% or choose mobility/easy Z2.","Gotowość jest niska (${r.score}%). Priorytetem jest regeneracja: zmniejsz objętość o 30–40% lub wybierz mobility/lekkie Z2.","Готовність низька (${r.score}%). Пріоритет — відновлення: зменш обсяг на 30–40% або обери mobility/легке Z2.")
        r.score < 65 -> p(lang,"Готовность средняя (${r.score}%). Выполни план, но оставляй 3 RIR и не форсируй прогрессию нагрузки.","Readiness is moderate (${r.score}%). Do the plan, keep about 3 RIR and do not force load progression.","Gotowość średnia (${r.score}%). Zrób plan, zostaw około 3 RIR i nie forsuj progresji ciężaru.","Готовність середня (${r.score}%). Виконай план, залишай близько 3 RIR і не форсуй прогресію.")
        r.score >= 80 && s.avg7 >= 60 -> p(lang,"Готовность высокая (${r.score}%). Если техника чистая и RIR ≥2, сегодня хороший день для прогрессии.","Readiness is high (${r.score}%). If technique is clean and RIR ≥2, today is a good day to progress.","Gotowość wysoka (${r.score}%). Jeśli technika jest czysta i RIR ≥2, to dobry dzień na progresję.","Готовність висока (${r.score}%). Якщо техніка чиста і RIR ≥2, сьогодні хороший день для прогресії.")
        else -> p(lang,"Готовность ${r.score}%. Выполняй план без гонки за цифрами: качество подходов важнее объёма любой ценой.","Readiness ${r.score}%. Follow the plan without chasing numbers: set quality matters more than volume at any cost.","Gotowość ${r.score}%. Realizuj plan bez gonienia za liczbami: jakość serii jest ważniejsza niż objętość za wszelką cenę.","Готовність ${r.score}%. Виконуй план без гонитви за цифрами: якість підходів важливіша за обсяг будь-якою ціною.")
    }
}

private fun achievements(repo:NZTRepository,product:ProductStore,bodyCount:Int,growthCount:Int):List<Pair<String,String>>{
    val out=mutableListOf<Pair<String,String>>()
    if(repo.scoreStreak()>=3) out += "3-day streak" to "Consistency started"
    if(repo.scoreStreak()>=7) out += "7-day streak" to "One full week"
    if(product.workoutsIn(30)>=8) out += "8 workouts" to "Training momentum"
    if(product.completedBooks()>=1) out += "First book" to "Applied learning"
    if(bodyCount>=2) out += "Body trend" to "Two check-ins logged"
    if(growthCount>=5) out += "Impact x5" to "Evidence of growth"
    if(out.isEmpty()) out += "First step" to "Build the first 3-day streak"
    return out
}

private fun categoryLabel(lang:AppLanguage,c:String):String=when(c.uppercase()){
    "BODY"->p(lang,"ТЕЛО","BODY","CIAŁO","ТІЛО");"NUTRITION"->p(lang,"ПИТАНИЕ","NUTRITION","ODŻYWIANIE","ХАРЧУВАННЯ");"INFLUENCE"->p(lang,"ВЛИЯНИЕ","INFLUENCE","WPŁYW","ВПЛИВ");"MIND"->p(lang,"МЫШЛЕНИЕ","MIND","UMYSŁ","МИСЛЕННЯ");"CAREER"->p(lang,"КАРЬЕРА","CAREER","KARIERA","КАР'ЄРА");"MONEY"->p(lang,"ДЕНЬГИ","MONEY","FINANSE","ГРОШІ");else->c}

private fun taskLabel(lang:AppLanguage,t:DailyTask):String=when(t.id){
    "protein"->p(lang,"Белок по цели","Protein target","Cel białka","Білок за ціллю");"mirror"->"Mirror";"label"->"Label";"mind"->p(lang,"Чтение / обучение 20 мин","Read / learn 20 min","Czytanie / nauka 20 min","Читання / навчання 20 хв");"career"->p(lang,"15 минут карьерного действия","15 min career action","15 min działania kariery","15 хв кар'єрної дії");"money"->p(lang,"1 действие для роста дохода","1 income growth action","1 działanie wzrostu dochodu","1 дія для росту доходу");else->t.title}

private fun p(lang:AppLanguage,ru:String,en:String,pl:String,uk:String):String=when(lang){AppLanguage.RU->ru;AppLanguage.EN->en;AppLanguage.PL->pl;AppLanguage.UK->uk}
