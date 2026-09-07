package com.nzt365.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

private enum class V4Tab { TODAY, BODY, GROWTH, PROGRESS, SETTINGS }
private enum class V4GrowthMode { BOOKS, IMPACT }

@Composable
fun NZT4Root(repo: NZTRepository, profileStore: ProfileStore) {
    var onboarded by remember { mutableStateOf(profileStore.isOnboarded()) }
    var lang by remember { mutableStateOf(profileStore.language()) }
    var profileTick by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalAppLanguage provides lang) {
        NZTProTheme {
            if (!onboarded) {
                V4Onboarding(lang) { name, language ->
                    profileStore.save(name, language)
                    lang = language
                    onboarded = true
                    profileTick++
                }
            } else {
                V4Shell(
                    repo = repo,
                    profile = profileStore,
                    lang = lang,
                    profileTick = profileTick,
                    onLanguage = { profileStore.setLanguage(it); lang = it; profileTick++ },
                    onProfileChanged = { profileTick++ }
                )
            }
        }
    }
}

@Composable
private fun V4Onboarding(initial: AppLanguage, onDone: (String, AppLanguage) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(initial) }
    Scaffold(containerColor = NztBg, contentWindowInsets = WindowInsets.safeDrawing) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.height(18.dp))
                Surface(color = NztAccent, shape = RoundedCornerShape(22.dp)) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        Text("N", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 34.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("NZT 365", fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text(
                    when (lang) {
                        AppLanguage.RU -> "Твоя система тела, дисциплины, роста и капитала"
                        AppLanguage.EN -> "Your system for body, discipline, growth and capital"
                        AppLanguage.PL -> "Twój system ciała, dyscypliny, rozwoju i kapitału"
                        AppLanguage.UK -> "Твоя система тіла, дисципліни, розвитку та капіталу"
                    },
                    color = NztMuted, fontSize = 17.sp, lineHeight = 25.sp
                )
                Spacer(Modifier.height(30.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 28) name = it },
                    label = { Text(V3Strings.t(lang, "name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(22.dp))
                Text(V3Strings.t(lang, "language"), color = NztMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AppLanguage.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { l ->
                            FilterChip(
                                selected = l == lang,
                                onClick = { lang = l },
                                label = { Text(l.label) },
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
                modifier = Modifier.fillMaxWidth().height(58.dp).navigationBarsPadding(),
                shape = RoundedCornerShape(18.dp)
            ) { Text(V3Strings.t(lang, "start"), fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        }
    }
}

@Composable
private fun V4Shell(
    repo: NZTRepository,
    profile: ProfileStore,
    lang: AppLanguage,
    profileTick: Int,
    onLanguage: (AppLanguage) -> Unit,
    onProfileChanged: () -> Unit
) {
    var tab by remember { mutableStateOf(V4Tab.TODAY) }
    val name = remember(profileTick) { profile.name() }

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF09131C),
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                V4Nav(tab, V4Tab.TODAY, Icons.Default.Home, V3Strings.t(lang, "today")) { tab = it }
                V4Nav(tab, V4Tab.BODY, Icons.Default.FitnessCenter, V3Strings.t(lang, "body")) { tab = it }
                V4Nav(tab, V4Tab.GROWTH, Icons.Default.AutoGraph, V3Strings.t(lang, "growth")) { tab = it }
                V4Nav(tab, V4Tab.PROGRESS, Icons.Default.BarChart, V3Strings.t(lang, "progress")) { tab = it }
                V4Nav(tab, V4Tab.SETTINGS, Icons.Default.Settings, V3Strings.t(lang, "settings")) { tab = it }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(NztBg)) {
            when (tab) {
                V4Tab.TODAY -> V4Today(repo, name, lang)
                V4Tab.BODY -> V4Body(repo, lang)
                V4Tab.GROWTH -> V4Growth(repo, lang)
                V4Tab.PROGRESS -> V4Progress(repo, lang)
                V4Tab.SETTINGS -> V4Settings(profile, lang, onLanguage, onProfileChanged)
            }
        }
    }
}

@Composable
private fun RowScope.V4Nav(current: V4Tab, target: V4Tab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (V4Tab) -> Unit) {
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
private fun V4Header(name: String?, lang: AppLanguage, badge: String? = null) {
    val hour = LocalDateTime.now().hour
    val greeting = when (lang) {
        AppLanguage.RU -> if (hour < 11) "Доброе утро" else if (hour < 18) "Добрый день" else "Добрый вечер"
        AppLanguage.EN -> if (hour < 11) "Good morning" else if (hour < 18) "Good afternoon" else "Good evening"
        AppLanguage.PL -> if (hour < 18) "Dzień dobry" else "Dobry wieczór"
        AppLanguage.UK -> if (hour < 11) "Доброго ранку" else if (hour < 18) "Добрий день" else "Добрий вечір"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("NZT 365", fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            if (!name.isNullOrBlank()) Text("$greeting, $name", color = NztMuted, fontSize = 14.sp)
        }
        if (!badge.isNullOrBlank()) {
            Surface(color = NztSurface2, shape = RoundedCornerShape(16.dp)) {
                Text(badge, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun V4Section(title: String, code: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(code, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V4Today(repo: NZTRepository, name: String, lang: AppLanguage) {
    val context = LocalContext.current
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(plan) { StathamEngine.targets(plan) }
    var tasks by remember { mutableStateOf(repo.tasksForToday()) }
    val score = repo.completionPercent(tasks)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { V4Header(name, lang, "DAY ${repo.dayNumber()} / 365") }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(Color(0xFF10222D), Color(0xFF0D1821), Color(0xFF142017)))
                    ).padding(22.dp)
                ) {
                    Column {
                        Text("NZT SCORE", color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(score.toString(), color = NztAccent, fontSize = 46.sp, fontWeight = FontWeight.Black)
                            Text(" / 100", color = NztMuted, fontSize = 17.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = NztAccent, trackColor = NztLine)
                    }
                }
            }
        }
        item { V4Section(V3Strings.t(lang, "todayPlan"), "01") }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(220.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(plan.session.title, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("${plan.session.minutes} min • ${plan.session.exercises.size} exercises • ${plan.phase.name}", color = NztMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) { Text("KCAL", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(targets.calories.toString(), fontWeight = FontWeight.Bold) }
                        }
                        Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) { Text("PROTEIN", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("${targets.protein} g", fontWeight = FontWeight.Bold) }
                        }
                        Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) { Text("WEEK", color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(plan.week.toString(), fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { context.startActivity(Intent(context, V4WorkoutActivity::class.java).putExtra("date", date.toString())) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(V3Strings.t(lang, "start"), fontWeight = FontWeight.Black, fontSize = 16.sp) }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V4Quick(Icons.Default.Restaurant, V3Strings.t(lang, "nutrition"), "${targets.calories} kcal", Modifier.weight(1f)) {
                    context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString()))
                }
                V4Quick(Icons.Default.MenuBook, V3Strings.t(lang, "books"), "12 / 12 months", Modifier.weight(1f)) { }
            }
        }
        item { V4Section(V3Strings.t(lang, "habits"), "02") }
        items(tasks, key = { it.id }) { task ->
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = {
                            tasks = tasks.map { if (it.id == task.id) it.copy(done = if (it.done >= it.target) 0 else it.target) else it }
                            repo.saveTasks(tasks)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (task.done >= task.target) NztAccent else NztSurface2,
                            contentColor = if (task.done >= task.target) Color.Black else NztText
                        )
                    ) { Icon(if (task.done >= task.target) Icons.Default.Check else Icons.Default.Add, null) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.category, color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (task.target > 1) Text("${task.done}/${task.target}", color = NztAccent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V4Quick(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
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
private fun V4Body(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val last = repo.bodyLogs().firstOrNull()
    var showMeasure by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { V4Header(null, lang, V3Strings.t(lang, "body")) }
        item { V4Section(V3Strings.t(lang, "workout"), "01") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(14.dp)) {
                    ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(220.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(plan.session.title, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(plan.phase.focus, color = NztMuted, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { context.startActivity(Intent(context, V4WorkoutActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(V3Strings.t(lang, "start"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { context.startActivity(Intent(context, ProTrainingLibraryActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text("${V3Strings.t(lang, "workouts")} / Library")
                    }
                }
            }
        }
        item { V4Section(V3Strings.t(lang, "nutrition"), "02") }
        item {
            V4ActionCard(Icons.Default.Restaurant, V3Strings.t(lang, "dailyFuel"), "Calories • protein • fats • carbs • water") {
                context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString()))
            }
        }
        item { V4Section(V3Strings.t(lang, "measurements"), "03") }
        item {
            V4ActionCard(Icons.Default.MonitorWeight, V3Strings.t(lang, "measurements"), last?.let { "${it.weight ?: "—"} kg • waist ${it.waist ?: "—"} cm" } ?: V3Strings.t(lang, "noData")) { showMeasure = true }
        }
    }

    if (showMeasure) V4MeasurementDialog(repo, lang) { showMeasure = false }
}

@Composable
private fun V4ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = NztSurface2, shape = RoundedCornerShape(15.dp)) { Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = NztAccent) } }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(subtitle, color = NztMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Icon(Icons.Default.ChevronRight, null, tint = NztMuted)
        }
    }
}

@Composable
private fun V4MeasurementDialog(repo: NZTRepository, lang: AppLanguage, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf("") }; var waist by remember { mutableStateOf("") }; var chest by remember { mutableStateOf("") }; var arm by remember { mutableStateOf("") }; var thigh by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(V3Strings.t(lang, "measurements")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Weight kg" to weight, "Waist cm" to waist, "Chest cm" to chest, "Arm cm" to arm, "Thigh cm" to thigh).forEachIndexed { i, pair ->
                    OutlinedTextField(
                        value = pair.second,
                        onValueChange = { v -> when (i) { 0 -> weight = v; 1 -> waist = v; 2 -> chest = v; 3 -> arm = v; else -> thigh = v } },
                        label = { Text(pair.first) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                repo.saveBody(BodyLog(LocalDate.now().toString(), weight.toDoubleOrNull(), waist.toDoubleOrNull(), chest.toDoubleOrNull(), arm.toDoubleOrNull(), thigh.toDoubleOrNull()))
                onDismiss()
            }) { Text(V3Strings.t(lang, "save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(V3Strings.t(lang, "close")) } },
        containerColor = NztSurface
    )
}

@Composable
private fun V4Growth(repo: NZTRepository, lang: AppLanguage) {
    var mode by remember { mutableStateOf(V4GrowthMode.BOOKS) }
    Column(Modifier.fillMaxSize()) {
        V4Header(null, lang, V3Strings.t(lang, "growth"))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            SegmentedButton(selected = mode == V4GrowthMode.BOOKS, onClick = { mode = V4GrowthMode.BOOKS }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(V3Strings.t(lang, "books")) }
            SegmentedButton(selected = mode == V4GrowthMode.IMPACT, onClick = { mode = V4GrowthMode.IMPACT }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(V3Strings.t(lang, "impact")) }
        }
        Spacer(Modifier.height(6.dp))
        if (mode == V4GrowthMode.BOOKS) V3BooksScreen(lang, Modifier.fillMaxSize()) else V4Impact(repo, lang)
    }
}

@Composable
private fun V4Impact(repo: NZTRepository, lang: AppLanguage) {
    var type by remember { mutableStateOf("Career") }
    var note by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val logs = remember(refresh) { repo.growthLogs() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(V3Strings.t(lang, "impact"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Career", "Mind", "Influence", "Money").forEach { value ->
                            FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value, fontSize = 11.sp) })
                        }
                    }
                    OutlinedTextField(note, { note = it }, label = { Text(V3Strings.t(lang, "note")) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { if (note.isNotBlank()) { repo.addGrowth(type, note.trim()); note = ""; refresh++ } }, modifier = Modifier.fillMaxWidth()) { Text(V3Strings.t(lang, "save")) }
                }
            }
        }
        items(logs.take(20)) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) { Text(log.type, color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(log.note, fontWeight = FontWeight.SemiBold); Text(log.date, color = NztMuted, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun V4Progress(repo: NZTRepository, lang: AppLanguage) {
    val tasks = repo.tasksForToday(); val score = repo.completionPercent(tasks); val body = repo.bodyLogs(); val growth = repo.growthLogs(); val money = repo.moneyLogs()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { V4Header(null, lang, V3Strings.t(lang, "progress")) }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V4Metric("NZT", "$score / 100", Modifier.weight(1f)); V4Metric(V3Strings.t(lang, "streak"), "${repo.dayNumber()} d", Modifier.weight(1f))
            }
        }
        item { V4Section(V3Strings.t(lang, "trend"), "07D") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    val vals = listOf(42, 58, 64, 51, 72, 78, score.coerceAtLeast(4))
                    vals.forEachIndexed { index, v ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("D${index + 1}", color = NztMuted, fontSize = 10.sp, modifier = Modifier.width(28.dp))
                            LinearProgressIndicator(progress = { v / 100f }, modifier = Modifier.weight(1f).height(7.dp), color = if (index == 6) NztAccent else NztAccent2, trackColor = NztLine)
                            Text(" $v", fontSize = 10.sp, color = NztMuted)
                        }
                    }
                }
            }
        }
        item { V4ActionCard(Icons.Default.MonitorWeight, V3Strings.t(lang, "measurements"), "${body.size} check-ins") {} }
        item { V4ActionCard(Icons.Default.AutoGraph, V3Strings.t(lang, "impact"), "${growth.size} actions") {} }
        item { V4ActionCard(Icons.Default.AccountBalanceWallet, V3Strings.t(lang, "money"), "${money.size} snapshots") {} }
    }
}

@Composable
private fun V4Metric(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) { Text(label.uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(value, color = NztAccent, fontSize = 26.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun V4Settings(profile: ProfileStore, lang: AppLanguage, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var editName by remember { mutableStateOf(profile.name()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { V4Header(null, lang, V3Strings.t(lang, "settings")) }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("PROFILE", color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(editName, { if (it.length <= 28) editName = it }, label = { Text(V3Strings.t(lang, "name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { profile.setName(editName.trim()); onProfileChanged() }, enabled = editName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(V3Strings.t(lang, "save")) }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(V3Strings.t(lang, "language").uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AppLanguage.entries.forEach { l ->
                        ListItem(
                            headlineContent = { Text(l.label) },
                            leadingContent = { RadioButton(selected = l == lang, onClick = { onLanguage(l) }) },
                            modifier = Modifier.clickable { onLanguage(l) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("NZT 365", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("v4.0.0 • local-first • signed release", color = NztMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
