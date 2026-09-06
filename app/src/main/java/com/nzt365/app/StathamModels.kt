package com.nzt365.app

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

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

internal fun e(name: String, sets: Int, target: String, rest: Int, technique: String, progression: String) =
    StathamExercise(name, sets, target, rest, technique, progression)

internal fun s(code: String, title: String, type: String, minutes: Int, vararg exercises: StathamExercise) =
    StathamSession(code, title, type, minutes, exercises.toList())

object StathamEngine {
    val START: LocalDate = LocalDate.of(2026, 9, 6)
    val FINAL: LocalDate = LocalDate.of(2028, 3, 5)

    val phases = listOf(
        StathamPhase(1, "Фундамент и рекомпозиция", LocalDate.of(2026,9,6), LocalDate.of(2026,11,28), "Техника, привычка и уменьшение талии без потери силы", -50, -250, 1.9),
        StathamPhase(2, "Набор атлетической массы", LocalDate.of(2026,11,29), LocalDate.of(2027,5,15), "Плечи, верх груди, спина, руки и сильные ноги", 150, 0, 1.8),
        StathamPhase(3, "Проявление пресса", LocalDate.of(2027,5,16), LocalDate.of(2027,8,7), "Снизить жир, сохранив мышцы, скорость и рабочую мощность", -200, -350, 2.0),
        StathamPhase(4, "Атлетическое развитие", LocalDate.of(2027,8,8), LocalDate.of(2028,1,8), "Закрепить V-силуэт, мощность и выносливость", 100, -50, 1.8),
        StathamPhase(5, "Финальная сушка", LocalDate.of(2028,1,9), LocalDate.of(2028,3,4), "Чёткий пресс, сохранение силы и финальные фотографии", -250, -400, 2.0)
    )

    private val sessions: Map<String, StathamSession> by lazy {
        (baseSessions() + advancedSessions()).associateBy { it.code }
    }

    fun session(code: String): StathamSession? = sessions[code]

    fun forDate(raw: LocalDate = LocalDate.now()): StathamDayPlan {
        val date = raw.coerceIn(START, FINAL)
        val phase = phases.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) } ?: phases.last()
        val week = (ChronoUnit.DAYS.between(START, date) / 7).toInt() + 1
        val phaseWeek = (ChronoUnit.DAYS.between(phase.start, date) / 7).toInt() + 1
        val index = date.dayOfWeek.value % 7
        val first = arrayOf("FB-A","BIKE-Z2","FB-B","MOB","FB-C","REST","BIKE-Z2-L")
        val later = arrayOf("UPPER-A","BIKE-Z2","LOWER-A","MOB","UPPER-B","BIKE-INT","LOWER-B")
        var code = (if (phase.id == 1) first else later)[index]
        val deload = phaseWeek % 4 == 0
        if (deload && code == "BIKE-INT") code = "BIKE-Z2"
        var session = sessions[code] ?: sessions.getValue("REST")
        if (deload && session.type == "Силовая") {
            session = session.copy(
                title = session.title + " · разгрузка",
                minutes = (session.minutes * 0.7).toInt().coerceAtLeast(20),
                exercises = session.exercises.map { it.copy(sets = (it.sets * 0.6).toInt().coerceAtLeast(1)) }
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
        val bmr = (10 * weight + 6.25 * height - 5 * age + 5).roundToInt()
        val tdee = (bmr * activity).roundToInt()
        val active = plan.session.type in setOf("Силовая", "Велосипед", "Бег", "Кардио", "Контроль")
        val calories = tdee + if (active) plan.phase.trainOffset else plan.phase.restOffset
        val protein = (weight * plan.phase.proteinFactor).roundToInt()
        val fats = (weight * 0.8).roundToInt()
        val carbs = ((calories - protein * 4 - fats * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return StathamTargets(tdee, calories, protein, fats, carbs)
    }
}

private fun LocalDate.coerceIn(min: LocalDate, max: LocalDate): LocalDate = when {
    isBefore(min) -> min
    isAfter(max) -> max
    else -> this
}
