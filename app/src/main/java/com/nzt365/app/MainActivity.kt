package com.nzt365.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = NZTRepository(this)

        scheduleReminder()

        setContent {
            NZT365Theme {
                App(repo)
            }
        }
    }

    private fun scheduleReminder() {
        val request =
            PeriodicWorkRequestBuilder<ReminderWorker>(
                24,
                TimeUnit.HOURS
            ).build()

        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                "nzt_daily",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }
}

private val Bg = Color(0xFF0B0D10)
private val AppCard = Color(0xFF15181D)
private val Accent = Color(0xFFE9FF70)
private val Muted = Color(0xFF9BA3AF)

@Composable
fun NZT365Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Bg,
            surface = AppCard,
            onPrimary = Color.Black
        ),
        content = content
    )
}

enum class Tab(val title: String) {
    Today("Сегодня"),
    Body("Тело"),
    Growth("Рост"),
    Progress("Прогресс"),
    Settings("Настройки")
}

@Composable
fun App(repo: NZTRepository) {

    var tab by remember {
        mutableStateOf(Tab.Today)
    }

    Scaffold(
        containerColor = Bg,

        bottomBar = {

            NavigationBar(
                containerColor = Color(0xFF101318)
            ) {

                NavItem(
                    current = tab,
                    target = Tab.Today,
                    icon = Icons.Default.Home
                ) {
                    tab = it
                }

                NavItem(
                    current = tab,
                    target = Tab.Body,
                    icon = Icons.Default.FitnessCenter
                ) {
                    tab = it
                }

                NavItem(
                    current = tab,
                    target = Tab.Growth,
                    icon = Icons.Default.TrendingUp
                ) {
                    tab = it
                }

                NavItem(
                    current = tab,
                    target = Tab.Progress,
                    icon = Icons.Default.BarChart
                ) {
                    tab = it
                }

                NavItem(
                    current = tab,
                    target = Tab.Settings,
                    icon = Icons.Default.Settings
                ) {
                    tab = it
                }
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Bg)
        ) {

            when (tab) {

                Tab.Today -> TodayScreen(repo)

                Tab.Body -> BodyScreen(repo)

                Tab.Growth -> GrowthScreen(repo)

                Tab.Progress -> ProgressScreen(repo)

                Tab.Settings -> SettingsScreen(repo)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    current: Tab,
    target: Tab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (Tab) -> Unit
) {

    NavigationBarItem(
        selected = current == target,

        onClick = {
            onClick(target)
        },

        icon = {
            Icon(
                imageVector = icon,
                contentDescription = target.title
            )
        },

        label = {
            Text(
                text = target.title,
                fontSize = 10.sp
            )
        }
    )
}

@Composable
fun Header(
    repo: NZTRepository,
    subtitle: String = ""
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 8.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "NZT 365",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text =
                        "DAY ${repo.dayNumber()} / 365",
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
            }

            if (subtitle.isNotBlank()) {

                Text(
                    text = subtitle,
                    color = Muted
                )
            }
        }
    }
}

@Composable
fun TodayScreen(
    repo: NZTRepository
) {

    var tasks by remember {
        mutableStateOf(
            repo.tasksForToday()
        )
    }

    val score =
        repo.completionPercent(tasks)

    val nutrition =
        repo.nutrition()

    val targets =
        repo.targets()

    Column {

        Header(
            repo = repo,
            subtitle =
                LocalDate.now().toString()
        )

        LazyColumn(
            contentPadding =
                PaddingValues(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                ScoreCard(score)
            }

            item {

                SectionTitle("TODAY")
            }

            items(
                items = tasks,
                key = { it.id }
            ) { task ->

                TaskCard(
                    task = task
                ) { delta ->

                    tasks =
                        tasks.map {

                            if (it.id == task.id) {

                                it.copy(
                                    done =
                                        (it.done + delta)
                                            .coerceIn(
                                                0,
                                                it.target
                                            )
                                )

                            } else {

                                it
                            }
                        }

                    repo.saveTasks(tasks)
                }
            }

            item {

                NutritionCard(
                    cal = nutrition.first,
                    protein =
                        nutrition.second,
                    targetCal =
                        targets.first,
                    targetProtein =
                        targets.second,
                    repo = repo
                )
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        "Правило дня: не заканчивать день с нулём.",
                    color = Muted
                )
            }
        }
    }
}

@Composable
fun ScoreCard(
    score: Int
) {

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = AppCard
            ),

        shape =
            RoundedCornerShape(22.dp),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = "NZT SCORE",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = score.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color =
                    if (score >= 80)
                        Accent
                    else
                        Color.White
            )

            LinearProgressIndicator(
                progress = {
                    score / 100f
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp),

                color = Accent,

                trackColor =
                    Color(0xFF2A2F36)
            )
        }
    }
}

@Composable
fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        color = Muted,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
}

@Composable
fun TaskCard(
    task: DailyTask,
    onChange: (Int) -> Unit
) {

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = AppCard
            ),

        shape =
            RoundedCornerShape(18.dp),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = task.category,
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        "${task.done} / ${task.target}",
                    color = Muted
                )
            }

            IconButton(
                onClick = {
                    onChange(-1)
                },

                enabled =
                    task.done > 0
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Remove,

                    contentDescription =
                        "Уменьшить"
                )
            }

            FilledIconButton(
                onClick = {
                    onChange(1)
                },

                enabled =
                    task.done <
                        task.target,

                colors =
                    IconButtonDefaults
                        .filledIconButtonColors(
                            containerColor =
                                Accent,

                            contentColor =
                                Color.Black
                        )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Добавить"
                )
            }
        }
    }
}

@Composable
fun NutritionCard(
    cal: Int,
    protein: Int,
    targetCal: Int,
    targetProtein: Int,
    repo: NZTRepository
) {

    var calText by remember {
        mutableStateOf(
            cal.toString()
        )
    }

    var proteinText by remember {
        mutableStateOf(
            protein.toString()
        )
    }

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    AppCard
            ),

        shape =
            RoundedCornerShape(18.dp),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = "NUTRITION",
                color = Accent,
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Калории и белок",
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement
                        .spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = calText,

                    onValueChange = {
                        calText =
                            it.filter(
                                Char::isDigit
                            )
                    },

                    label = {
                        Text(
                            "ккал / $targetCal"
                        )
                    },

                    modifier =
                        Modifier.weight(1f),

                    singleLine = true
                )

                OutlinedTextField(
                    value =
                        proteinText,

                    onValueChange = {
                        proteinText =
                            it.filter(
                                Char::isDigit
                            )
                    },

                    label = {
                        Text(
                            "белок / $targetProtein г"
                        )
                    },

                    modifier =
                        Modifier.weight(1f),

                    singleLine = true
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Button(
                onClick = {

                    repo.setNutrition(
                        calText
                            .toIntOrNull()
                            ?: 0,

                        proteinText
                            .toIntOrNull()
                            ?: 0
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Сохранить")
            }
        }
    }
}

@Composable
fun BodyScreen(
    repo: NZTRepository
) {

    var weight by remember {
        mutableStateOf("")
    }

    var waist by remember {
        mutableStateOf("")
    }

    var chest by remember {
        mutableStateOf("")
    }

    var arm by remember {
        mutableStateOf("")
    }

    var thigh by remember {
        mutableStateOf("")
    }

    var refresh by remember {
        mutableIntStateOf(0)
    }

    val logs =
        remember(refresh) {
            repo.bodyLogs()
        }

    Column {

        Header(repo)

        LazyColumn(
            contentPadding =
                PaddingValues(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                SectionTitle(
                    "CHECKPOINT"
                )
            }

            item {

                Card(
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    AppCard
                            ),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    ) {

                        Text(
                            text =
                                "Измерения тела",
                            fontSize =
                                20.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        NumField(
                            "Вес, кг",
                            weight
                        ) {
                            weight = it
                        }

                        NumField(
                            "Талия, см",
                            waist
                        ) {
                            waist = it
                        }

                        NumField(
                            "Грудь, см",
                            chest
                        ) {
                            chest = it
                        }

                        NumField(
                            "Рука, см",
                            arm
                        ) {
                            arm = it
                        }

                        NumField(
                            "Бедро, см",
                            thigh
                        ) {
                            thigh = it
                        }

                        Button(
                            onClick = {

                                repo.saveBody(
                                    BodyLog(
                                        date =
                                            LocalDate
                                                .now()
                                                .toString(),

                                        weight =
                                            weight
                                                .toDoubleOrNull(),

                                        waist =
                                            waist
                                                .toDoubleOrNull(),

                                        chest =
                                            chest
                                                .toDoubleOrNull(),

                                        arm =
                                            arm
                                                .toDoubleOrNull(),

                                        thigh =
                                            thigh
                                                .toDoubleOrNull()
                                    )
                                )

                                refresh++
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Text(
                                "Сохранить измерения"
                            )
                        }
                    }
                }
            }

            item {

                SectionTitle(
                    "ИСТОРИЯ"
                )
            }

            items(logs) { log ->

                Card(
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    AppCard
                            )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                14.dp
                            )
                    ) {

                        Text(
                            text =
                                log.date,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Вес ${log.weight ?: "—"} • талия ${log.waist ?: "—"} • грудь ${log.chest ?: "—"} • рука ${log.arm ?: "—"} • бедро ${log.thigh ?: "—"}",

                            color =
                                Muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NumField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {

    OutlinedTextField(
        value = value,

        onValueChange = {

            onChange(
                it
                    .replace(',', '.')
                    .filter { char ->
                        char.isDigit() ||
                            char == '.'
                    }
            )
        },

        label = {
            Text(label)
        },

        modifier =
            Modifier.fillMaxWidth(),

        singleLine = true
    )
}

@Composable
fun GrowthScreen(
    repo: NZTRepository
) {

    var type by remember {
        mutableStateOf(
            "Переговоры"
        )
    }

    var note by remember {
        mutableStateOf("")
    }

    var refresh by remember {
        mutableIntStateOf(0)
    }

    val logs =
        remember(refresh) {
            repo.growthLogs()
        }

    val types =
        listOf(
            "Переговоры",
            "Книга / идея",
            "Карьера",
            "ServiceFlow",
            "Доп. доход"
        )

    Column {

        Header(repo)

        LazyColumn(
            contentPadding =
                PaddingValues(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            item {
    BooksSection()
}
            item {

                SectionTitle(
                    "IMPACT LOG"
                )
            }

            item {

                Card(
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    AppCard
                            ),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    ) {

                        Text(
                            text =
                                "Записать действие",

                            fontSize =
                                20.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement
                                    .spacedBy(6.dp),

                            modifier =
                                Modifier.padding(
                                    vertical =
                                        8.dp
                                )
                        ) {

                            types
                                .take(3)
                                .forEach {

                                    FilterChip(
                                        selected =
                                            type ==
                                                it,

                                        onClick = {
                                            type = it
                                        },

                                        label = {
                                            Text(it)
                                        }
                                    )
                                }
                        }

                        Row(
                            horizontalArrangement =
                                Arrangement
                                    .spacedBy(6.dp)
                        ) {

                            types
                                .drop(3)
                                .forEach {

                                    FilterChip(
                                        selected =
                                            type ==
                                                it,

                                        onClick = {
                                            type = it
                                        },

                                        label = {
                                            Text(it)
                                        }
                                    )
                                }
                        }

                        OutlinedTextField(
                            value = note,

                            onValueChange = {
                                note = it
                            },

                            label = {
                                Text(
                                    "Что сделал / что понял"
                                )
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top =
                                            8.dp
                                    ),

                            minLines = 3
                        )

                        Button(
                            onClick = {

                                if (
                                    note
                                        .isNotBlank()
                                ) {

                                    repo.addGrowth(
                                        type,
                                        note.trim()
                                    )

                                    note = ""

                                    refresh++
                                }
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top =
                                            8.dp
                                    )
                        ) {

                            Text("Добавить")
                        }
                    }
                }
            }

            item {

                SectionTitle(
                    "ПОСЛЕДНИЕ ДЕЙСТВИЯ"
                )
            }

            items(
                logs.take(30)
            ) { log ->

                Card(
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    AppCard
                            )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                14.dp
                            )
                    ) {

                        Text(
                            text =
                                log.type,

                            color =
                                Accent,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                log.note
                        )

                        Text(
                            text =
                                log.date,

                            color =
                                Muted,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressScreen(
    repo: NZTRepository
) {

    val body =
        repo.bodyLogs()

    val growth =
        repo.growthLogs()

    val money =
        repo.moneyLogs()

    val score =
        repo.completionPercent(
            repo.tasksForToday()
        )

    Column {

        Header(repo)

        LazyColumn(
            contentPadding =
                PaddingValues(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {
                ScoreCard(score)
            }

            item {

                Metric(
                    "День проекта",
                    repo.dayNumber()
                        .toString(),
                    "из 365"
                )
            }

            item {

                Metric(
                    "Контрольных измерений",
                    body.size
                        .toString(),
                    "BODY"
                )
            }

            item {

                Metric(
                    "Действий развития",
                    growth.size
                        .toString(),
                    "MIND / CAREER / INFLUENCE"
                )
            }

            item {

                Metric(
                    "Финансовых снимков",
                    money.size
                        .toString(),
                    "MONEY"
                )
            }

            if (
                repo.dayNumber() %
                    30 in 0..2
            ) {

                item {

                    Card(
                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        Color(
                                            0xFF222A18
                                        )
                                )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .padding(
                                        18.dp
                                    )
                        ) {

                            Text(
                                text =
                                    "30-DAY CHECKPOINT",

                                color =
                                    Accent,

                                fontWeight =
                                    FontWeight.Black
                            )

                            Text(
                                "Сделай фото спереди/сбоку/сзади, измерения тела, оцени карьеру, деньги и дисциплину."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Metric(
    label: String,
    value: String,
    sub: String
) {

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    AppCard
            ),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.padding(18.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = label,
                    color = Muted
                )

                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight =
                        FontWeight.Black
                )
            }

            Text(
                text = sub,
                color = Accent,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsScreen(
    repo: NZTRepository
) {

    val targets =
        repo.targets()

    var cal by remember {
        mutableStateOf(
            targets.first.toString()
        )
    }

    var protein by remember {
        mutableStateOf(
            targets.second.toString()
        )
    }

    var mainIncome by remember {
        mutableStateOf("")
    }

    var sideIncome by remember {
        mutableStateOf("")
    }

    var capital by remember {
        mutableStateOf("")
    }

    var expenses by remember {
        mutableStateOf("")
    }

    var saved by remember {
        mutableStateOf(false)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) {
        }

    Column {

        Header(repo)

        LazyColumn(
            contentPadding =
                PaddingValues(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                SectionTitle(
                    "ПИТАНИЕ"
                )
            }

            item {

                Card(
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    AppCard
                            )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    ) {

                        NumField(
                            "Цель ккал",
                            cal
                        ) {
                            cal = it
                        }

                        NumField(
                            "Цель белка, г",
                            protein
                        ) {
                            protein = it
                        }

                        Button(
                            onClick = {

                                repo.setTargets(
                                    cal.toIntOrNull()
                                        ?: 2200,

                                    protein
                                        .toIntOrNull()
                                        ?: 140
                                )

                                saved = true
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Text(
                                if (saved)
                                    "Сохранено"
                                else
                                    "Сохранить цели"
                            )
                        }
                    }
                }
            }

            item {

                SectionTitle(
                    "MONEY SNAPSHOT"
                )
            }

            item {

                Card(
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    AppCard
                            )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            )
                    ) {

                        NumField(
                            "Основной доход",
                            mainIncome
                        ) {
                            mainIncome = it
                        }

                        NumField(
                            "Доп. доход",
                            sideIncome
                        ) {
                            sideIncome = it
                        }

                        NumField(
                            "Капитал",
                            capital
                        ) {
                            capital = it
                        }

                        NumField(
                            "Расходы",
                            expenses
                        ) {
                            expenses = it
                        }

                        Button(
                            onClick = {

                                repo.saveMoney(
                                    MoneyLog(
                                        date =
                                            LocalDate
                                                .now()
                                                .toString(),

                                        mainIncome =
                                            mainIncome
                                                .toDoubleOrNull()
                                                ?: 0.0,

                                        sideIncome =
                                            sideIncome
                                                .toDoubleOrNull()
                                                ?: 0.0,

                                        capital =
                                            capital
                                                .toDoubleOrNull()
                                                ?: 0.0,

                                        expenses =
                                            expenses
                                                .toDoubleOrNull()
                                                ?: 0.0
                                    )
                                )
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Text(
                                "Сохранить финансовый снимок"
                            )
                        }
                    }
                }
            }

            item {

                SectionTitle(
                    "УВЕДОМЛЕНИЯ"
                )
            }

            item {

                Button(
                    onClick = {

                        if (
                            Build.VERSION
                                .SDK_INT >= 33
                        ) {

                            permissionLauncher
                                .launch(
                                    Manifest.permission
                                        .POST_NOTIFICATIONS
                                )
                        }
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Icon(
                        imageVector =
                            Icons.Default
                                .Notifications,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.padding(
                                end = 8.dp
                            )
                    )

                    Text(
                        "Разрешить уведомления"
                    )
                }
            }

            item {

                Text(
                    text =
                        "Данные хранятся локально на телефоне.",
                    color =
                        Muted
                )
            }
        }
    }
}
