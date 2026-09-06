package com.nzt365.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime

val NztBg = Color(0xFF071018)
val NztSurface = Color(0xFF0E1923)
val NztSurface2 = Color(0xFF132330)
val NztAccent = Color(0xFFE8FF62)
val NztAccent2 = Color(0xFF74E6C4)
val NztText = Color(0xFFF2F5F7)
val NztMuted = Color(0xFF8E9AA6)
val NztLine = Color(0xFF243440)
val NztDanger = Color(0xFFFF7D7D)

@Composable
fun NZTProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NztAccent,
            secondary = NztAccent2,
            background = NztBg,
            surface = NztSurface,
            surfaceVariant = NztSurface2,
            onPrimary = Color(0xFF101500),
            onBackground = NztText,
            onSurface = NztText
        ),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}

private enum class ProTab { TODAY, BODY, GROWTH, PROGRESS, SETTINGS }

@Composable
fun NZTProRoot(repo: NZTRepository, profileStore: ProfileStore) {
    var onboarded by remember { mutableStateOf(profileStore.isOnboarded()) }
    var language by remember { mutableStateOf(profileStore.language()) }
    var profileVersion by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalAppLanguage provides language) {
        NZTProTheme {
            if (!onboarded) {
                OnboardingScreen(profileStore) { name, lang ->
                    profileStore.save(name, lang)
                    language = lang
                    onboarded = true
                    profileVersion++
                }
            } else {
                ProApp(
                    repo = repo,
                    profileStore = profileStore,
                    profileVersion = profileVersion,
                    onLanguageChanged = { language = it; profileVersion++ },
                    onProfileChanged = { profileVersion++ }
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(store: ProfileStore, onDone: (String, AppLanguage) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(store.language()) }
    val l = Localizer(lang)

    Box(Modifier.fillMaxSize().background(NztBg).padding(24.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.padding(top = 44.dp)) {
                Box(
                    Modifier.size(64.dp).background(NztAccent, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("N", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 30.sp) }
                Spacer(Modifier.height(28.dp))
                Text(l.t("welcome"), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text(l.t("tagline"), color = NztMuted, fontSize = 18.sp, lineHeight = 26.sp)
                Spacer(Modifier.height(34.dp))
                Text(l.t("your_name"), color = NztMuted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 28) name = it },
                    placeholder = { Text(l.t("name_hint")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(26.dp))
                Text(l.t("choose_language"), color = NztMuted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                AppLanguage.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            FilterChip(
                                selected = lang == item,
                                onClick = { lang = item },
                                label = { Text(item.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Button(
                onClick = { if (name.trim().isNotBlank()) onDone(name.trim(), lang) },
                enabled = name.trim().isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text(l.t("continue"), fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        }
    }
}

@Composable
private fun ProApp(
    repo: NZTRepository,
    profileStore: ProfileStore,
    profileVersion: Int,
    onLanguageChanged: (AppLanguage) -> Unit,
    onProfileChanged: () -> Unit
) {
    var tab by remember { mutableStateOf(ProTab.TODAY) }
    val l = rememberLocalizer()
    val name = remember(profileVersion) { profileStore.name() }

    Scaffold(
        containerColor = NztBg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF09131C), tonalElevation = 0.dp) {
                ProNavItem(tab, ProTab.TODAY, Icons.Default.Home, l.t("today")) { tab = it }
                ProNavItem(tab, ProTab.BODY, Icons.Default.FitnessCenter, l.t("body")) { tab = it }
                ProNavItem(tab, ProTab.GROWTH, Icons.Default.AutoGraph, l.t("growth")) { tab = it }
                ProNavItem(tab, ProTab.PROGRESS, Icons.Default.BarChart, l.t("progress")) { tab = it }
                ProNavItem(tab, ProTab.SETTINGS, Icons.Default.Settings, l.t("settings")) { tab = it }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(NztBg)) {
            when (tab) {
                ProTab.TODAY -> ProTodayScreen(repo, name)
                ProTab.BODY -> ProBodyScreen(repo)
                ProTab.GROWTH -> ProGrowthScreen(repo)
                ProTab.PROGRESS -> ProProgressScreen(repo)
                ProTab.SETTINGS -> ProSettingsScreen(profileStore, onLanguageChanged, onProfileChanged)
            }
        }
    }
}

@Composable
private fun RowScope.ProNavItem(current: ProTab, target: ProTab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (ProTab) -> Unit) {
    NavigationBarItem(
        selected = current == target,
        onClick = { onClick(target) },
        icon = { Icon(icon, label) },
        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = NztText,
            indicatorColor = NztAccent,
            unselectedIconColor = NztMuted,
            unselectedTextColor = NztMuted
        )
    )
}

@Composable
private fun ProHeader(name: String? = null, trailing: String? = null) {
    val l = rememberLocalizer()
    val hour = LocalDateTime.now().hour
    val greeting = when { hour < 11 -> l.t("good_morning"); hour < 18 -> l.t("good_day"); else -> l.t("good_evening") }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("NZT 365", fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            if (!name.isNullOrBlank()) Text("$greeting, $name", color = NztMuted, fontSize = 14.sp)
        }
        if (!trailing.isNullOrBlank()) {
            Surface(color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
                Text(trailing, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ProTodayScreen(repo: NZTRepository, name: String) {
    val l = rememberLocalizer()
    var tasks by remember { mutableStateOf(repo.tasksForToday()) }
    val score = repo.completionPercent(tasks)
    val day = repo.dayNumber()
    val context = LocalContext.current
    val plan = remember { StathamEngine.forDate(LocalDate.now()) }
    val targets = remember { StathamEngine.targets(plan) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ProHeader(name, "${l.t("day")} $day / 365") }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ProtocolHero(score, day)
            }
        }
        item {
            SectionHeader(l.t("focus_today"), "01")
            FocusWorkoutCard(plan.session.title, plan.session.minutes, targets.calories, targets.protein) {
                context.startActivity(Intent(context, ProWorkoutActivity::class.java).putExtra("date", LocalDate.now().toString()))
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Default.Restaurant, l.t("nutrition"), "${targets.calories} kcal", Modifier.weight(1f)) {
                    context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", LocalDate.now().toString()))
                }
                QuickTile(Icons.Default.SelfImprovement, l.t("recovery"), plan.phase.name, Modifier.weight(1f)) {}
            }
        }
        item { SectionHeader(l.t("daily_actions"), "02") }
        items(tasks, key = { it.id }) { task ->
            DailyActionRow(task) { delta ->
                tasks = tasks.map { if (it.id == task.id) it.copy(done = (it.done + delta).coerceIn(0, it.target)) else it }
                repo.saveTasks(tasks)
            }
        }
        item {
            Text(l.t("no_zero_day"), Modifier.padding(20.dp), color = NztMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProtocolHero(score: Int, day: Int) {
    val l = rememberLocalizer()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NztSurface),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(l.t("protocol"), color = NztMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(l.t("score"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = if (score >= 80) NztAccent else NztSurface2, shape = CircleShape) {
                    Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Text(score.toString(), color = if (score >= 80) Color.Black else NztText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = NztAccent,
                trackColor = NztLine
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${l.t("day")} $day", color = NztMuted)
                Text("365", color = NztAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusWorkoutCard(title: String, minutes: Int, kcal: Int, protein: Int, onStart: () -> Unit) {
    val l = rememberLocalizer()
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NztSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseIllustration(title, Modifier.size(108.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(l.t("workout"), color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$minutes min • $kcal kcal • P $protein g", color = NztMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(l.t("start_workout"), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun QuickTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, title, tint = NztAccent)
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = NztMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DailyActionRow(task: DailyTask, onChange: (Int) -> Unit) {
    val progress = if (task.target > 0) task.done.toFloat() / task.target else 0f
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NztSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(if (task.done >= task.target) NztAccent else NztSurface2, CircleShape), contentAlignment = Alignment.Center) {
                if (task.done >= task.target) Icon(Icons.Default.Check, null, tint = Color.Black) else Text(task.done.toString(), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(task.category, color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(4.dp), color = NztAccent, trackColor = NztLine)
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(onClick = { onChange(if (task.done < task.target) 1 else -task.done) }) {
                Icon(if (task.done < task.target) Icons.Default.Add else Icons.Default.Refresh, null)
            }
        }
    }
}

@Composable
private fun ProBodyScreen(repo: NZTRepository) {
    val l = rememberLocalizer()
    val context = LocalContext.current
    val plan = remember { StathamEngine.forDate(LocalDate.now()) }
    var weight by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var arm by remember { mutableStateOf("") }
    var thigh by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProHeader(trailing = "${l.t("week")} ${plan.week}") }
        item {
            SectionHeader(l.t("training_plan"), "BODY")
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp)) {
                    ExerciseIllustration(plan.session.title, Modifier.fillMaxWidth().height(180.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(plan.session.title, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("${plan.phase.name} • ${plan.session.minutes} min", color = NztMuted)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { context.startActivity(Intent(context, ProWorkoutActivity::class.java).putExtra("date", LocalDate.now().toString())) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)
                    ) { Text(l.t("start_workout"), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(context, ProTrainingLibraryActivity::class.java).putExtra("date", LocalDate.now().toString())) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)
                    ) { Text(l.t("replace_workout")) }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Default.MenuBook, l.t("exercise_library"), "Form • load • progress", Modifier.weight(1f)) {
                    context.startActivity(Intent(context, ProTrainingLibraryActivity::class.java))
                }
                QuickTile(Icons.Default.Restaurant, l.t("nutrition"), "Macros • meals", Modifier.weight(1f)) {
                    context.startActivity(Intent(context, ProNutritionActivity::class.java))
                }
            }
        }
        item { SectionHeader(l.t("measurements"), "CHECK-IN") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniNumberField(l.t("weight"), weight, { weight = it }, Modifier.weight(1f))
                        MiniNumberField(l.t("waist"), waist, { waist = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniNumberField(l.t("chest"), chest, { chest = it }, Modifier.weight(1f))
                        MiniNumberField(l.t("arm"), arm, { arm = it }, Modifier.weight(1f))
                        MiniNumberField(l.t("thigh"), thigh, { thigh = it }, Modifier.weight(1f))
                    }
                    Button(onClick = {
                        repo.saveBody(BodyLog(LocalDate.now().toString(), weight.toDoubleOrNull(), waist.toDoubleOrNull(), chest.toDoubleOrNull(), arm.toDoubleOrNull(), thigh.toDoubleOrNull()))
                        saved = true
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (saved) "✓ ${l.t("done")}" else l.t("save")) }
                }
            }
        }
        item {
            SectionHeader(l.t("check_in"), "ADAPT")
            InfoCard(Icons.Default.Tune, l.t("check_in"), l.t("soon"), NztAccent2)
        }
    }
}

@Composable
private fun MiniNumberField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = { onValue(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.')) }, label = { Text(label, fontSize = 11.sp) }, singleLine = true, modifier = modifier)
}

private data class ProBook(val author: String, val title: String)
private val proBooks = listOf(
    ProBook("Chris Voss", "Never Split the Difference"), ProBook("James Clear", "Atomic Habits"), ProBook("Morgan Housel", "The Psychology of Money"), ProBook("Robert Cialdini", "Influence"),
    ProBook("Cal Newport", "Deep Work"), ProBook("Peter Drucker", "The Effective Executive"), ProBook("Jim Collins", "Good to Great"), ProBook("Ray Dalio", "Principles"),
    ProBook("Daniel Kahneman", "Thinking, Fast and Slow"), ProBook("Robert Greene", "The 48 Laws of Power"), ProBook("Nassim Nicholas Taleb", "Antifragile"), ProBook("Benjamin Graham", "The Intelligent Investor")
)

@Composable
private fun ProGrowthScreen(repo: NZTRepository) {
    val l = rememberLocalizer()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nzt_books", Context.MODE_PRIVATE) }
    var refresh by remember { mutableIntStateOf(0) }
    var type by remember { mutableStateOf(l.t("career")) }
    var note by remember { mutableStateOf("") }
    val logs = remember(refresh) { repo.growthLogs() }
    val types = listOf(l.t("career"), l.t("mind"), l.t("influence"), l.t("money"))

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProHeader(trailing = l.t("growth")) }
        item { SectionHeader(l.t("books"), "MIND") }
        items(proBooks.indices.toList()) { index ->
            val book = proBooks[index]
            var progress by remember(refresh, index) { mutableIntStateOf(prefs.getInt("book_${index + 1}", 0)) }
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(if (progress >= 100) NztAccent else NztSurface2, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = if (progress >= 100) Color.Black else NztAccent, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(book.author, color = NztMuted, fontSize = 12.sp)
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(4.dp), color = NztAccent, trackColor = NztLine)
                    }
                    Spacer(Modifier.width(10.dp))
                    FilledTonalButton(onClick = {
                        progress = (progress + 10).coerceAtMost(100)
                        prefs.edit().putInt("book_${index + 1}", progress).apply()
                        refresh++
                    }) { Text("$progress%") }
                }
            }
        }
        item { SectionHeader(l.t("impact_log"), "CAREER") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        types.forEach { item -> FilterChip(selected = type == item, onClick = { type = item }, label = { Text(item, fontSize = 11.sp) }) }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, placeholder = { Text(l.t("what_done")) }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(16.dp))
                    Button(onClick = { if (note.isNotBlank()) { repo.addGrowth(type, note.trim()); note = ""; refresh++ } }, modifier = Modifier.fillMaxWidth()) { Text(l.t("add_action")) }
                }
            }
        }
        if (logs.isNotEmpty()) {
            item { SectionHeader(l.t("history"), "LOG") }
            items(logs.take(12)) { log ->
                Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(log.type, color = NztAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(log.date, color = NztMuted, fontSize = 11.sp) }
                        Text(log.note, Modifier.padding(top = 5.dp), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProProgressScreen(repo: NZTRepository) {
    val l = rememberLocalizer()
    val body = remember { repo.bodyLogs() }
    val growth = remember { repo.growthLogs() }
    val money = remember { repo.moneyLogs() }
    val tasks = repo.tasksForToday()
    val score = repo.completionPercent(tasks)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProHeader(trailing = l.t("progress")) }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(l.t("score"), score.toString(), "/100", NztAccent, Modifier.weight(1f))
                MetricCard(l.t("streak"), repo.dayNumber().toString(), l.t("days"), NztAccent2, Modifier.weight(1f))
            }
        }
        item { SectionHeader(l.t("consistency"), "TREND") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("7D PROTOCOL", color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    MiniBarChart(listOf(42, 56, 61, 48, 72, 80, score))
                }
            }
        }
        item { SectionHeader(l.t("body_progress"), "BODY") }
        item { ProgressSummaryCard(Icons.Default.MonitorWeight, l.t("measurements"), body.size.toString(), body.firstOrNull()?.let { "${it.weight ?: "—"} kg • ${it.waist ?: "—"} cm" } ?: "—") }
        item { SectionHeader(l.t("growth_progress"), "MIND / CAREER") }
        item { ProgressSummaryCard(Icons.Default.Psychology, l.t("impact_log"), growth.size.toString(), growth.firstOrNull()?.note ?: "—") }
        item { SectionHeader(l.t("money_progress"), "MONEY") }
        item { ProgressSummaryCard(Icons.Default.AccountBalanceWallet, l.t("money"), money.size.toString(), money.firstOrNull()?.let { "${it.mainIncome + it.sideIncome} / ${it.capital}" } ?: "—") }
    }
}

@Composable
private fun MiniBarChart(values: List<Int>) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val max = 100f
        val gap = 10f
        val barW = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = size.height * (v / max)
            val x = i * (barW + gap)
            drawLine(NztLine, Offset(x + barW / 2, size.height), Offset(x + barW / 2, 0f), barW, StrokeCap.Round)
            drawLine(if (i == values.lastIndex) NztAccent else NztAccent2.copy(alpha = .65f), Offset(x + barW / 2, size.height), Offset(x + barW / 2, size.height - h), barW, StrokeCap.Round)
        }
    }
}

@Composable
private fun ProSettingsScreen(store: ProfileStore, onLanguageChanged: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    val l = rememberLocalizer()
    var name by remember { mutableStateOf(store.name()) }
    var language by remember { mutableStateOf(store.language()) }
    var editing by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProHeader(trailing = "v2.0") }
        item { SectionHeader(l.t("profile"), "ACCOUNT") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).background(NztAccent, CircleShape), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = Color.Black, fontSize = 23.sp, fontWeight = FontWeight.Black) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(l.t("offline"), color = NztMuted, fontSize = 11.sp) }
                        IconButton(onClick = { editing = !editing }) { Icon(Icons.Default.Edit, l.t("edit_name")) }
                    }
                    if (editing) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = name, onValueChange = { if (it.length <= 28) name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = { if (name.isNotBlank()) { store.setName(name); editing = false; onProfileChanged() } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(l.t("save")) }
                    }
                }
            }
        }
        item { SectionHeader(l.t("language"), "4 LANG") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(14.dp)) {
                    AppLanguage.entries.forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable {
                            language = item; store.setLanguage(item); onLanguageChanged(item)
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = language == item, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(item.label, Modifier.weight(1f), fontWeight = if (language == item) FontWeight.Bold else FontWeight.Normal)
                            Text(item.code.uppercase(), color = NztMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item { SectionHeader(l.t("about"), "NZT") }
        item { InfoCard(Icons.Default.Security, l.t("version"), "NZT 365 v2.0 • local-first • Android", NztAccent) }
    }
}

@Composable
private fun SectionHeader(title: String, code: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), color = NztMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(code, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, accent: Color) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(accent.copy(alpha = .14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }
            Spacer(Modifier.width(12.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = NztMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, suffix: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title.uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) { Text(value, fontSize = 30.sp, fontWeight = FontWeight.Black, color = accent); Spacer(Modifier.width(4.dp)); Text(suffix, color = NztMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 5.dp)) }
        }
    }
}

@Composable
private fun ProgressSummaryCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, subtitle: String) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(NztSurface2, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = NztAccent) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = NztMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = NztAccent)
        }
    }
}
