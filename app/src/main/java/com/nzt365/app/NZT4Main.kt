package com.nzt365.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class NZT4Tab { TODAY, PLAN, GROWTH, PROGRESS, PROFILE }
private enum class GrowthSection { BOOKS, IMPACT }

private fun q(lang: AppLanguage, ru: String, en: String, pl: String, uk: String): String = when (lang) {
    AppLanguage.RU -> ru
    AppLanguage.EN -> en
    AppLanguage.PL -> pl
    AppLanguage.UK -> uk
}

@Composable
fun NZT4Root(repo: NZTRepository, profileStore: ProfileStore) {
    var onboarded by remember { mutableStateOf(profileStore.isOnboarded()) }
    var language by remember { mutableStateOf(profileStore.language()) }
    var profileTick by remember { mutableIntStateOf(0) }

    NZTProTheme {
        if (!onboarded) {
            NZT4Onboarding(language) { name, lang ->
                profileStore.save(name, lang)
                language = lang
                onboarded = true
                profileTick++
            }
        } else {
            NZT4Shell(
                repo = repo,
                profile = profileStore,
                lang = language,
                profileTick = profileTick,
                onLanguage = { profileStore.setLanguage(it); language = it; profileTick++ },
                onProfileChanged = { profileTick++ }
            )
        }
    }
}

@Composable
private fun NZT4Onboarding(initial: AppLanguage, onDone: (String, AppLanguage) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(initial) }
    Scaffold(containerColor = NztBg, contentWindowInsets = WindowInsets.safeDrawing) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.height(24.dp))
                Surface(color = NztAccent, shape = RoundedCornerShape(22.dp)) {
                    Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                        Text("N", color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(26.dp))
                Text("NZT 365", fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text(
                    q(lang, "Тело. Фокус. Карьера. Капитал.", "Body. Focus. Career. Capital.", "Ciało. Fokus. Kariera. Kapitał.", "Тіло. Фокус. Кар'єра. Капітал."),
                    color = NztMuted, fontSize = 18.sp
                )
                Spacer(Modifier.height(34.dp))
                Text(q(lang, "Как тебя зовут?", "What's your name?", "Jak masz na imię?", "Як тебе звати?"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 28) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(q(lang, "Имя", "Name", "Imię", "Ім'я")) },
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(q(lang, "Язык", "Language", "Język", "Мова"), color = NztMuted, fontWeight = FontWeight.Bold)
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
                Text(q(lang, "Начать мой год", "Start my year", "Rozpocznij mój rok", "Почати мій рік"), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun NZT4Shell(
    repo: NZTRepository,
    profile: ProfileStore,
    lang: AppLanguage,
    profileTick: Int,
    onLanguage: (AppLanguage) -> Unit,
    onProfileChanged: () -> Unit
) {
    var tab by remember { mutableStateOf(NZT4Tab.TODAY) }
    val name = remember(profileTick) { profile.name() }

    Scaffold(
        containerColor = NztBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF09131C), modifier = Modifier.navigationBarsPadding()) {
                NZT4Nav(tab, NZT4Tab.TODAY, Icons.Default.Home, q(lang,"Сегодня","Today","Dzisiaj","Сьогодні")) { tab = it }
                NZT4Nav(tab, NZT4Tab.PLAN, Icons.Default.FitnessCenter, q(lang,"План","Plan","Plan","План")) { tab = it }
                NZT4Nav(tab, NZT4Tab.GROWTH, Icons.Default.AutoStories, q(lang,"Рост","Growth","Rozwój","Розвиток")) { tab = it }
                NZT4Nav(tab, NZT4Tab.PROGRESS, Icons.Default.ShowChart, q(lang,"Прогресс","Progress","Postęp","Прогрес")) { tab = it }
                NZT4Nav(tab, NZT4Tab.PROFILE, Icons.Default.Person, q(lang,"Профиль","Profile","Profil","Профіль")) { tab = it }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                NZT4Tab.TODAY -> NZT4Today(repo, name, lang)
                NZT4Tab.PLAN -> NZT4Plan(repo, lang)
                NZT4Tab.GROWTH -> NZT4Growth(repo, lang)
                NZT4Tab.PROGRESS -> NZT4Progress(repo, lang)
                NZT4Tab.PROFILE -> NZT4Profile(profile, lang, onLanguage, onProfileChanged)
            }
        }
    }
}

@Composable
private fun RowScope.NZT4Nav(current: NZT4Tab, target: NZT4Tab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (NZT4Tab) -> Unit) {
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
private fun NZT4Header(name: String?, lang: AppLanguage, badge: String? = null) {
    val h = LocalDateTime.now().hour
    val greeting = q(
        lang,
        if (h < 11) "Доброе утро" else if (h < 18) "Добрый день" else "Добрый вечер",
        if (h < 11) "Good morning" else if (h < 18) "Good afternoon" else "Good evening",
        if (h < 18) "Dzień dobry" else "Dobry wieczór",
        if (h < 11) "Доброго ранку" else if (h < 18) "Добрий день" else "Добрий вечір"
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("NZT 365", fontSize = 32.sp, fontWeight = FontWeight.Black)
            if (!name.isNullOrBlank()) Text("$greeting, $name", color = NztMuted, fontSize = 14.sp)
        }
        if (badge != null) {
            Surface(color = NztSurface2, shape = RoundedCornerShape(16.dp)) {
                Text(badge, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = NztAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, action: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        if (action != null) Text(action, color = NztAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun NZT4Today(repo: NZTRepository, name: String, lang: AppLanguage) {
    val context = LocalContext.current
    val date = LocalDate.now()
    val plan = remember(date) { StathamEngine.forDate(date) }
    val targets = remember(plan) { StathamEngine.targets(plan) }
    var tasks by remember { mutableStateOf(repo.tasksForToday()) }
    val score = repo.completionPercent(tasks)
    val workoutTitle = plan.session.title

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { NZT4Header(name, lang, "DAY ${repo.dayNumber()} / 365") }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(q(lang,"ИНДЕКС ДНЯ","DAILY SCORE","INDEKS DNIA","ІНДЕКС ДНЯ"), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("$score", color = NztAccent, fontSize = 46.sp, fontWeight = FontWeight.Black)
                        }
                        Text("/ 100", color = NztMuted, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = NztAccent, trackColor = NztLine)
                }
            }
        }
        item { SectionLabel(q(lang,"Главная задача","Primary mission","Główna misja","Головне завдання"), "01") }
        item {
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NztSurface),
                shape = RoundedCornerShape(26.dp)
            ) {
                Box {
                    NZT4ExercisePhoto(workoutTitle, Modifier.fillMaxWidth().height(255.dp), 26)
                    Box(
                        Modifier.fillMaxWidth().height(255.dp).background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x10000000), Color(0xEE071018))
                            )
                        )
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Text(q(lang,"ТРЕНИРОВКА СЕГОДНЯ","TODAY'S WORKOUT","TRENING NA DZIŚ","ТРЕНУВАННЯ СЬОГОДНІ"), color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(workoutTitle, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Text("${plan.session.minutes} min  •  ${plan.session.exercises.size} ${q(lang,"упражнений","exercises","ćwiczeń","вправ")}", color = Color(0xFFD0D6DB))
                    }
                }
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniMetric(Icons.Default.Timer, "${plan.session.minutes} min", Modifier.weight(1f))
                        MiniMetric(Icons.Default.FitnessCenter, "${plan.session.exercises.sumOf { it.sets }} ${q(lang,"подх.","sets","serii","підх.")}", Modifier.weight(1f))
                        MiniMetric(Icons.Default.TrendingUp, q(lang,"Прогресс","Progress","Progres","Прогрес"), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(context, NZT4WorkoutActivity::class.java).putExtra("date", date.toString())) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(q(lang,"Начать тренировку","Start workout","Rozpocznij trening","Почати тренування"), fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(Icons.Default.Restaurant, q(lang,"Питание","Nutrition","Odżywianie","Харчування"), "${targets.calories} kcal", Modifier.weight(1f)) {
                    context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", date.toString()))
                }
                QuickCard(Icons.Default.AutoStories, q(lang,"Фокус","Focus","Fokus","Фокус"), q(lang,"20 мин чтения","20 min reading","20 min czytania","20 хв читання"), Modifier.weight(1f)) { }
            }
        }
        item { SectionLabel(q(lang,"Протокол дня","Daily protocol","Protokół dnia","Протокол дня"), "02") }
        items(tasks, key = { it.id }) { task ->
            val done = task.done >= task.target
            Card(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable {
                    tasks = tasks.map { if (it.id == task.id) it.copy(done = if (done) 0 else it.target) else it }
                    repo.saveTasks(tasks)
                },
                colors = CardDefaults.cardColors(containerColor = if (done) Color(0xFF13251F) else NztSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = if (done) NztAccent else NztSurface2, shape = CircleShape) {
                        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                            Icon(if (done) Icons.Default.Check else Icons.Default.Add, null, tint = if (done) Color.Black else NztMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(task.category, color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (task.target > 1) Text("${task.done}/${task.target}", color = NztAccent, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = NztSurface2, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NztAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, tint = NztAccent)
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Black)
            Text(subtitle, color = NztMuted, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun NZT4Plan(repo: NZTRepository, lang: AppLanguage) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val week = remember(today) { (0..6).map { today.plusDays(it.toLong()) } }
    var selected by remember { mutableStateOf(today) }
    val plan = remember(selected) { StathamEngine.forDate(selected) }
    val fmt = remember { DateTimeFormatter.ofPattern("dd.MM") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { NZT4Header(null, lang, q(lang,"ПЛАН","PLAN","PLAN","ПЛАН")) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(week) { day ->
                    val active = day == selected
                    Surface(
                        modifier = Modifier.width(68.dp).clickable { selected = day },
                        color = if (active) NztAccent else NztSurface,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.dayOfWeek.name.take(3), color = if (active) Color.Black else NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(day.format(fmt), color = if (active) Color.Black else NztText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(plan.phase.name.uppercase(), color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(plan.session.title, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(plan.phase.focus, color = NztMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { context.startActivity(Intent(context, NZT4WorkoutActivity::class.java).putExtra("date", selected.toString())) }, modifier = Modifier.weight(1f)) { Text(q(lang,"Начать","Start","Start","Почати"), fontWeight = FontWeight.Bold) }
                        OutlinedButton(onClick = { context.startActivity(Intent(context, ProTrainingLibraryActivity::class.java).putExtra("date", selected.toString())) }, modifier = Modifier.weight(1f)) { Text(q(lang,"Заменить","Replace","Zamień","Замінити")) }
                    }
                }
            }
        }
        item { SectionLabel(q(lang,"Упражнения","Exercises","Ćwiczenia","Вправи"), "${plan.session.exercises.size}") }
        itemsIndexed(plan.session.exercises) { index, ex ->
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NZT4ExercisePhoto(ex.name, Modifier.size(88.dp), 16)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${index + 1}. ${ex.name}", fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${ex.sets} × ${ex.target}  •  ${ex.restSeconds}s", color = NztMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(ex.technique, color = NztMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, ProNutritionActivity::class.java).putExtra("date", selected.toString())) },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(54.dp)
            ) { Icon(Icons.Default.Restaurant, null); Spacer(Modifier.width(8.dp)); Text(q(lang,"Питание на этот день","Nutrition for this day","Odżywianie na ten dzień","Харчування на цей день")) }
        }
    }
}

@Composable
private fun NZT4Growth(repo: NZTRepository, lang: AppLanguage) {
    var section by remember { mutableStateOf(GrowthSection.BOOKS) }
    Column(Modifier.fillMaxSize()) {
        NZT4Header(null, lang, q(lang,"РОСТ","GROWTH","ROZWÓJ","РОЗВИТОК"))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            SegmentedButton(selected = section == GrowthSection.BOOKS, onClick = { section = GrowthSection.BOOKS }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(q(lang,"Книги","Books","Książki","Книги")) }
            SegmentedButton(selected = section == GrowthSection.IMPACT, onClick = { section = GrowthSection.IMPACT }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Impact Log") }
        }
        Spacer(Modifier.height(6.dp))
        when (section) {
            GrowthSection.BOOKS -> V3BooksScreen(lang, Modifier.weight(1f))
            GrowthSection.IMPACT -> ImpactScreen(repo, lang, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ImpactScreen(repo: NZTRepository, lang: AppLanguage, modifier: Modifier = Modifier) {
    var type by remember { mutableStateOf("Career") }
    var note by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val logs = remember(refresh) { repo.growthLogs() }
    val types = listOf("Career", "Influence", "Mind", "Money", "ServiceFlow")
    LazyColumn(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(q(lang,"Зафиксировать рост","Log an impact","Zapisz wpływ","Зафіксувати ріст"), fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(types) { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }) }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text(q(lang,"Что сделал и какой результат?","What did you do and what changed?","Co zrobiłeś i jaki był efekt?","Що зробив і який результат?")) })
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { if (note.isNotBlank()) { repo.addGrowth(type, note.trim()); note = ""; refresh++ } }, enabled = note.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(q(lang,"Добавить","Add","Dodaj","Додати")) }
                }
            }
        }
        item { Text(q(lang,"Последние действия","Recent impact","Ostatnie działania","Останні дії").uppercase(), color = NztMuted, fontSize = 11.sp, fontWeight = FontWeight.Black) }
        items(logs.take(20)) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(log.type, color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(log.note, fontWeight = FontWeight.Bold)
                    Text(log.date, color = NztMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun NZT4Progress(repo: NZTRepository, lang: AppLanguage) {
    val body = repo.bodyLogs()
    val growth = repo.growthLogs()
    val tasks = repo.tasksForToday()
    val score = repo.completionPercent(tasks)
    val currentWeight = body.firstOrNull()?.weight
    val currentWaist = body.firstOrNull()?.waist

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { NZT4Header(null, lang, q(lang,"ПРОГРЕСС","PROGRESS","POSTĘP","ПРОГРЕС")) }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(q(lang,"Индекс","Score","Indeks","Індекс"), "$score", "/100", NztAccent, Modifier.weight(1f))
                StatCard(q(lang,"День","Day","Dzień","День"), "${repo.dayNumber()}", "/365", NztAccent2, Modifier.weight(1f))
            }
        }
        item { SectionLabel(q(lang,"Тело","Body","Ciało","Тіло"), "BODY") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    ProgressDatum(q(lang,"Вес","Weight","Waga","Вага"), currentWeight?.let { "${it} kg" } ?: "—", Modifier.weight(1f))
                    ProgressDatum(q(lang,"Талия","Waist","Talia","Талія"), currentWaist?.let { "${it} cm" } ?: "—", Modifier.weight(1f))
                    ProgressDatum(q(lang,"Замеры","Checks","Pomiary","Заміри"), "${body.size}", Modifier.weight(1f))
                }
            }
        }
        item { SectionLabel(q(lang,"Развитие","Growth","Rozwój","Розвиток"), "MIND / CAREER") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = NztSurface2, shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.AutoGraph, null, tint = NztAccent, modifier = Modifier.padding(12.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Impact Log", fontWeight = FontWeight.Black, fontSize = 18.sp); Text(q(lang,"действий, которые двигают тебя вперёд","actions that move you forward","działań, które pchają Cię do przodu","дій, що рухають тебе вперед"), color = NztMuted, fontSize = 11.sp) }
                    Text("${growth.size}", color = NztAccent, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        item { SectionLabel(q(lang,"365-дневный принцип","365-day principle","Zasada 365 dni","Принцип 365 днів"), "NZT") }
        item {
            Surface(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), color = Color(0xFF13251F), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = NztAccent)
                    Spacer(Modifier.width(10.dp))
                    Text(q(lang,"Не идеальный день. Не нулевой день.","Not a perfect day. Never a zero day.","Nie idealny dzień. Nigdy dzień zerowy.","Не ідеальний день. Ніколи не нульовий день."), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, suffix: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp)) {
            Text(title.uppercase(), color = NztMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.Bottom) { Text(value, color = color, fontSize = 35.sp, fontWeight = FontWeight.Black); Text(suffix, color = NztMuted, modifier = Modifier.padding(bottom = 5.dp)) }
        }
    }
}

@Composable
private fun ProgressDatum(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) { Text(title.uppercase(), color = NztMuted, fontSize = 9.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(4.dp)); Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp) }
}

@Composable
private fun NZT4Profile(profile: ProfileStore, lang: AppLanguage, onLanguage: (AppLanguage) -> Unit, onProfileChanged: () -> Unit) {
    var name by remember { mutableStateOf(profile.name()) }
    var editName by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { NZT4Header(profile.name(), lang, "v4.0.0") }
        item { SectionLabel(q(lang,"Профиль","Profile","Profil","Профіль"), "LOCAL") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = NztAccent, shape = CircleShape) { Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Text(profile.name().take(1).uppercase(), color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Black) } }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(profile.name(), fontWeight = FontWeight.Black, fontSize = 19.sp); Text(q(lang,"Данные хранятся на этом телефоне","Data is stored on this phone","Dane są zapisane na tym telefonie","Дані зберігаються на цьому телефоні"), color = NztMuted, fontSize = 11.sp) }
                        IconButton(onClick = { editName = true }) { Icon(Icons.Default.Edit, null) }
                    }
                }
            }
        }
        item { SectionLabel(q(lang,"Язык","Language","Język","Мова"), "4") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(14.dp)) {
                    AppLanguage.entries.forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { onLanguage(item) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = item == lang, onClick = { onLanguage(item) })
                            Text(item.label, fontWeight = if (item == lang) FontWeight.Black else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        item {
            Surface(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), color = NztSurface2, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("NZT 365 • v4.0.0", color = NztAccent, fontWeight = FontWeight.Black)
                    Text(q(lang,"Профиль, тренировки, питание, книги и прогресс работают локально. Фото упражнений загружаются через интернет.","Profile, workouts, nutrition, books and progress work locally. Exercise photos load from the internet.","Profil, treningi, odżywianie, książki i postęp działają lokalnie. Zdjęcia ćwiczeń są pobierane z internetu.","Профіль, тренування, харчування, книги та прогрес працюють локально. Фото вправ завантажуються з інтернету."), color = NztMuted, fontSize = 11.sp)
                }
            }
        }
    }
    if (editName) {
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(q(lang,"Изменить имя","Edit name","Edytuj imię","Змінити ім'я")) },
            text = { OutlinedTextField(name, { if (it.length <= 28) name = it }, singleLine = true) },
            confirmButton = { Button(onClick = { if (name.isNotBlank()) { profile.setName(name.trim()); onProfileChanged(); editName = false } }) { Text(q(lang,"Сохранить","Save","Zapisz","Зберегти")) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(q(lang,"Отмена","Cancel","Anuluj","Скасувати")) } },
            containerColor = NztSurface
        )
    }
}
