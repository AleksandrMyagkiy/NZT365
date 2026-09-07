package com.nzt365.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private data class ExerciseMedia(val url: String?, val label: String)

private fun mediaFor(name: String): ExerciseMedia {
    val s = name.lowercase()
    return when {
        listOf("велосип", "bike", "cycling").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1725902255593-2f585acfce33?auto=format&fit=crop&w=1400&q=82",
            "ENDURANCE"
        )
        listOf("подтяг", "pull-up", "pullup", "drąż").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1575898311530-2af21854a893?auto=format&fit=crop&w=1400&q=82",
            "PULL"
        )
        listOf("жим", "press", "гантел", "dumbbell", "curl", "row", "тяга").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1704223523204-504405c9331a?auto=format&fit=crop&w=1400&q=82",
            "STRENGTH"
        )
        listOf("бег", "run").any { it in s } -> ExerciseMedia(
            null,
            "RUN"
        )
        listOf("присед", "squat", "выпад", "lunge", "deadlift", "румын").any { it in s } -> ExerciseMedia(
            null,
            "LOWER BODY"
        )
        listOf("планк", "plank", "core", "пресс", "dead bug", "bird-dog").any { it in s } -> ExerciseMedia(
            null,
            "CORE"
        )
        else -> ExerciseMedia(null, "TRAINING")
    }
}

@Composable
fun ExercisePhoto(name: String, modifier: Modifier = Modifier) {
    val media = mediaFor(name)
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0A131B))
    ) {
        if (media.url != null) {
            AsyncImage(
                model = media.url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF122B36), Color(0xFF0B151E), Color(0xFF182118)))
                )
            )
            val icon = when (media.label) {
                "RUN" -> Icons.Default.DirectionsRun
                "LOWER BODY" -> Icons.Default.FitnessCenter
                "CORE" -> Icons.Default.SelfImprovement
                else -> Icons.Default.FitnessCenter
            }
            Icon(
                icon,
                contentDescription = null,
                tint = NztAccent,
                modifier = Modifier.align(Alignment.Center).size(72.dp)
            )
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    .58f to Color.Transparent,
                    1f to Color(0xE6071018)
                )
            )
        )

        Column(
            Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Text(media.label, color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(3.dp))
            Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2)
        }
    }
}
