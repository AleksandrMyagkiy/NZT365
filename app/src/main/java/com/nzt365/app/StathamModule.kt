package com.nzt365.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StathamExercise(
    val name: String,
    val sets: Int,
    val target: String,
    val restSeconds: Int,
    val technique: String,
    val progression: String
)

data class StathamSession(
    val code: String,
    val title: String,
    val type: String,
    val minutes: Int,
    val exercises: List<StathamExercise>
)

data class StathamPhase(
    val id: Int,
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
    val focus: String,
    val trainOffset: Int,
    val restOffset: Int,
    val proteinFactor: Double
)

data class StathamDayPlan(
    val date: LocalDate,
    val phase: StathamPhase,
    val week: Int,
    val phaseWeek: Int,
    val deload: Boolean,
    val session: StathamSession
)

data class StathamTargets(
    val tdee: Int,
    val calories: Int,
    val protein: Int,
    val fats: Int,
    val carbs: Int
)

object StathamEngine {
    val START: LocalDate = LocalDate.of(2026, 9, 6)
    val FINAL: LocalDate = LocalDate.of(2028, 3, 5)

    val phases = listOf(
        StathamPhase(1, "Фундамент и рекомпозиция", LocalDate.of(2026,9,6), LocalDate.of(2026,11,28),
            "Техника, привычка и уменьшение талии без потери силы", -50, -250, 1.9),
        StathamPhase(2, "Набор атлетической массы", LocalDate.of(2026,11,29), LocalDate.of(2027,5,15),
            "Плечи, верх груди, спина, руки и сильные ноги", 150, 0, 1.8),
        StathamPhase(3, "Проявление пресса", LocalDate.of(2027,5,16), LocalDate.of(2027,8,7),
            "Снизить жир, сохранив мышцы, скорость и рабочую мощность", -200, -350, 2.0),
        StathamPhase(4, "Атлетическое развитие", LocalDate.of(2027,8,8), LocalDate.of(2028,1,8),
            "Закрепить V-силуэт, мощность и выносливость", 100, -50, 1.8),
        StathamPhase(5, "Финальная сушка", LocalDate.of(2028,1,9), LocalDate.of(2028,3,4),
            "Чёткий пресс, сохранение силы и финальные фотографии", -250, -400, 2.0)
    )

    private val sessions = mapOf(
        "FB-A" to StathamSession(
            code = "FB-A",
            title = "Всё тело A — база",
            type = "Силовая",
            minutes = 48,
            exercises = listOf(
                StathamExercise("Разминка", 1, "5–7 минут", 0, "Мягкая динамическая разминка, суставы и пульс. Без боли.", "Перед первым силовым упражнением сделай лёгкий пробный подход."),
                StathamExercise("Отжимания на доске — нейтральный или широкий хват", 3, "6–12", 90, "Корпус одной линией, локти 30–60°. Оставьте 2–3 повтора в запасе.", "12 повторов во всех подходах дважды → резинка на спину или ноги выше."),
                StathamExercise("Тяга фитнес-резинки к поясу", 3, "10–15", 75, "Лопатки назад и вниз. Не вытягивайте подбородок и не тяните шеей.", "15×3 → более тугая резинка или пауза 2 секунды."),
                StathamExercise("Сплит-присед", 3, "8–12 на ногу", 75, "Колено движется по линии стопы, корпус стабилен.", "12×3 → болгарский вариант или более тугая резинка."),
                StathamExercise("Румынская тяга с резинкой", 3, "10–15", 90, "Таз назад, спина нейтральна, движение начинается в тазобедренном суставе.", "При дискомфорте в пояснице замените ягодичным мостом."),
                StathamExercise("Face pull с резинкой", 2, "15–20", 60, "Тяните к уровню глаз, плечи не поднимайте.", "20×2 → пауза или более тугая резинка."),
                StathamExercise("Dead bug", 3, "8–10 на сторону", 45, "Поясница мягко прижата, движения медленные.", "10×3 → увеличьте рычаг."),
                StathamExercise("Кистевой экспандер", 2, "15–25 на руку", 45, "Полностью и подконтрольно сжимайте рукоятки.", "25×2 → более жёсткий экспандер.")
            )
        ),
        "FB-B" to StathamSession(
            code = "FB-B",
            title = "Всё тело B — тяга",
            type = "Силовая",
            minutes = 50,
            exercises = listOf(
                StathamExercise("Разминка", 1, "5–7 минут", 0, "Мягкая динамическая разминка, суставы и пульс. Без боли.", "Перед первым силовым упражнением сделай лёгкий пробный подход."),
                StathamExercise("Подтягивания с резинкой или тяга резинки сверху", 4, "5–10", 120, "Грудь направлена вверх. Не тяните голову к перекладине.", "10×4 → уменьшите помощь резинки."),
                StathamExercise("Узкие отжимания на доске", 3, "6–12", 90, "Локти ближе к корпусу, плечи не проваливаются.", "12×3 → резинка на спину."),
                StathamExercise("Обратные выпады", 3, "8–12 на ногу", 75, "Шаг назад, передняя стопа полностью на полу.", "12×3 → добавьте сопротивление резинки."),
                StathamExercise("Ягодичный мост с резинкой", 3, "12–20", 60, "Не переразгибайте поясницу, сделайте паузу сверху.", "20×3 → по одной ноге или туже резинка."),
                StathamExercise("Разведения рук с резинкой в стороны", 3, "12–20", 60, "Локти слегка согнуты, плечи опущены.", "20×3 → более тугая резинка."),
                StathamExercise("Боковая планка", 3, "20–40 с на сторону", 45, "Тело прямое, шею не зажимайте.", "40 секунд → поднять верхнюю ногу."),
                StathamExercise("Bird-dog", 2, "8 на сторону", 45, "Таз не вращается, движение медленное.", "Добавьте паузу 3 секунды в вытяжении.")
            )
        ),
        "FB-C" to StathamSession(
            code = "FB-C",
            title = "Всё тело C — тонус",
            type = "Силовая",
            minutes = 52,
            exercises = listOf(
                StathamExercise("Разминка", 1, "5–7 минут", 0, "Мягкая динамическая разминка, суставы и пульс. Без боли.", "Перед первым силовым упражнением сделай лёгкий пробный подход."),
                StathamExercise("Отжимания — комфортный хват", 4, "8–15", 75, "Оставляйте два чистых повтора в запасе.", "15×4 → резинка на спину."),
                StathamExercise("Тяга резинки одной рукой", 4, "10–15 на руку", 75, "Таз и грудь не вращаются.", "15×4 → более тугая резинка."),
                StathamExercise("Присед с резинкой", 3, "12–20", 75, "Три секунды вниз, стопы устойчивы.", "20×3 → более тугая резинка."),
                StathamExercise("Good morning с резинкой", 3, "12–15", 75, "Короткая безболезненная амплитуда, таз назад.", "При дискомфорте замените ягодичным мостом."),
                StathamExercise("Сгибание рук с резинкой", 3, "10–15", 60, "Локти неподвижны, опускайте руки медленно.", "15×3 → более тугая резинка."),
                StathamExercise("Разгибание рук с резинкой вниз", 3, "10–15", 60, "Плечи неподвижны, локоть разгибается полностью.", "15×3 → более тугая резинка."),
                StathamExercise("Ролик с колен до стены или ограничителя", 3, "3–8", 90, "Подкрутите таз. Поясница не должна провисать.", "8×3 без боли → отодвиньтесь от стены на 10 см."),
                StathamExercise("Подъём на носки", 3, "15–25", 45, "Полная амплитуда и пауза сверху.", "25×3 → выполнять по одной ноге.")
            )
        ),
        "UPPER-A" to StathamSession(
            code = "UPPER-A",
            title = "Верх A — ширина и сила",
            type = "Силовая",
            minutes = 60,
            exercises = listOf(
                StathamExercise("Разминка", 1, "5–7 минут", 0, "Мягкая динамическая разминка, суставы и пульс. Без боли.", "Перед первым силовым упражнением сделай лёгкий пробный подход."),
                StathamExercise("Подтягивания или подтягивания с резинкой", 4, "4–10", 120, "Полная управляемая амплитуда. Оставьте 1–2 повтора.", "10×4 → уменьшить помощь; затем пауза или дополнительный вес."),
                StathamExercise("Отжимания с резинкой или ногами выше", 4, "8–15", 90, "Грудь между ручками, корпус жёсткий.", "15×4 → усложнить вариант; позже гантельный жим."),
                StathamExercise("Тяга резинки к поясу", 4, "8–15", 90, "Пауза 1–2 секунды у корпуса.", "15×4 → туже резинка или тяга гантели."),
                StathamExercise("Pike push-up", 3, "6–12", 90, "Макушка движется вперёд-вниз, шея нейтральна.", "При боли замените жимом резинки под углом."),
                StathamExercise("Разведения рук в стороны", 3, "15–25", 60, "Без рывка и подъёма плеч.", "25×3 → более тугая резинка."),
                StathamExercise("Сгибание рук с резинкой", 3, "10–15", 60, "Опускайте руки 2–3 секунды.", "15×3 → более тугая резинка."),
                StathamExercise("Разгибание рук с резинкой", 3, "10–15", 60, "Локти зафиксированы.", "15×3 → более тугая резинка."),
                StathamExercise("Ролик для пресса", 3, "6–15", 90, "Только амплитуда без прогиба поясницы.", "15×3 → более длинный выкат.")
            )
        ),
        "UPPER-B" to StathamSession(
            code = "UPPER-B",
            title = "Верх B — грудь и руки",
            type = "Силовая",
            minutes = 60,
            exercises = listOf(
                StathamExercise("Разминка", 1, "5–7 минут", 0, "Мягкая динамическая разминка, суставы и пульс. Без боли.", "Перед первым силовым упражнением сделай лёгкий пробный подход."),
                StathamExercise("Подтягивания обратным хватом", 4, "4–8", 120, "Без раскачки, подбородок не тяните вперёд.", "8×4 → обычный хват или медленнее опускание."),
                StathamExercise("Узкие отжимания с резинкой", 4, "8–15", 90, "Акцент на трицепс, без боли в запястьях и плечах.", "15×4 → более тугая резинка."),
                StathamExercise("Тяга резинки одной рукой", 3, "10–15 на руку", 75, "Лопатка движется, корпус стабилен.", "15×3 → более тугая резинка."),
                StathamExercise("Сведение рук с резинкой", 3, "12–20", 60, "Локти слегка согнуты, не перерастягивайте плечо.", "20×3 → более тугая резинка."),
                StathamExercise("Face pull или задняя дельта", 3, "15–25", 60, "Медленно, плечи опущены.", "25×3 → более тугая резинка."),
                StathamExercise("Молотковое сгибание с резинкой", 3, "10–15", 60, "Кисть остаётся нейтральной.", "15×3 → более тугая резинка."),
                StathamExercise("Разгибание рук из-за головы", 3, "10–15", 60, "Рёбра не выпячивать. При шее — разгибание вниз.", "15×3 → более тугая резинка."),
                StathamExercise("Hollow hold или dead bug", 3, "20–40 с", 60, "Поясница остаётся под контролем.", "40 секунд → увеличьте рычаг.")
            )
        ),
        "LOWER-A" to StathamSession(
            code = "LOWER-A",
            title = "Низ A — сила и мощность",
            type = "Силовая",
            minutes = 57,
            exercises = listOf(
                StathamExercise("Разминка", 1, "5–7 минут", 0, "Мягкая динамическая разминка, суставы и пульс. Без боли.", "Перед первым силовым упражнением сделай лёгкий пробный подход."),
                StathamExercise("Быстрый подъём из приседа на стул", 5, "3–5", 60, "Максимально быстро вверх, мягко. Не до усталости.", "После четырёх недель без боли → низкие прыжки 5×3."),
                StathamExercise("Болгарский сплит-присед", 4, "8–12 на ногу", 90, "Контролируйте колено и таз.", "12×4 → более тугая резинка или гантели."),
                StathamExercise("Румынская тяга с резинкой", 4, "8–15", 90, "Нейтральная спина, нагрузка в ягодицах.", "15×4 → более тугая резинка или гантели."),
                StathamExercise("Шаг на устойчивую опору", 3, "8–12 на ногу", 75, "Поднимайтесь усилием рабочей ноги.", "Увеличьте высоту или сопротивление."),
                StathamExercise("Ягодичный мост", 3, "12–20", 60, "Пауза две секунды сверху.", "По одной ноге или с резинкой."),
                StathamExercise("Подъём на носки", 4, "15–25", 45, "Пауза в верхнем и нижнем положении.", "Перейдите на одну ногу."),
                StathamExercise("Dead bug", 3, "10 на сторону", 45, "Медленно, поясница стабильна.", "Увеличьте рычаг.")
            )
        ),
        "LOWER-B" to StathamSession(
            code = "LOWER-B",
            title = "Низ B — устойчивость",
            type = "Силовая",
            minutes = 55,
            exercises = listOf(
                StathamExercise("Разминка", 1, "5–7 минут", 0, "Мягкая динамическая разминка, суставы и пульс. Без боли.", "Перед первым силовым упражнением сделай лёгкий пробный подход."),
                StathamExercise("Обратные выпады", 4, "8–12 на ногу", 90, "Ровный таз и уверенная опора стопы.", "Более тугая резинка или гантели."),
                StathamExercise("Румынская тяга на одной ноге", 3, "8–12 на ногу", 75, "При необходимости держитесь за опору.", "Добавляйте сопротивление, не жертвуя балансом."),
                StathamExercise("Присед с резинкой", 4, "10–20", 75, "Три секунды вниз, мощно вверх.", "20×4 → более тугая резинка."),
                StathamExercise("Сгибание ног на полотенце", 3, "8–15", 75, "Ягодицы приподняты, амплитуда контролируется.", "15×3 → по одной ноге."),
                StathamExercise("Ягодичный мост на одной ноге", 3, "10–15 на ногу", 60, "Таз не разворачивается.", "Добавьте резинку или паузу."),
                StathamExercise("Подъём на носки", 4, "15–25", 45, "Полная амплитуда.", "Перейдите на одну ногу."),
                StathamExercise("Боковая планка", 3, "30–45 с на сторону", 45, "Шея остаётся нейтральной.", "Поднимите верхнюю ногу."),
                StathamExercise("Кистевой экспандер", 3, "15–25 на руку", 45, "Не допускайте боли в локте.", "Используйте более жёсткий уровень.")
            )
        ),
        "BIKE-Z2" to StathamSession(
            code = "BIKE-Z2",
            title = "Велосипед — аэробная база",
            type = "Велосипед",
            minutes = 65,
            exercises = listOf(
                StathamExercise("Разминка лёгким ходом", 1, "10 мин", 0, "RPE 2–3. Постепенно поднимите каденс.", ""),
                StathamExercise("Основной отрезок Z2", 1, "45–120 мин", 0, "RPE 3–4: можно говорить полными предложениями.", "Добавляйте 5–10 минут в неделю."),
                StathamExercise("Заминка", 1, "5–10 мин", 0, "Очень легко. После — вода и обычный приём пищи.", "")
            )
        ),
        "BIKE-Z2-L" to StathamSession(
            code = "BIKE-Z2-L",
            title = "Длинная поездка Z2",
            type = "Велосипед",
            minutes = 80,
            exercises = listOf(
                StathamExercise("Разминка лёгким ходом", 1, "10 мин", 0, "RPE 2–3, движение свободное.", ""),
                StathamExercise("Ровная поездка Z2", 1, "60–90 мин", 0, "Разговорный темп, без гонки за средней скоростью.", "Увеличивайте время не более чем на 10 минут в неделю."),
                StathamExercise("Заминка", 1, "5–10 мин", 0, "Очень лёгкое вращение педалей.", "")
            )
        ),
        "BIKE-INT" to StathamSession(
            code = "BIKE-INT",
            title = "Велосипед — интервалы",
            type = "Велосипед",
            minutes = 55,
            exercises = listOf(
                StathamExercise("Разминка и три ускорения", 1, "12–15 мин", 0, "Постепенно разогрейтесь до RPE 6.", ""),
                StathamExercise("Рабочие интервалы", 1, "по этапу", 0, "Тяжело, но контролируемо. Все повторы ровные.", "Этап 2: 6×1/2, затем 5×3/3. Этапы 3 и 5: 4×4/4. Этап 4: 8×30 с или 3×8 мин."),
                StathamExercise("Заминка", 1, "10 мин", 0, "Очень легко. Запишите самочувствие и скорость.", "")
            )
        ),
        "MOB" to StathamSession(
            code = "MOB",
            title = "Мобилизация и восстановление",
            type = "Восстановление",
            minutes = 25,
            exercises = listOf(
                StathamExercise("Диафрагмальное дыхание", 2, "5 дыханий", 30, "Длинный выдох, расслабьте шею.", ""),
                StathamExercise("Мягкое втягивание подбородка", 2, "8", 30, "Не запрокидывайте голову. При симптомах уберите упражнение.", ""),
                StathamExercise("Повороты грудного отдела лёжа", 2, "8 на сторону", 30, "Таз остаётся стабильным.", ""),
                StathamExercise("Bird-dog", 2, "8 на сторону", 30, "Медленно, без вращения таза.", ""),
                StathamExercise("Растяжка сгибателя бедра", 2, "30 с на сторону", 30, "Не прогибайте поясницу.", ""),
                StathamExercise("Спокойная прогулка", 1, "20–30 мин", 0, "RPE 2. При сильной усталости можно пропустить.", "")
            )
        ),
        "REST" to StathamSession(
            code = "REST",
            title = "Полный отдых",
            type = "Отдых",
            minutes = 0,
            exercises = listOf(
                
            )
        ),
        "TEST-FINAL" to StathamSession(
            code = "TEST-FINAL",
            title = "Финальный контроль",
            type = "Контроль",
            minutes = 45,
            exercises = listOf(
                StathamExercise("Фото спереди, сбоку и сзади", 1, "одинаковый свет", 0, "Утром, до еды, в той же позе, что на старте.", ""),
                StathamExercise("Вес и окружности", 1, "внести в прогресс", 0, "Талия на уровне пупка после обычного выдоха.", ""),
                StathamExercise("Силовые тесты", 1, "по технике", 0, "Отжимания, подтягивания, ролик и планка.", ""),
                StathamExercise("Контрольные 20 км", 1, "знакомый маршрут", 0, "Сравнивайте при близких погодных условиях.", "")
            )
        )
    )

    private fun phaseFor(date: LocalDate): StathamPhase =
        phases.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }
            ?: if (date.isBefore(START)) phases.first() else phases.last()

    fun forDate(raw: LocalDate = LocalDate.now()): StathamDayPlan {
        val date = when {
            raw.isBefore(START) -> START
            raw.isAfter(FINAL) -> FINAL
            else -> raw
        }
        val phase = phaseFor(date)
        val week = (ChronoUnit.DAYS.between(START, date) / 7).toInt() + 1
        val phaseWeek = (ChronoUnit.DAYS.between(phase.start, date) / 7).toInt() + 1
        val index = date.dayOfWeek.value % 7
        val first = arrayOf("FB-A","BIKE-Z2","FB-B","MOB","FB-C","REST","BIKE-Z2-L")
        val later = arrayOf("UPPER-A","BIKE-Z2","LOWER-A","MOB","UPPER-B","BIKE-INT","LOWER-B")
        var code = (if (phase.id == 1) first else later)[index]
        val deload = phaseWeek % 4 == 0
        if (deload && code == "BIKE-INT") code = "BIKE-Z2"
        var session = sessions[code] ?: sessions["REST"]!!
        if (deload && session.type == "Силовая") {
            session = session.copy(
                title = session.title + " · разгрузка",
                minutes = maxOf(20, (session.minutes * 0.7).toInt()),
                exercises = session.exercises.map { it.copy(sets = maxOf(1, (it.sets * 0.6).toInt())) }
            )
        }
        return StathamDayPlan(date, phase, week, phaseWeek, deload, session)
    }

    fun targets(
        plan: StathamDayPlan,
        age: Int = 41,
        height: Double = 168.0,
        weight: Double = 71.0,
        activity: Double = 1.45
    ): StathamTargets {
        val bmr = kotlin.math.round(10 * weight + 6.25 * height - 5 * age + 5).toInt()
        val tdee = kotlin.math.round(bmr * activity).toInt()
        val active = plan.session.type in setOf("Силовая","Велосипед","Контроль")
        val calories = tdee + if (active) plan.phase.trainOffset else plan.phase.restOffset
        val protein = kotlin.math.round(weight * plan.phase.proteinFactor).toInt()
        val fats = kotlin.math.round(weight * 0.8).toInt()
        val carbs = maxOf(0, kotlin.math.round((calories - protein * 4 - fats * 9) / 4.0).toInt())
        return StathamTargets(tdee, calories, protein, fats, carbs)
    }
}

private class StathamStore(context: Context) {
    private val prefs = context.getSharedPreferences("nzt_statham_module", Context.MODE_PRIVATE)

    fun doneSets(date: LocalDate, code: String, exercise: Int): Int =
        prefs.getInt("sets_${date}_${code}_${exercise}", 0)

    fun setDoneSets(date: LocalDate, code: String, exercise: Int, value: Int) {
        prefs.edit().putInt("sets_${date}_${code}_${exercise}", value).apply()
    }

    fun nutrition(date: LocalDate): List<Int> = listOf(
        prefs.getInt("cal_${date}", 0),
        prefs.getInt("protein_${date}", 0),
        prefs.getInt("fats_${date}", 0),
        prefs.getInt("carbs_${date}", 0)
    )

    fun saveNutrition(date: LocalDate, cal: Int, protein: Int, fats: Int, carbs: Int) {
        prefs.edit()
            .putInt("cal_${date}", cal)
            .putInt("protein_${date}", protein)
            .putInt("fats_${date}", fats)
            .putInt("carbs_${date}", carbs)
            .apply()
    }

    fun habit(date: LocalDate, key: String): Boolean = prefs.getBoolean("habit_${date}_${key}", false)
    fun setHabit(date: LocalDate, key: String, value: Boolean) {
        prefs.edit().putBoolean("habit_${date}_${key}", value).apply()
    }
}

@Composable
fun StathamTodayCard() {
    val context = LocalContext.current
    val store = remember { StathamStore(context) }
    val plan = remember { StathamEngine.forDate(LocalDate.now()) }
    val targets = remember { StathamEngine.targets(plan) }
    var refresh by remember { mutableIntStateOf(0) }
    val completed = plan.session.exercises.indices.sumOf {
        minOf(store.doneSets(plan.date, plan.session.code, it), plan.session.exercises[it].sets)
    }
    val total = plan.session.exercises.sumOf { it.sets }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15181D)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("STATHAM FORM • СЕГОДНЯ", color = Color(0xFFE9FF70), fontWeight = FontWeight.Bold)
            Text(plan.session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${plan.phase.name} • неделя ${plan.week} • ${plan.session.minutes} мин")
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { completed.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Подходы: $completed / $total")
            } else {
                Text("Сегодня восстановление / отдых")
            }
            Text("Питание: ${targets.calories} ккал • Б ${targets.protein} г • Ж ${targets.fats} г • У ${targets.carbs} г")
        }
    }
}

@Composable
fun StathamBodyModule() {
    val context = LocalContext.current
    val store = remember { StathamStore(context) }
    val plan = remember { StathamEngine.forDate(LocalDate.now()) }
    var refresh by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("STATHAM FORM", color = Color(0xFFE9FF70), fontWeight = FontWeight.Bold)
        Text(plan.session.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("${plan.phase.name} • неделя ${plan.week}${if (plan.deload) " • РАЗГРУЗКА" else ""}")
        Text(plan.phase.focus, color = Color(0xFF9BA3AF))

        plan.session.exercises.forEachIndexed { index, ex ->
            val done = store.doneSets(plan.date, plan.session.code, index)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15181D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(ex.name, fontWeight = FontWeight.Bold)
                    Text("${ex.sets} подхода • ${ex.target} • отдых ${ex.restSeconds} с")
                    Text(ex.technique, color = Color(0xFF9BA3AF))
                    Text("Прогрессия: ${ex.progression}", color = Color(0xFFE9FF70))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                store.setDoneSets(plan.date, plan.session.code, index, maxOf(0, done - 1))
                                refresh++
                            },
                            enabled = done > 0
                        ) { Text("−") }
                        Button(
                            onClick = {
                                store.setDoneSets(plan.date, plan.session.code, index, minOf(ex.sets, done + 1))
                                refresh++
                            },
                            enabled = done < ex.sets
                        ) { Text("$done / ${ex.sets}") }
                    }
                }
            }
        }
    }
}

@Composable
fun StathamNutritionModule() {
    val context = LocalContext.current
    val store = remember { StathamStore(context) }
    val date = LocalDate.now()
    val plan = remember { StathamEngine.forDate(date) }
    val target = remember { StathamEngine.targets(plan) }
    val current = remember { store.nutrition(date) }

    var cal by remember { mutableStateOf(current[0].toString()) }
    var protein by remember { mutableStateOf(current[1].toString()) }
    var fats by remember { mutableStateOf(current[2].toString()) }
    var carbs by remember { mutableStateOf(current[3].toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15181D)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ПИТАНИЕ STATHAM FORM", color = Color(0xFFE9FF70), fontWeight = FontWeight.Bold)
            Text("Цель: ${target.calories} ккал • Б ${target.protein} • Ж ${target.fats} • У ${target.carbs}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallNumberField("ккал", cal, { cal = it }, Modifier.weight(1f))
                SmallNumberField("Белок", protein, { protein = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallNumberField("Жиры", fats, { fats = it }, Modifier.weight(1f))
                SmallNumberField("Углев.", carbs, { carbs = it }, Modifier.weight(1f))
            }
            Button(
                onClick = {
                    store.saveNutrition(
                        date,
                        cal.toIntOrNull() ?: 0,
                        protein.toIntOrNull() ?: 0,
                        fats.toIntOrNull() ?: 0,
                        carbs.toIntOrNull() ?: 0
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить питание") }
        }
    }
}

@Composable
private fun SmallNumberField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun NZTHabitsModule() {
    val context = LocalContext.current
    val store = remember { StathamStore(context) }
    val date = LocalDate.now()
    val habits = listOf(
        "sleep" to "Сон 7+ часов",
        "water" to "Вода",
        "training" to "Тренировка / активность",
        "reading" to "Чтение",
        "polish" to "Польский",
        "career" to "Карьерное действие"
    )
    var refresh by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15181D)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ПРИВЫЧКИ", color = Color(0xFFE9FF70), fontWeight = FontWeight.Bold)
            habits.forEach { (key, title) ->
                val checked = store.habit(date, key)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            store.setHabit(date, key, it)
                            refresh++
                        }
                    )
                }
            }
        }
    }
}
