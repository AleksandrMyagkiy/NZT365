package com.nzt365.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val BridgeAccent = Color(0xFFE9FF70)
private val BridgeCard = Color(0xFF15181D)
private val BridgeMuted = Color(0xFF9BA3AF)

private fun launch(context: Context, cls: Class<*>, date: LocalDate) {
    context.startActivity(Intent(context, cls).putExtra("date", date.toString()))
}

@Composable
fun StathamTodayCard() {
    val context = LocalContext.current
    val date = LocalDate.now()
    val plan = remember { StathamEngine.forDate(date) }
    val target = remember { StathamEngine.targets(plan) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BridgeCard), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("STATHAM FORM • СЕГОДНЯ", color = BridgeAccent, fontWeight = FontWeight.Bold)
            Text(plan.session.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${plan.phase.name} • неделя ${plan.week} • ${plan.session.minutes} мин", color = BridgeMuted)
            Text("Питание: ${target.calories} ккал • Б ${target.protein} • Ж ${target.fats} • У ${target.carbs}")
            Button(onClick = { launch(context, WorkoutActivity::class.java, date) }, modifier = Modifier.fillMaxWidth()) { Text("Начать тренировку") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launch(context, NutritionActivity::class.java, date) }, modifier = Modifier.weight(1f)) { Text("Питание") }
                OutlinedButton(onClick = { launch(context, TrainingLibraryActivity::class.java, date) }, modifier = Modifier.weight(1f)) { Text("Заменить") }
            }
        }
    }
}

@Composable
fun StathamBodyModule() {
    val context = LocalContext.current
    val date = LocalDate.now()
    val plan = remember { StathamEngine.forDate(date) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BridgeCard), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("STATHAM FORM 2.0", color = BridgeAccent, fontWeight = FontWeight.Bold)
            Text(plan.session.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${plan.phase.name} • неделя ${plan.week}${if (plan.deload) " • разгрузка" else ""}")
            Text(plan.phase.focus, color = BridgeMuted)
            Text("Картинки упражнений, активные подходы, таймер отдыха, вес/уровень, автопрогрессия и контроль нагрузки.", color = BridgeMuted)
            Button(onClick = { launch(context, WorkoutActivity::class.java, date) }, modifier = Modifier.fillMaxWidth()) { Text("Открыть полноценную тренировку") }
            OutlinedButton(onClick = { launch(context, TrainingLibraryActivity::class.java, date) }, modifier = Modifier.fillMaxWidth()) {
                Text("Турник • брусья • гантели • штанга • бег • своя")
            }
        }
    }
}

@Composable
fun StathamNutritionModule() {
    val context = LocalContext.current
    val date = LocalDate.now()
    val target = remember { StathamEngine.targets(StathamEngine.forDate(date)) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BridgeCard), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ПИТАНИЕ", color = BridgeAccent, fontWeight = FontWeight.Bold)
            Text("Цель: ${target.calories} ккал • Б ${target.protein} • Ж ${target.fats} • У ${target.carbs}")
            Text("Завтрак, обед, перекус и ужин. Добавление продуктов по граммам и БЖУ.", color = BridgeMuted)
            Button(onClick = { launch(context, NutritionActivity::class.java, date) }, modifier = Modifier.fillMaxWidth()) { Text("Открыть питание") }
        }
    }
}

@Composable
fun NZTHabitsModule() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nzt_habits", Context.MODE_PRIVATE) }
    val date = LocalDate.now().toString()
    val habits = listOf(
        "sleep" to "Сон 7+ часов",
        "water" to "Вода",
        "training" to "Тренировка / активность",
        "reading" to "Чтение",
        "polish" to "Польский",
        "career" to "Карьерное действие"
    )
    var refresh by remember { mutableIntStateOf(0) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BridgeCard), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("ПРИВЫЧКИ", color = BridgeAccent, fontWeight = FontWeight.Bold)
            habits.forEach { (key, title) ->
                val prefKey = "${date}_$key"
                val checked = prefs.getBoolean(prefKey, false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, modifier = Modifier.weight(1f))
                    Checkbox(checked = checked, onCheckedChange = {
                        prefs.edit().putBoolean(prefKey, it).apply()
                        refresh++
                    })
                }
            }
        }
    }
}
