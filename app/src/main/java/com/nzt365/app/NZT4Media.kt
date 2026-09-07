package com.nzt365.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Premium, photo-first media resolver. Photos are free Unsplash images and stay online,
 * so the APK remains small. Every media slot has a clean fallback – never a stick figure. */
fun nzt4PhotoUrl(name: String): String {
    val s = name.lowercase()
    val id = when {
        listOf("велосип", "bike", "cycling", "rower").any { it in s } -> "1681295686151-323e071e1e89"
        listOf("подтяг", "pull-up", "pullup", "chin-up", "drąż").any { it in s } -> "1772450014229-2a8b006893e9"
        listOf("отжим", "push-up", "pushup", "press-up").any { it in s } -> "1731341400836-baaa5535b8d5"
        listOf("планк", "plank", "dead bug", "bird-dog", "core", "пресс").any { it in s } -> "1765302741884-e846c7a178df"
        listOf("присед", "squat", "выпад", "lunge", "split squat").any { it in s } -> "1666121363683-1f03bf2e0cc1"
        listOf("бег", "run", "sprint").any { it in s } -> "1552674605-db6ffd4facb5"
        listOf("stretch", "mobility", "мобил", "размин", "warm").any { it in s } -> "1544367567-0f2fcb009e0b"
        else -> "1517836357463-d25dfeac3438"
    }
    return "https://images.unsplash.com/photo-$id?auto=format&fit=crop&w=1400&q=82"
}

@Composable
fun NZT4ExercisePhoto(
    name: String,
    modifier: Modifier = Modifier,
    radius: Int = 22
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(Color(0xFF101A23))
    ) {
        AsyncImage(
            model = nzt4PhotoUrl(name),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color(0xB8071018))
                    )
                )
        )
    }
}

@Composable
fun NZT4MediaFallback(name: String, modifier: Modifier = Modifier) {
    val s = name.lowercase()
    val icon = when {
        listOf("bike", "cycling", "велосип").any { it in s } -> Icons.Default.DirectionsBike
        listOf("run", "бег").any { it in s } -> Icons.Default.DirectionsRun
        listOf("plank", "core", "мобил", "stretch").any { it in s } -> Icons.Default.SelfImprovement
        else -> Icons.Default.FitnessCenter
    }
    Box(
        modifier
            .background(Brush.linearGradient(listOf(Color(0xFF10222D), Color(0xFF101820)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = NztAccent, modifier = Modifier.fillMaxSize(0.28f))
    }
}
