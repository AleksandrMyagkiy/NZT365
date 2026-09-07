package com.nzt365.app

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime

private enum class V3Tab { TODAY, BODY, GROWTH, PROGRESS, SETTINGS }
private enum class V3GrowthMode { BOOKS, IMPACT }

@Composable
fun NZT3Root(repo: NZTRepository, profileStore: ProfileStore) {
    var onboarded by remember { mutableStateOf(profileStore.isOnboarded()) }
    var lang by remember { mutableStateOf(profileStore.language()) }
    var profileTick by remember { mutableIntStateOf(0) }
    NZTProTheme {
        if (!onboarded) {
            V3Onboarding(lang) { name, language ->
                profileStore.save(name, language); lang = language; onboarded = true; profileTick++
            }
        } else {
            V3Shell(repo, profileStore, lang, profileTick,
                onLanguage = { profileStore.setLanguage(it); lang = it; profileTick++ },
                onProfileChanged = { profileTick++ })
        }
    }
}

@Composable
private fun V3Onboarding(initial: AppLanguage, onDone: (String, AppLanguage) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(initial) }
    Scaffold(containerColor = NztBg, contentWindowInsets = WindowInsets.safeDrawing) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Spacer(Modifier.height(28.dp))
                Text("NZT 365", fontSize = 40.sp, fontWeight = FontWeight.Black)
                Text(V3Strings.t(lang, "protocol"), color = NztAccent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(name, { if (it.length <= 28) name = it }, label = { Text(V3Strings.t(lang, "name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(18.dp))
                Text(V3Strings.t(lang, "language"), color = NztMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AppLanguage.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { l -> FilterChip(selected = l == lang, onClick = { lang = l }, label = { Text(l.label) }, modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Button(onClick = { onDone(name.trim(), lang) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
                Text(V3Strings.t(lang, "start"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V3Shell(repo: NZTRepository, profile: ProfileStore, lang: AppLanguage, profileTick: Int, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var tab by remember { mutableStateOf(V3Tab.TODAY) }
    val name = remember(profileTick) { profile.name() }
    Scaffold(containerColor = NztBg, contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF09131C), modifier = Modifier.navigationBarsPadding()) {
                V3Nav(tab, V3Tab.TODAY, Icons.Default.Home, V3Strings.t(lang, "today")) { tab = it }
                V3Nav(tab, V3Tab.BODY, Icons.Default.FitnessCenter, V3Strings.t(lang, "body")) { tab = it }
                V3Nav(tab, V3Tab.GROWTH, Icons.Default.AutoGraph, V3Strings.t(lang, "growth")) { tab = it }
                V3Nav(tab, V3Tab.PROGRESS, Icons.Default.BarChart, V3Strings.t(lang, "progress")) { tab = it }
                V3Nav(tab, V3Tab.SETTINGS, Icons.Default.Settings, V3Strings.t(lang, "settings")) { tab = it }
            }
        }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                V3Tab.TODAY -> V3Today(repo, name, lang)
                V3Tab.BODY -> V3Body(repo, lang)
                V3Tab.GROWTH -> V3Growth(repo, lang)
                V3Tab.PROGRESS -> V3Progress(repo, lang)
                V3Tab.SETTINGS -> V3Settings(profile, lang, onLanguage, onProfileChanged)
            }
        }
    }
}

@Composable
private fun RowScope.V3Nav(current: V3Tab, target: V3Tab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (V3Tab) -> Unit) {
    NavigationBarItem(selected = current == target, onClick = { onClick(target) }, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 9.sp, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, indicatorColor = NztAccent, selectedTextColor = NztText, unselectedIconColor = NztMuted, unselectedTextColor = NztMuted))
}

@Composable
private fun V3Header(name: String?, lang: AppLanguage, badge: String? = null) {
    val hour = LocalDateTime.now().hour
    val greeting = when (lang) {
        AppLanguage.RU -> if (hour < 11) "Доброе утро" else if (hour < 18) "Добрый день" else "Добрый вечер"
        AppLanguage.EN -> if (hour < 11) "Good morning" else if (hour < 18) "Good afternoon" else "Good evening"
        AppLanguage.PL -> if (hour < 18) "Dzień dobry" else "Dobry wieczór"
        AppLanguage.UK -> if (hour < 11) "Доброго ранку" else if (hour < 18) "Добрий день" else "Добрий вечір"
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("NZT 365", fontSize = 30.sp, fontWeight = FontWeight.Black); if (!name.isNullOrBlank()) Text("$greeting, $name", color = NztMuted, fontSize = 13.sp) }
        if (badge != null) Surface(color = NztSurface2, shape = RoundedCornerShape(15.dp)) { Text(badge, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
    }
}

@Composable
private fun V3Section(title: String, code: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(code, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V3Today(repo: NZTRepository, name: String, lang: AppLanguage) {
    val context = LocalContext.current; val date = LocalDate.now(); val plan = remember(date) { StathamEngine.forDate(date) }; val targets = remember(plan) { StathamEngine.targets(plan) }
    var tasks by remember { mutableStateOf(repo.tasksForToday()) }; val score = repo.completionPercent(tasks)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { V3Header(name, lang, "DAY ${repo.dayNumber()} / 365") }
        item { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) { Column(Modifier.padding(20.dp)) { Text(V3Strings.t(lang, "score").uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("$score / 100", color = NztAccent, fontSize = 34.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = NztAccent, trackColor = NztLine) } } }
        item { V3Section(V3Strings.t(lang, "todayPlan"), "01") }
        item { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(16.dp)) { ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(190.dp)); Spacer(Modifier.height(12.dp)); Text(plan.session.title, fontSize = 21.sp, fontWeight = FontWeight.Black); Text("${plan.session.minutes} min • ${plan.session.exercises.size} exercises", color = NztMuted); Spacer(Modifier.height(12.dp)); Button(onClick = { context.startActivity(Intent(context, V3WorkoutActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(V3Strings.t(lang, "start"), fontWeight = FontWeight.Bold) } } } }
        item { Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { V3Quick(Icons.Default.Restaurant, V3Strings.t(lang, "nutrition"), "${targets.calories} kcal", Modifier.weight(1f)) { context.startActivity(Intent(context, V3NutritionActivity::class.java).putExtra("date", date.toString())) }; V3Quick(Icons.Default.SelfImprovement, V3Strings.t(lang, "recovery"), plan.phase.name, Modifier.weight(1f)) {} } }
        item { V3Section(V3Strings.t(lang, "daily_actions"), "02") }
        items(tasks, key = { it.id }) { task -> Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = task.done >= task.target, onCheckedChange = { checked -> tasks = tasks.map { if (it.id == task.id) it.copy(done = if (checked) it.target else 0) else it }; repo.saveTasks(tasks) }); Column(Modifier.weight(1f)) { Text(task.title, fontWeight = FontWeight.Bold); Text(task.category, color = NztMuted, fontSize = 11.sp) } } } }
    }
}

@Composable
private fun V3Quick(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(14.dp)) { Icon(icon, null, tint = NztAccent); Spacer(Modifier.height(10.dp)); Text(title, fontWeight = FontWeight.Bold, maxLines = 1); Text(subtitle, color = NztMuted, fontSize = 11.sp, maxLines = 2) } }
}

@Composable
private fun V3Body(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current; val date = LocalDate.now(); val plan = remember(date) { StathamEngine.forDate(date) }; var showMeasure by remember { mutableStateOf(false) }; val last = repo.bodyLogs().firstOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { V3Header(null, lang, V3Strings.t(lang, "body")) }; item { V3Section(V3Strings.t(lang, "workout"), "01") }
        item { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(16.dp)) { ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(200.dp)); Spacer(Modifier.height(12.dp)); Text(plan.session.title, fontWeight = FontWeight.Black, fontSize = 21.sp); Text(plan.phase.focus, color = NztMuted, fontSize = 13.sp); Spacer(Modifier.height(12.dp)); Button(onClick = { context.startActivity(Intent(context, V3WorkoutActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth()) { Text(V3Strings.t(lang, "start")) }; Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { context.startActivity(Intent(context, V3NutritionActivity::class.java).putExtra("date", date.toString())) }, modifier = Modifier.fillMaxWidth()) { Text(V3Strings.t(lang, "nutrition")) } } } }
        item { V3Section(V3Strings.t(lang, "measurements"), "02") }
        item { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable { showMeasure = true }, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MonitorWeight, null, tint = NztAccent); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(V3Strings.t(lang, "measurements"), fontWeight = FontWeight.Bold); Text(last?.let { "${it.weight ?: "—"} kg • ${it.waist ?: "—"} cm" } ?: V3Strings.t(lang, "noData"), color = NztMuted, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = NztMuted) } } }
    }
    if (showMeasure) V3MeasurementDialog(repo, lang) { showMeasure = false }
}

@Composable
private fun V3MeasurementDialog(repo: NZTRepository, lang: AppLanguage, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf("") }; var waist by remember { mutableStateOf("") }; var chest by remember { mutableStateOf("") }; var arm by remember { mutableStateOf("") }; var thigh by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(V3Strings.t(lang, "measurements")) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(weight, { weight = it }, label = { Text("kg") }, singleLine = true); OutlinedTextField(waist, { waist = it }, label = { Text("waist cm") }, singleLine = true); OutlinedTextField(chest, { chest = it }, label = { Text("chest cm") }, singleLine = true); OutlinedTextField(arm, { arm = it }, label = { Text("arm cm") }, singleLine = true); OutlinedTextField(thigh, { thigh = it }, label = { Text("thigh cm") }, singleLine = true) } }, confirmButton = { Button(onClick = { repo.saveBody(BodyLog(LocalDate.now().toString(), weight.toDoubleOrNull(), waist.toDoubleOrNull(), chest.toDoubleOrNull(), arm.toDoubleOrNull(), thigh.toDoubleOrNull())); onDismiss() }) { Text(V3Strings.t(lang, "save")) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(V3Strings.t(lang, "close")) } }, containerColor = NztSurface)
}

@Composable
private fun V3Growth(repo: NZTRepository, lang: AppLanguage) {
    var mode by remember { mutableStateOf(V3GrowthMode.BOOKS) }
    Column(Modifier.fillMaxSize()) { V3Header(null, lang, V3Strings.t(lang, "growth")); SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { SegmentedButton(selected = mode == V3GrowthMode.BOOKS, onClick = { mode = V3GrowthMode.BOOKS }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(V3Strings.t(lang, "books")) }; SegmentedButton(selected = mode == V3GrowthMode.IMPACT, onClick = { mode = V3GrowthMode.IMPACT }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(V3Strings.t(lang, "impact")) } }; Spacer(Modifier.height(8.dp)); if (mode == V3GrowthMode.BOOKS) V3BooksScreen(lang, Modifier.weight(1f)) else V3Impact(repo, lang, Modifier.weight(1f)) }
}

@Composable
private fun V3Impact(repo: NZTRepository, lang: AppLanguage, modifier: Modifier) {
    var note by remember { mutableStateOf("") }; var tick by remember { mutableIntStateOf(0) }; val logs = remember(tick) { repo.growthLogs() }
    LazyColumn(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp)) { OutlinedTextField(note, { note = it }, label = { Text(V3Strings.t(lang, "note")) }, modifier = Modifier.fillMaxWidth(), minLines = 3); Spacer(Modifier.height(8.dp)); Button(onClick = { if (note.isNotBlank()) { repo.addGrowth("Career", note.trim()); note = ""; tick++ } }, modifier = Modifier.fillMaxWidth()) { Text("+ ${V3Strings.t(lang, "impact")}") } } } }; items(logs) { log -> Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp)) { Text(log.type, color = NztAccent, fontWeight = FontWeight.Bold); Text(log.note); Text(log.date, color = NztMuted, fontSize = 11.sp) } } } }
}

@Composable
private fun V3Progress(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current; val score = repo.completionPercent(repo.tasksForToday()); val training = remember { V3TrainingStore(context) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { V3Header(null, lang, V3Strings.t(lang, "progress")) }; item { Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { V3Stat(V3Strings.t(lang, "score"), "$score / 100", Modifier.weight(1f)); V3Stat(V3Strings.t(lang, "workouts"), "${training.completedCount()}", Modifier.weight(1f)) } }; item { V3Section(V3Strings.t(lang, "body"), "BODY") }; item { V3Summary(Icons.Default.MonitorWeight, V3Strings.t(lang, "measurements"), repo.bodyLogs().size.toString()) }; item { V3Section(V3Strings.t(lang, "growth"), "MIND") }; item { V3Summary(Icons.Default.Psychology, V3Strings.t(lang, "impact"), repo.growthLogs().size.toString()) }; item { V3Section(V3Strings.t(lang, "money"), "MONEY") }; item { V3Summary(Icons.Default.AccountBalanceWallet, V3Strings.t(lang, "money"), repo.moneyLogs().size.toString()) } }
}

@Composable private fun V3Stat(label: String, value: String, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text(label.uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(value, color = NztAccent, fontSize = 28.sp, fontWeight = FontWeight.Black) } } }
@Composable private fun V3Summary(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = NztAccent); Spacer(Modifier.width(14.dp)); Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(value, color = NztAccent, fontSize = 26.sp, fontWeight = FontWeight.Black) } } }

@Composable
private fun V3Settings(profile: ProfileStore, lang: AppLanguage, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var name by remember { mutableStateOf(profile.name()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { V3Header(null, lang, V3Strings.t(lang, "settings")) }; item { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp)) { OutlinedTextField(name, { if (it.length <= 28) name = it }, label = { Text(V3Strings.t(lang, "name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(8.dp)); Button(onClick = { profile.setName(name.trim()); onProfileChanged() }, modifier = Modifier.fillMaxWidth()) { Text(V3Strings.t(lang, "save")) } } } }; item { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(12.dp)) { AppLanguage.entries.forEach { l -> Row(Modifier.fillMaxWidth().clickable { onLanguage(l) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = l == lang, onClick = { onLanguage(l) }); Spacer(Modifier.width(8.dp)); Text(l.label) } } } } }; item { Text("NZT 365 • v3.0.0", Modifier.padding(horizontal = 20.dp), color = NztMuted, fontSize = 12.sp) } }
}
