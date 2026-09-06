package com.nzt365.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

private val SfBg = Color(0xFF07101A)
private val SfCard = Color(0xFF101B27)
private val SfLine = Color(0xFF263544)
private val SfAccent = Color(0xFFE9FF70)
private val SfMuted = Color(0xFF9BA3AF)

private data class FoodEntry(
    val id: String,
    val meal: String,
    val name: String,
    val grams: Double,
    val kcal100: Double,
    val protein100: Double,
    val fats100: Double,
    val carbs100: Double
) {
    val factor get() = grams / 100.0
    val kcal get() = kcal100 * factor
    val protein get() = protein100 * factor
    val fats get() = fats100 * factor
    val carbs get() = carbs100 * factor
}

private class FullStore(context: android.content.Context) {
    private val p = context.getSharedPreferences("nzt_statham_full_v130", MODE_PRIVATE)

    fun doneSets(date: LocalDate, code: String, i: Int) = p.getInt("done_${date}_${code}_$i", 0)
    fun setDoneSets(date: LocalDate, code: String, i: Int, v: Int) = p.edit().putInt("done_${date}_${code}_$i", v).apply()
    fun prescribedSets(name: String, base: Int) = p.getInt("sets_${name.hashCode()}", base)
    fun setPrescribedSets(name: String, v: Int) = p.edit().putInt("sets_${name.hashCode()}", v.coerceIn(1, 6)).apply()
    fun load(name: String) = p.getFloat("load_${name.hashCode()}", 0f).toDouble()
    fun setLoad(name: String, v: Double) = p.edit().putFloat("load_${name.hashCode()}", v.coerceAtLeast(0.0).toFloat()).apply()
    fun level(name: String) = p.getInt("level_${name.hashCode()}", 1)
    fun setLevel(name: String, v: Int) = p.edit().putInt("level_${name.hashCode()}", v.coerceIn(1, 5)).apply()
    fun successes(name: String) = p.getInt("ok_${name.hashCode()}", 0)
    fun setSuccesses(name: String, v: Int) = p.edit().putInt("ok_${name.hashCode()}", v.coerceAtLeast(0)).apply()
    fun replacement(date: LocalDate) = p.getString("replacement_$date", null)
    fun setReplacement(date: LocalDate, code: String?) = p.edit().apply { if (code == null) remove("replacement_$date") else putString("replacement_$date", code) }.apply()
    fun reviewInterval() = p.getInt("review_interval", 14)
    fun setReviewInterval(v: Int) = p.edit().putInt("review_interval", if (v <= 7) 7 else 14).apply()
    fun lastReview() = p.getLong("last_review", StathamEngine.START.toEpochDay())
    fun markReview(date: LocalDate) = p.edit().putLong("last_review", date.toEpochDay()).apply()
    fun reviewDue(date: LocalDate) = date.toEpochDay() - lastReview() >= reviewInterval()

    fun customSession(): StathamSession? {
        val raw = p.getString("custom_session", null) ?: return null
        return try {
            val root = JSONObject(raw)
            val ex = root.getJSONArray("ex")
            val items = buildList {
                for (i in 0 until ex.length()) {
                    val o = ex.getJSONObject(i)
                    add(StathamExercise(o.getString("name"), o.optInt("sets", 3), o.optString("target", "8–12"), o.optInt("rest", 60), o.optString("technique", "Контролируй технику и не работай через боль."), o.optString("progression", "Верх диапазона дважды → усложни нагрузку.")))
                }
            }
            StathamSession("CUSTOM", root.optString("title", "Своя тренировка"), root.optString("type", "Силовая"), root.optInt("minutes", 45), items)
        } catch (_: Exception) { null }
    }

    fun saveCustom(title: String, type: String, minutes: Int, rows: String) {
        val ex = JSONArray()
        rows.lines().filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                ex.put(JSONObject().put("name", parts[0]).put("sets", parts.getOrNull(1)?.toIntOrNull() ?: 3).put("target", parts.getOrNull(2) ?: "8–12").put("rest", parts.getOrNull(3)?.toIntOrNull() ?: 60).put("technique", parts.getOrNull(4) ?: "Работай подконтрольно.").put("progression", "Верх диапазона дважды с запасом → усложни."))
            }
        }
        val root = JSONObject().put("title", title.ifBlank { "Своя тренировка" }).put("type", type.ifBlank { "Силовая" }).put("minutes", minutes.coerceAtLeast(10)).put("ex", ex)
        p.edit().putString("custom_session", root.toString()).apply()
    }

    fun food(date: LocalDate): List<FoodEntry> {
        val raw = p.getString("food_$date", "[]") ?: "[]"
        return try {
            val a = JSONArray(raw)
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(FoodEntry(o.getString("id"), o.getString("meal"), o.getString("name"), o.getDouble("grams"), o.getDouble("kcal"), o.optDouble("protein"), o.optDouble("fats"), o.optDouble("carbs")))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun addFood(date: LocalDate, item: FoodEntry) {
        val all = food(date).toMutableList().apply { add(item) }
        saveFood(date, all)
    }
    fun removeFood(date: LocalDate, id: String) = saveFood(date, food(date).filterNot { it.id == id })
    private fun saveFood(date: LocalDate, all: List<FoodEntry>) {
        val a = JSONArray()
        all.forEach { e -> a.put(JSONObject().put("id", e.id).put("meal", e.meal).put("name", e.name).put("grams", e.grams).put("kcal", e.kcal100).put("protein", e.protein100).put("fats", e.fats100).put("carbs", e.carbs100)) }
        p.edit().putString("food_$date", a.toString()).apply()
    }
}

private fun librarySessions(): List<StathamSession> = listOf(
    StathamSession("INDOOR", "Кардио дома — без велосипеда", "Кардио", 42, listOf(
        StathamExercise("Разминка всего тела",1,"7 мин",0,"Суставная разминка и лёгкий шаг.",""),
        StathamExercise("Шаги с высоким коленом",6,"2 мин",60,"RPE 5–6, корпус высокий.","Добавь интервал, когда все шесть ровные."),
        StathamExercise("Альпинист у высокой опоры",6,"30 с",45,"Таз стабилен, без удара стопой.","Снизь высоту опоры или +5 секунд."),
        StathamExercise("Заминка и дыхание",1,"6 мин",0,"Постепенно снизь пульс.","")
    )),
    StathamSession("PULL-DIPS", "Турник и брусья — сила верха", "Силовая", 52, listOf(
        StathamExercise("Подтягивания",4,"4–8",120,"Без раскачки, шея нейтральна.","8×4 дважды → +1–2 кг."),
        StathamExercise("Отжимания на брусьях",4,"5–10",120,"Не проваливай плечи ниже комфортной глубины.","10×4 дважды → +1–2 кг."),
        StathamExercise("Австралийские подтягивания",3,"8–15",90,"Корпус прямой.","15×3 → усложни угол."),
        StathamExercise("Подъём коленей в висе",3,"8–15",75,"Не раскачивайся.","15×3 → выпрямляй ноги.")
    )),
    StathamSession("DUMBBELL", "Гантели — всё тело", "Силовая", 58, listOf(
        StathamExercise("Гоблет-присед с гантелью",4,"8–12",90,"Стопа полностью на полу.","12×4 дважды → +2–5% веса."),
        StathamExercise("Жим гантелей лёжа",4,"8–12",90,"Лопатки сведены.","12×4 дважды → +2–5% веса."),
        StathamExercise("Тяга гантели одной рукой",4,"8–12",90,"Спина нейтральна.","12×4 дважды → +2–5% веса."),
        StathamExercise("Румынская тяга с гантелями",3,"8–12",105,"Таз назад, спина нейтральна.","12×3 дважды → +2–5% веса."),
        StathamExercise("Жим гантелей стоя",3,"8–12",90,"Не переразгибай поясницу.","12×3 → +1–2 кг."),
        StathamExercise("Ролик для пресса",3,"6–12",90,"Поясница не провисает.","12×3 → длиннее выкат.")
    )),
    StathamSession("BARBELL", "Штанга — атлетическая база", "Силовая", 68, listOf(
        StathamExercise("Присед со штангой",4,"5–8",150,"Начни с 3 повторов в запасе.","8×4 дважды → +2–5% веса."),
        StathamExercise("Жим штанги лёжа",4,"5–8",150,"Используй страховку.","8×4 дважды → +2–5% веса."),
        StathamExercise("Тяга штанги в наклоне",4,"6–10",120,"Спина нейтральна.","10×4 дважды → +2–5% веса."),
        StathamExercise("Румынская тяга со штангой",3,"6–10",150,"Гриф близко к ногам.","10×3 дважды → +2–5% веса."),
        StathamExercise("Жим штанги стоя",3,"6–10",120,"Корпус жёсткий.","10×3 → +1–2,5 кг.")
    )),
    StathamSession("RUN", "Бег — лёгкая выносливость", "Бег", 45, listOf(
        StathamExercise("Разминка ходьбой",1,"8–10 мин",0,"Начни с быстрой ходьбы.",""),
        StathamExercise("Лёгкий бег или бег/ходьба",1,"25–35 мин",0,"RPE 3–4: можешь говорить предложениями.","+3–5 минут, максимум 10% в неделю."),
        StathamExercise("Заминка ходьбой",1,"5–8 мин",0,"Снизь темп постепенно.","")
    ))
)

private fun imageFor(name: String): Int {
    val n = name.lowercase()
    return when {
        "подтяг" in n || "тяга" in n || "face" in n -> R.drawable.ex_pull
        "присед" in n || "выпад" in n || "шаг" in n || "икр" in n -> R.drawable.ex_squat
        "румын" in n || "мост" in n || "good morning" in n -> R.drawable.ex_hinge
        "бег" in n || "ходь" in n || "вел" in n || "кардио" in n || "размин" in n -> R.drawable.ex_cardio
        "ролик" in n || "планк" in n || "dead" in n || "bird" in n || "пресс" in n -> R.drawable.ex_core
        "сгиб" in n || "разгиб" in n || "бицеп" in n || "трицеп" in n -> R.drawable.ex_arms
        "дых" in n || "подбород" in n || "поворот" in n || "растяж" in n -> R.drawable.ex_mobility
        else -> R.drawable.ex_push
    }
}

private fun FullStore.sessionFor(date: LocalDate): StathamSession {
    val code = replacement(date)
    if (code == "CUSTOM") return customSession() ?: StathamEngine.forDate(date).session
    return librarySessions().firstOrNull { it.code == code } ?: StathamEngine.forDate(date).session
}

class WorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        setContent { NZT365Theme { WorkoutFullScreen(date) { finish() } } }
    }
}

@Composable
private fun WorkoutFullScreen(date: LocalDate, close: () -> Unit) {
    val context = LocalContext.current
    val store = remember { FullStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val session = remember(refresh) { store.sessionFor(date) }
    var restLeft by remember { mutableIntStateOf(0) }
    var reviewOpen by remember { mutableStateOf(false) }

    LaunchedEffect(restLeft) {
        if (restLeft > 0) { delay(1000); restLeft-- }
    }

    Scaffold(containerColor = SfBg, topBar = {
        Surface(color = SfBg) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("STATHAM FORM • ТРЕНИРОВКА", color = SfAccent, fontWeight = FontWeight.Bold)
                    Text(session.title, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = close) { Text("Закрыть") }
            }
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SfCard)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${session.type} • ${session.minutes} мин • ${session.exercises.size} упражнений", color = SfMuted)
                        if (restLeft > 0) Text("ОТДЫХ: ${restLeft} с", color = SfAccent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        if (store.reviewDue(date)) Button(onClick = { reviewOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Контроль нагрузки — пора проверить") }
                    }
                }
            }
            itemsIndexed(session.exercises) { index, ex ->
                val sets = store.prescribedSets(ex.name, ex.sets)
                val done = store.doneSets(date, session.code, index).coerceAtMost(sets)
                val load = store.load(ex.name)
                val level = store.level(ex.name)
                Card(colors = CardDefaults.cardColors(containerColor = SfCard), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(imageFor(ex.name)), ex.name, modifier = Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit)
                        Text("${index + 1}. ${ex.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$sets подхода • ${ex.target} • отдых ${ex.restSeconds} с")
                        Text(ex.technique, color = SfMuted)
                        if (ex.progression.isNotBlank()) Text("Прогрессия: ${ex.progression}", color = SfAccent)
                        Text("Нагрузка: ${if (load > 0) "${String.format("%.1f", load)} кг" else "уровень $level"}", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { store.setPrescribedSets(ex.name, sets - 1); refresh++ }, enabled = sets > 1, modifier = Modifier.weight(1f)) { Text("− подход") }
                            OutlinedButton(onClick = { store.setPrescribedSets(ex.name, sets + 1); refresh++ }, enabled = sets < 6, modifier = Modifier.weight(1f)) { Text("+ подход") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { store.setLoad(ex.name, load - 0.5); refresh++ }, modifier = Modifier.weight(1f)) { Text("−0,5 кг") }
                            OutlinedButton(onClick = { store.setLoad(ex.name, load + 0.5); refresh++ }, modifier = Modifier.weight(1f)) { Text("+0,5 кг") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { store.setLevel(ex.name, level - 1); refresh++ }, enabled = level > 1, modifier = Modifier.weight(1f)) { Text("Уровень −") }
                            OutlinedButton(onClick = { store.setLevel(ex.name, level + 1); refresh++ }, enabled = level < 5, modifier = Modifier.weight(1f)) { Text("Уровень +") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(sets) { s ->
                                val checked = s < done
                                FilledTonalButton(onClick = {
                                    val newDone = if (checked) s else (s + 1).coerceAtMost(sets)
                                    store.setDoneSets(date, session.code, index, newDone)
                                    if (!checked && ex.restSeconds > 0) restLeft = ex.restSeconds
                                    refresh++
                                }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) { Text(if (checked) "✓${s + 1}" else "${s + 1}") }
                            }
                        }
                    }
                }
            }
            item {
                Button(onClick = {
                    session.exercises.forEachIndexed { i, ex ->
                        val sets = store.prescribedSets(ex.name, ex.sets)
                        if (store.doneSets(date, session.code, i) >= sets) {
                            val ok = store.successes(ex.name) + 1
                            if (ok >= 2) {
                                if (store.load(ex.name) > 0) store.setLoad(ex.name, store.load(ex.name) * 1.025 + 0.25)
                                else store.setLevel(ex.name, store.level(ex.name) + 1)
                                store.setSuccesses(ex.name, 0)
                            } else store.setSuccesses(ex.name, ok)
                        }
                    }
                    close()
                }, modifier = Modifier.fillMaxWidth()) { Text("Завершить тренировку") }
            }
        }
    }

    if (reviewOpen) {
        AlertDialog(onDismissRequest = { reviewOpen = false }, title = { Text("Контроль нагрузки") }, text = { Text("Как ощущалась последняя неделя? Выбор автоматически скорректирует нагрузку. Проверка повторяется каждые ${store.reviewInterval()} дней.") }, confirmButton = {
            Column {
                TextButton(onClick = {
                    session.exercises.forEach { ex -> store.setPrescribedSets(ex.name, store.prescribedSets(ex.name, ex.sets) + 1) }
                    store.markReview(date); reviewOpen = false; refresh++
                }) { Text("Слишком легко → +1 подход") }
                TextButton(onClick = { store.markReview(date); reviewOpen = false }) { Text("Нормально → оставить") }
                TextButton(onClick = {
                    session.exercises.forEach { ex -> store.setPrescribedSets(ex.name, store.prescribedSets(ex.name, ex.sets) - 1) }
                    store.markReview(date); reviewOpen = false; refresh++
                }) { Text("Тяжело → −1 подход") }
            }
        }, dismissButton = { TextButton(onClick = { reviewOpen = false }) { Text("Позже") } })
    }
}

class TrainingLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        setContent { NZT365Theme { LibraryScreen(date) { finish() } } }
    }
}

@Composable
private fun LibraryScreen(date: LocalDate, close: () -> Unit) {
    val context = LocalContext.current
    val store = remember { FullStore(context) }
    var customOpen by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val base = StathamEngine.forDate(date).session
    val current = remember(refresh) { store.sessionFor(date) }

    Scaffold(containerColor = SfBg, topBar = { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("STATHAM FORM", color = SfAccent, fontWeight = FontWeight.Bold); Text("Тренировки и замены", style = MaterialTheme.typography.titleLarge) }; TextButton(onClick = close) { Text("Готово") } } }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Сейчас: ${current.title}", fontWeight = FontWeight.Bold); Text("Замена действует только на $date", color = SfMuted) }
            item {
                SessionChoice(base, "Исходный план") { store.setReplacement(date, null); refresh++ }
            }
            itemsIndexed(librarySessions()) { _, s -> SessionChoice(s, s.type) { store.setReplacement(date, s.code); refresh++ } }
            item {
                val custom = store.customSession()
                if (custom != null) SessionChoice(custom, "Моя тренировка") { store.setReplacement(date, "CUSTOM"); refresh++ }
                Button(onClick = { customOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Создать / изменить свою тренировку") }
            }
        }
    }

    if (customOpen) CustomWorkoutDialog(store, onClose = { customOpen = false }, onSaved = { customOpen = false; refresh++ })
}

@Composable
private fun SessionChoice(s: StathamSession, badge: String, choose: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SfCard)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(badge.uppercase(), color = SfAccent, style = MaterialTheme.typography.labelSmall)
            Text(s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${s.type} • ${s.minutes} мин • ${s.exercises.size} упражнений", color = SfMuted)
            Text(s.exercises.take(4).joinToString("\n") { "• ${it.name} — ${it.sets}×${it.target}" }, color = SfMuted)
            Button(onClick = choose, modifier = Modifier.fillMaxWidth()) { Text("Поставить на этот день") }
        }
    }
}

@Composable
private fun CustomWorkoutDialog(store: FullStore, onClose: () -> Unit, onSaved: () -> Unit) {
    var title by remember { mutableStateOf(store.customSession()?.title ?: "") }
    var type by remember { mutableStateOf(store.customSession()?.type ?: "Силовая") }
    var minutes by remember { mutableStateOf((store.customSession()?.minutes ?: 45).toString()) }
    var rows by remember { mutableStateOf(store.customSession()?.exercises?.joinToString("\n") { "${it.name} | ${it.sets} | ${it.target} | ${it.restSeconds} | ${it.technique}" } ?: "") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Своя тренировка") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Название") })
            OutlinedTextField(type, { type = it }, label = { Text("Тип") })
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Минуты") })
            Text("Каждое упражнение с новой строки: Название | подходы | повторы | отдых сек | техника", color = SfMuted)
            OutlinedTextField(rows, { rows = it }, label = { Text("Упражнения") }, minLines = 5)
        }
    }, confirmButton = { Button(onClick = { store.saveCustom(title, type, minutes.toIntOrNull() ?: 45, rows); onSaved() }) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = onClose) { Text("Отмена") } })
}

class NutritionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date = intent.getStringExtra("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        setContent { NZT365Theme { NutritionFullScreen(date) { finish() } } }
    }
}

private data class MealDef(val key: String, val title: String, val share: Double, val suggestion: String)
private val meals = listOf(
    MealDef("breakfast","Завтрак",.25,"Яйца + овсянка + ягоды/фрукт"),
    MealDef("lunch","Обед",.35,"Курица/индейка + рис/гречка + овощи"),
    MealDef("snack","Перекус",.15,"Skyr/творог + фрукт или протеиновый перекус"),
    MealDef("dinner","Ужин",.25,"Рыба/постное мясо + картофель/рис + овощи")
)

@Composable
private fun NutritionFullScreen(date: LocalDate, close: () -> Unit) {
    val context = LocalContext.current
    val store = remember { FullStore(context) }
    val plan = StathamEngine.forDate(date)
    val target = StathamEngine.targets(plan)
    var refresh by remember { mutableIntStateOf(0) }
    val food = remember(refresh) { store.food(date) }
    var addMeal by remember { mutableStateOf<String?>(null) }
    val kcal = food.sumOf { it.kcal }
    val protein = food.sumOf { it.protein }
    val fats = food.sumOf { it.fats }
    val carbs = food.sumOf { it.carbs }

    Scaffold(containerColor = SfBg, topBar = { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("ПИТАНИЕ", color = SfAccent, fontWeight = FontWeight.Bold); Text(date.toString(), color = SfMuted) }; TextButton(onClick = close) { Text("Готово") } } }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SfCard)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Итог дня", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${kcal.roundToInt()} / ${target.calories} ккал", color = SfAccent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        LinearProgressIndicator(progress = { (kcal / target.calories).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
                        Text("Белок ${protein.roundToInt()}/${target.protein} • Жиры ${fats.roundToInt()}/${target.fats} • Углеводы ${carbs.roundToInt()}/${target.carbs} г", color = SfMuted)
                    }
                }
            }
            itemsIndexed(meals) { _, meal ->
                val items = food.filter { it.meal == meal.key }
                val mealKcal = items.sumOf { it.kcal }
                val goal = (target.calories * meal.share).roundToInt()
                Card(colors = CardDefaults.cardColors(containerColor = SfCard)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(meal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${mealKcal.roundToInt()} / $goal ккал", color = SfMuted)
                        Text("Пример: ${meal.suggestion}", color = SfAccent)
                        items.forEach { e ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(e.name, fontWeight = FontWeight.Bold); Text("${e.grams.roundToInt()} г • ${e.kcal.roundToInt()} ккал • Б ${e.protein.roundToInt()}", color = SfMuted) }
                                TextButton(onClick = { store.removeFood(date, e.id); refresh++ }) { Text("×") }
                            }
                        }
                        OutlinedButton(onClick = { addMeal = meal.key }, modifier = Modifier.fillMaxWidth()) { Text("+ Добавить продукт") }
                    }
                }
            }
        }
    }
    addMeal?.let { key -> AddFoodDialog(key, onClose = { addMeal = null }, onAdd = { e -> store.addFood(date, e); addMeal = null; refresh++ }) }
}

@Composable
private fun AddFoodDialog(meal: String, onClose: () -> Unit, onAdd: (FoodEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Добавить продукт") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name,{name=it},label={Text("Продукт")})
            OutlinedTextField(grams,{grams=it.filter { c -> c.isDigit() || c=='.' || c==',' }.replace(',','.')},label={Text("Количество, г")})
            OutlinedTextField(kcal,{kcal=it.filter(Char::isDigit)},label={Text("Ккал / 100 г")})
            OutlinedTextField(protein,{protein=it.filter { c -> c.isDigit() || c=='.' || c==',' }.replace(',','.')},label={Text("Белок / 100 г")})
            OutlinedTextField(fats,{fats=it.filter { c -> c.isDigit() || c=='.' || c==',' }.replace(',','.')},label={Text("Жиры / 100 г")})
            OutlinedTextField(carbs,{carbs=it.filter { c -> c.isDigit() || c=='.' || c==',' }.replace(',','.')},label={Text("Углеводы / 100 г")})
        }
    }, confirmButton = { Button(onClick = {
        val g=grams.toDoubleOrNull() ?: 0.0; val k=kcal.toDoubleOrNull() ?: 0.0
        if (name.isNotBlank() && g > 0) onAdd(FoodEntry(UUID.randomUUID().toString(), meal, name, g, k, protein.toDoubleOrNull() ?: 0.0, fats.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0))
    }) { Text("Добавить") } }, dismissButton = { TextButton(onClick=onClose){Text("Отмена")} })
}
