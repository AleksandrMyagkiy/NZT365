package com.nzt365.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ExerciseArtType { PUSH, PULL, SQUAT, LUNGE, HINGE, CORE, PLANK, CURL, TRICEPS, BIKE, RUN, MOBILITY, CALF, GENERIC }

fun artTypeFor(name: String): ExerciseArtType {
    val s = name.lowercase()
    return when {
        listOf("велосип", "bike", "cycling").any { it in s } -> ExerciseArtType.BIKE
        listOf("бег", "run", "biegan").any { it in s } -> ExerciseArtType.RUN
        listOf("подтяг", "pull-up", "pullup", "drąż").any { it in s } -> ExerciseArtType.PULL
        listOf("отжим", "жим", "push", "press").any { it in s } -> ExerciseArtType.PUSH
        listOf("сплит", "выпад", "lunge", "split").any { it in s } -> ExerciseArtType.LUNGE
        listOf("присед", "squat", "przys").any { it in s } -> ExerciseArtType.SQUAT
        listOf("румын", "hinge", "deadlift", "row", "тяга").any { it in s } -> ExerciseArtType.HINGE
        listOf("планк", "plank").any { it in s } -> ExerciseArtType.PLANK
        listOf("dead bug", "bird-dog", "пресс", "core", "raise").any { it in s } -> ExerciseArtType.CORE
        listOf("curl", "biceps", "сгибание").any { it in s } -> ExerciseArtType.CURL
        listOf("triceps", "разгибание").any { it in s } -> ExerciseArtType.TRICEPS
        listOf("calf", "икр", "носок").any { it in s } -> ExerciseArtType.CALF
        listOf("мобил", "mobility", "stretch", "размин").any { it in s } -> ExerciseArtType.MOBILITY
        else -> ExerciseArtType.GENERIC
    }
}

@Composable
fun ExerciseIllustration(name: String, modifier: Modifier = Modifier) {
    ExerciseThumbnail(name = name, modifier = modifier)
}
