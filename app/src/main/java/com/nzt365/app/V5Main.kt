package com.nzt365.app

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime

private enum class V5Tab { TODAY, BODY, GROWTH, PROGRESS, SETTINGS }
private enum class V5GrowthMode { BOOKS, IMPACT }

@Composable
fun NZT5Root(repo: NZTRepository, profileStore: ProfileStore) {
    var onboarded by remember { mutableStateOf(profileStore.isOnboarded()) }
    var lang by remember { mutableStateOf(profileStore.language()) }
    var profileTick by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalAppLanguage provides lang) {
        NZTProTheme {
            if (!onboarded) {
                V5Onboarding(lang) { name, language ->
                    profileStore.save(name, language)
                    lang = language
                    onboarded = true
                    profileTick++
                }
            } else {
                V5Shell(repo, profileStore, lang, profileTick,
                    onLanguage = { profileStore.setLanguage(it); lang = it; profileTick++ },
                    onProfileChanged = { profileTick++ })
            }
        }
    }
}

@Composable
private fun V5Onboarding(initial: AppLanguage, onDone: (String, AppLanguage) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(initial) }
    Scaffold(containerColor = NztBg, contentWindowInsets = WindowInsets.safeDrawing) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Spacer(Modifier.height(18.dp))
                Surface(color = NztAccent, shape = RoundedCornerShape(22.dp)) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        Text("N", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 34.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("NZT 365", fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text(v5t(lang, "tagline"), color = NztMuted, fontSize = 17.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(30.dp))
                OutlinedTextField(name, { if (it.length <= 28) name = it }, label = { Text(v5t(lang, "name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.height(22.dp))
                Text(v5t(lang, "language"), color = NztMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AppLanguage.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { l -> FilterChip(selected = l == lang, onClick = { lang = l }, label = { Text(l.label) }, modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Button(onClick = { onDone(name.trim(), lang) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(58.dp).navigationBarsPadding(), shape = RoundedCornerShape(18.dp)) {
                Text(v5t(lang, "start"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun V5Shell(repo: NZTRepository, profile: ProfileStore, lang: AppLanguage, profileTick: Int, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var tab by remember { mutableStateOf(V5Tab.TODAY) }
    val name = remember(profileTick) { profile.name() }

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF08121A), tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
                V5Nav(tab, V5Tab.TODAY, Icons.Default.Home, v5t(lang, "today")) { tab = it }
                V5Nav(tab, V5Tab.BODY, Icons.Default.FitnessCenter, v5t(lang, "body")) { tab = it }
                V5Nav(tab, V5Tab.GROWTH, Icons.Default.AutoGraph, v5t(lang, "growth")) { tab = it }
                V5Nav(tab, V5Tab.PROGRESS, Icons.Default.BarChart, v5t(lang, "progress")) { tab = it }
                V5Nav(tab, V5Tab.SETTINGS, Icons.Default.Settings, v5t(lang, "settings")) { tab = it }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(NztBg)) {
            when (tab) {
                V5Tab.TODAY -> V5Today(repo, name, lang, onBooks = { tab = V5Tab.GROWTH })
                V5Tab.BODY -> V5Body(repo, lang)
                V5Tab.GROWTH -> V5Growth(repo, lang)
                V5Tab.PROGRESS -> V5Progress(repo, lang, onBody = { tab = V5Tab.BODY }, onGrowth = { tab = V5Tab.GROWTH })
                V5Tab.SETTINGS -> V5Settings(profile, lang, onLanguage, onProfileChanged)
            }
        }
    }
}

@Composable
private fun RowScope.V5Nav(current: V5Tab, target: V5Tab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (V5Tab) -> Unit) {
    NavigationBarItem(
        selected = current == target,
        onClick = { onClick(target) },
        icon = { Icon(icon, label, modifier = Modifier.size(24.dp)) },
        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, indicatorColor = NztAccent, selectedTextColor = NztText, unselectedIconColor = NztMuted, unselectedTextColor = NztMuted)
    )
}

@Composable
private fun V5Header(name: String?, lang: AppLanguage, badge: String? = null) {
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
private fun V5Section(title: String, code: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(code, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V5Today(repo: NZTRepository, name: String, lang: AppLanguage, onBooks: () -> Unit) {
    val context = LocalContext.current
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(plan) { StathamEngine.targets(plan) }
    var tasks by remember { mutableStateOf(repo.tasksForToday()) }
    val score = repo.completionPercent(tasks)
    val doneUnits = tasks.sumOf { it.done.coerceAtMost(it.target) }
    val totalUnits = tasks.sumOf { it.target }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { V5Header(name, lang, "DAY ${repo.dayNumber()} / 365") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(28.dp)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF102532), Color(0xFF0D1821), Color(0xFF142019)))).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(v5t(lang, "daily_index").uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(score.toString(), color = NztAccent, fontSize = 44.sp, fontWeight = FontWeight.Black)
                                    Text(" / 100", color = NztMuted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                                }
                            }
                            Surface(color = Color(0x221FFFFFF), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.End) {
                                    Text(v5t(lang, "done"), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("$doneUnits/$totalUnits", color = NztText, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = NztAccent, trackColor = NztLine)
                    }
                }
            }
        }
        item { V5Section(v5t(lang, "today_plan"), "01") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(14.dp)) {
                    ExercisePhoto(plan.session.title, Modifier.fillMaxWidth().height(200.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(plan.phase.name, color = NztMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V5MiniMetric(v5t(lang, "duration"), "${plan.session.minutes} min", Modifier.weight(1f))
                        V5MiniMetric(v5t(lang, "exercises"), plan.session.exercises.size.toString(), Modifier.weight(1f))
                        V5MiniMetric(v5t(lang, "week"), plan.week.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { context.startActivity(Intent(context, V4WorkoutActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(v5t(lang, "start_workout"), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V5Quick(Icons.Default.Restaurant, v5t(lang, "nutrition"), "${targets.calories} kcal • P ${targets.protein} g", Modifier.weight(1f)) {
                    context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString()))
                }
                V5Quick(Icons.Default.MenuBook, v5t(lang, "books"), v5t(lang, "twelve_months"), Modifier.weight(1f), onBooks)
            }
        }
        item { V5Section(v5t(lang, "habits"), "02") }
        items(tasks, key = { it.id }) { task ->
            val done = task.done.coerceAtMost(task.target)
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = {
                            val next = if (done >= task.target) 0 else done + 1
                            tasks = tasks.map { if (it.id == task.id) it.copy(done = next) else it }
                            repo.saveTasks(tasks)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (done >= task.target) NztAccent else NztSurface2, contentColor = if (done >= task.target) Color.Black else NztText)
                    ) { Icon(if (done >= task.target) Icons.Default.Check else Icons.Default.Add, null) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v5Category(lang, task.category), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(v5TaskTitle(lang, task.id, task.title), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (task.target > 1) Surface(color = NztSurface2, shape = RoundedCornerShape(12.dp)) {
                        Text("$done/${task.target}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = if (done >= task.target) NztAccent else NztMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V5MiniMetric(label: String, value: String, modifier: Modifier) {
    Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(11.dp)) {
            Text(label.uppercase(), color = NztMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun V5Quick(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = NztAccent)
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = NztMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun V5Body(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(plan) { StathamEngine.targets(plan) }
    val last = repo.bodyLogs().firstOrNull()
    var showMeasure by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { V5Header(null, lang, v5t(lang, "body")) }
        item { V5Section(v5t(lang, "workout"), "01") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(14.dp)) {
                    ExercisePhoto(plan.session.title, Modifier.fillMaxWidth().height(200.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(plan.phase.focus, color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V5MiniMetric(v5t(lang, "duration"), "${plan.session.minutes} min", Modifier.weight(1f))
                        V5MiniMetric(v5t(lang, "calories"), targets.calories.toString(), Modifier.weight(1f))
                        V5MiniMetric(v5t(lang, "protein"), "${targets.protein} g", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { context.startActivity(Intent(context, V4WorkoutActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(v5t(lang, "start_workout"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { context.startActivity(Intent(context, ProTrainingLibraryActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text(v5t(lang, "training_library"))
                    }
                }
            }
        }
        item { V5Section(v5t(lang, "nutrition"), "02") }
        item { V5ActionCard(Icons.Default.Restaurant, v5t(lang, "daily_nutrition"), v5t(lang, "macros_water")) { context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString())) } }
        item { V5Section(v5t(lang, "measurements"), "03") }
        item { V5ActionCard(Icons.Default.MonitorWeight, v5t(lang, "measurements"), last?.let { "${it.weight ?: "—"} kg • ${v5t(lang, "waist")} ${it.waist ?: "—"} cm" } ?: v5t(lang, "no_data")) { showMeasure = true } }
    }
    if (showMeasure) V5MeasurementDialog(repo, lang) { showMeasure = false }
}

@Composable
private fun V5ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = NztSurface2, shape = RoundedCornerShape(15.dp)) { Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = NztAccent) } }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(subtitle, color = NztMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null, tint = NztMuted)
        }
    }
}

@Composable
private fun V5MeasurementDialog(repo: NZTRepository, lang: AppLanguage, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf("") }; var waist by remember { mutableStateOf("") }; var chest by remember { mutableStateOf("") }; var arm by remember { mutableStateOf("") }; var thigh by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(v5t(lang, "measurements")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(v5t(lang, "weight") to weight, v5t(lang, "waist") to waist, v5t(lang, "chest") to chest, v5t(lang, "arm") to arm, v5t(lang, "thigh") to thigh).forEachIndexed { i, pair ->
                    OutlinedTextField(pair.second, { v -> when (i) { 0 -> weight = v; 1 -> waist = v; 2 -> chest = v; 3 -> arm = v; else -> thigh = v } }, label = { Text(pair.first) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = { Button(onClick = { repo.saveBody(BodyLog(LocalDate.now().toString(), weight.toDoubleOrNull(), waist.toDoubleOrNull(), chest.toDoubleOrNull(), arm.toDoubleOrNull(), thigh.toDoubleOrNull())); onDismiss() }) { Text(v5t(lang, "save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(v5t(lang, "close")) } },
        containerColor = NztSurface
    )
}

@Composable
private fun V5Growth(repo: NZTRepository, lang: AppLanguage) {
    var mode by remember { mutableStateOf(V5GrowthMode.BOOKS) }
    Column(Modifier.fillMaxSize()) {
        V5Header(null, lang, v5t(lang, "growth"))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            SegmentedButton(selected = mode == V5GrowthMode.BOOKS, onClick = { mode = V5GrowthMode.BOOKS }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(v5t(lang, "books")) }
            SegmentedButton(selected = mode == V5GrowthMode.IMPACT, onClick = { mode = V5GrowthMode.IMPACT }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(v5t(lang, "impact")) }
        }
        Spacer(Modifier.height(6.dp))
        if (mode == V5GrowthMode.BOOKS) V5BooksScreen(lang, Modifier.fillMaxSize()) else V5Impact(repo, lang)
    }
}

@Composable
private fun V5Impact(repo: NZTRepository, lang: AppLanguage) {
    var type by remember { mutableStateOf("Career") }
    var note by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val logs = remember(refresh) { repo.growthLogs() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v5t(lang, "impact"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Career", "Mind", "Influence", "Money").forEach { value -> FilterChip(selected = type == value, onClick = { type = value }, label = { Text(v5ImpactLabel(lang, value), fontSize = 11.sp) }) }
                    }
                    OutlinedTextField(note, { note = it }, label = { Text(v5t(lang, "note")) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { if (note.isNotBlank()) { repo.addGrowth(type, note.trim()); note = ""; refresh++ } }, modifier = Modifier.fillMaxWidth()) { Text(v5t(lang, "save")) }
                }
            }
        }
        items(logs.take(20)) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) { Text(v5ImpactLabel(lang, log.type), color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(log.note, fontWeight = FontWeight.SemiBold); Text(log.date, color = NztMuted, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun V5Progress(repo: NZTRepository, lang: AppLanguage, onBody: () -> Unit, onGrowth: () -> Unit) {
    val tasks = repo.tasksForToday()
    val score = repo.completionPercent(tasks)
    val body = repo.bodyLogs()
    val growth = repo.growthLogs()
    val money = repo.moneyLogs()
    val trend = repo.lastDailyScores(7)
    val streak = repo.scoreStreak()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { V5Header(null, lang, v5t(lang, "progress")) }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V5Metric("NZT", "$score / 100", Modifier.weight(1f))
                V5Metric(v5t(lang, "streak"), "$streak ${v5t(lang, "days_short")}", Modifier.weight(1f))
            }
        }
        item { V5Section(v5t(lang, "seven_day_trend"), "07D") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    trend.forEach { (date, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(date.dayOfWeek.name.take(3), color = NztMuted, fontSize = 10.sp, modifier = Modifier.width(34.dp))
                            LinearProgressIndicator(progress = { value / 100f }, modifier = Modifier.weight(1f).height(8.dp), color = if (date == LocalDate.now()) NztAccent else NztAccent2, trackColor = NztLine)
                            Text(" $value", fontSize = 10.sp, color = NztMuted, modifier = Modifier.width(28.dp))
                        }
                    }
                }
            }
        }
        item { V5ActionCard(Icons.Default.MonitorWeight, v5t(lang, "measurements"), "${body.size} ${v5t(lang, "checkins")}", onBody) }
        item { V5ActionCard(Icons.Default.AutoGraph, v5t(lang, "impact"), "${growth.size} ${v5t(lang, "actions")}", onGrowth) }
        item { V5ActionCard(Icons.Default.AccountBalanceWallet, v5t(lang, "money"), "${money.size} ${v5t(lang, "entries")}") {} }
    }
}

@Composable
private fun V5Metric(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(label.uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(value, color = NztAccent, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V5Settings(profile: ProfileStore, lang: AppLanguage, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var editName by remember { mutableStateOf(profile.name()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { V5Header(null, lang, v5t(lang, "settings")) }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v5t(lang, "profile").uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(editName, { if (it.length <= 28) editName = it }, label = { Text(v5t(lang, "name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { profile.setName(editName.trim()); onProfileChanged() }, enabled = editName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(v5t(lang, "save")) }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v5t(lang, "language").uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AppLanguage.entries.forEach { l ->
                        ListItem(headlineContent = { Text(l.label) }, leadingContent = { RadioButton(selected = l == lang, onClick = { onLanguage(l) }) }, modifier = Modifier.clickable { onLanguage(l) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                    }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("NZT 365", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("v4.2.0 • local-first • signed release", color = NztMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun v5ImpactLabel(lang: AppLanguage, value: String): String = when (value) {
    "Career" -> v5t(lang, "career")
    "Mind" -> v5t(lang, "mind")
    "Influence" -> v5t(lang, "influence")
    "Money" -> v5t(lang, "money")
    else -> value
}

private fun v5Category(lang: AppLanguage, value: String): String = when (value.uppercase()) {
    "BODY" -> v5t(lang, "body")
    "NUTRITION" -> v5t(lang, "nutrition")
    "INFLUENCE" -> v5t(lang, "influence")
    "MIND" -> v5t(lang, "mind")
    "CAREER" -> v5t(lang, "career")
    "MONEY" -> v5t(lang, "money")
    else -> value
}

private fun v5TaskTitle(lang: AppLanguage, id: String, fallback: String): String = when (id) {
    "body" -> v5t(lang, "task_body")
    "protein" -> v5t(lang, "task_protein")
    "mirror" -> "Mirror"
    "label" -> "Label"
    "mind" -> v5t(lang, "task_mind")
    "career" -> v5t(lang, "task_career")
    "money" -> v5t(lang, "task_money")
    else -> fallback
}

private fun v5t(lang: AppLanguage, key: String): String {
    val ru = mapOf(
        "tagline" to "Твоя система тела, дисциплины, роста и капитала", "name" to "Имя", "language" to "Язык", "start" to "Начать",
        "today" to "Сегодня", "body" to "Тело", "growth" to "Рост", "progress" to "Прогресс", "settings" to "Настройки",
        "daily_index" to "Индекс дня", "done" to "Выполнено", "today_plan" to "План на сегодня", "duration" to "Время", "exercises" to "Упражнения", "week" to "Неделя",
        "start_workout" to "Начать тренировку", "nutrition" to "Питание", "books" to "Книги", "twelve_months" to "12 книг • 12 месяцев", "habits" to "Привычки",
        "workout" to "Тренировка", "training_library" to "Тренировки / библиотека", "daily_nutrition" to "Питание дня", "macros_water" to "Калории • белки • жиры • углеводы • вода",
        "measurements" to "Замеры", "waist" to "талия", "no_data" to "Пока нет данных", "weight" to "Вес, кг", "chest" to "Грудь, см", "arm" to "Рука, см", "thigh" to "Бедро, см", "save" to "Сохранить", "close" to "Закрыть",
        "impact" to "Impact Log", "note" to "Что сделал / что понял", "career" to "Карьера", "mind" to "Мышление", "influence" to "Влияние", "money" to "Деньги",
        "streak" to "Серия", "days_short" to "дн.", "seven_day_trend" to "Тренд 7 дней", "checkins" to "замеров", "actions" to "действий", "entries" to "записей",
        "profile" to "Профиль", "calories" to "Ккал", "protein" to "Белок", "task_body" to "Тренировка дня", "task_protein" to "Белок по цели", "task_mind" to "Чтение / обучение 20 мин", "task_career" to "15 минут карьерного действия", "task_money" to "1 действие для роста дохода"
    )
    val en = mapOf(
        "tagline" to "Your system for body, discipline, growth and capital", "name" to "Name", "language" to "Language", "start" to "Start",
        "today" to "Today", "body" to "Body", "growth" to "Growth", "progress" to "Progress", "settings" to "Settings",
        "daily_index" to "Daily index", "done" to "Done", "today_plan" to "Today plan", "duration" to "Duration", "exercises" to "Exercises", "week" to "Week",
        "start_workout" to "Start workout", "nutrition" to "Nutrition", "books" to "Books", "twelve_months" to "12 books • 12 months", "habits" to "Habits",
        "workout" to "Workout", "training_library" to "Workouts / library", "daily_nutrition" to "Daily nutrition", "macros_water" to "Calories • protein • fats • carbs • water",
        "measurements" to "Measurements", "waist" to "waist", "no_data" to "No data yet", "weight" to "Weight, kg", "chest" to "Chest, cm", "arm" to "Arm, cm", "thigh" to "Thigh, cm", "save" to "Save", "close" to "Close",
        "impact" to "Impact Log", "note" to "What I did / learned", "career" to "Career", "mind" to "Mind", "influence" to "Influence", "money" to "Money",
        "streak" to "Streak", "days_short" to "days", "seven_day_trend" to "7-day trend", "checkins" to "check-ins", "actions" to "actions", "entries" to "entries",
        "profile" to "Profile", "calories" to "Calories", "protein" to "Protein", "task_body" to "Today workout", "task_protein" to "Hit protein target", "task_mind" to "Read / learn 20 min", "task_career" to "15 min career action", "task_money" to "1 income-growth action"
    )
    val pl = mapOf(
        "tagline" to "Twój system ciała, dyscypliny, rozwoju i kapitału", "name" to "Imię", "language" to "Język", "start" to "Start",
        "today" to "Dziś", "body" to "Ciało", "growth" to "Rozwój", "progress" to "Postęp", "settings" to "Ustawienia",
        "daily_index" to "Indeks dnia", "done" to "Wykonano", "today_plan" to "Plan na dziś", "duration" to "Czas", "exercises" to "Ćwiczenia", "week" to "Tydzień",
        "start_workout" to "Rozpocznij trening", "nutrition" to "Odżywianie", "books" to "Książki", "twelve_months" to "12 książek • 12 miesięcy", "habits" to "Nawyki",
        "workout" to "Trening", "training_library" to "Treningi / biblioteka", "daily_nutrition" to "Żywienie dnia", "macros_water" to "Kalorie • białko • tłuszcze • węgle • woda",
        "measurements" to "Pomiary", "waist" to "talia", "no_data" to "Brak danych", "weight" to "Waga, kg", "chest" to "Klatka, cm", "arm" to "Ramię, cm", "thigh" to "Udo, cm", "save" to "Zapisz", "close" to "Zamknij",
        "impact" to "Impact Log", "note" to "Co zrobiłem / czego się nauczyłem", "career" to "Kariera", "mind" to "Umysł", "influence" to "Wpływ", "money" to "Pieniądze",
        "streak" to "Seria", "days_short" to "dni", "seven_day_trend" to "Trend 7 dni", "checkins" to "pomiarów", "actions" to "działań", "entries" to "wpisów",
        "profile" to "Profil", "calories" to "Kalorie", "protein" to "Białko", "task_body" to "Dzisiejszy trening", "task_protein" to "Cel białka", "task_mind" to "Czytanie / nauka 20 min", "task_career" to "15 min działania zawodowego", "task_money" to "1 działanie dla wzrostu dochodu"
    )
    val uk = mapOf(
        "tagline" to "Твоя система тіла, дисципліни, розвитку та капіталу", "name" to "Ім'я", "language" to "Мова", "start" to "Почати",
        "today" to "Сьогодні", "body" to "Тіло", "growth" to "Розвиток", "progress" to "Прогрес", "settings" to "Налаштування",
        "daily_index" to "Індекс дня", "done" to "Виконано", "today_plan" to "План на сьогодні", "duration" to "Час", "exercises" to "Вправи", "week" to "Тиждень",
        "start_workout" to "Почати тренування", "nutrition" to "Харчування", "books" to "Книги", "twelve_months" to "12 книг • 12 місяців", "habits" to "Звички",
        "workout" to "Тренування", "training_library" to "Тренування / бібліотека", "daily_nutrition" to "Харчування дня", "macros_water" to "Калорії • білки • жири • вуглеводи • вода",
        "measurements" to "Заміри", "waist" to "талія", "no_data" to "Поки немає даних", "weight" to "Вага, кг", "chest" to "Груди, см", "arm" to "Рука, см", "thigh" to "Стегно, см", "save" to "Зберегти", "close" to "Закрити",
        "impact" to "Impact Log", "note" to "Що зробив / що зрозумів", "career" to "Кар'єра", "mind" to "Мислення", "influence" to "Вплив", "money" to "Гроші",
        "streak" to "Серія", "days_short" to "дн.", "seven_day_trend" to "Тренд 7 днів", "checkins" to "замірів", "actions" to "дій", "entries" to "записів",
        "profile" to "Профіль", "calories" to "Ккал", "protein" to "Білок", "task_body" to "Тренування дня", "task_protein" to "Білок за ціллю", "task_mind" to "Читання / навчання 20 хв", "task_career" to "15 хв кар'єрної дії", "task_money" to "1 дія для росту доходу"
    )
    val map = when (lang) { AppLanguage.RU -> ru; AppLanguage.EN -> en; AppLanguage.PL -> pl; AppLanguage.UK -> uk }
    return map[key] ?: key
}
