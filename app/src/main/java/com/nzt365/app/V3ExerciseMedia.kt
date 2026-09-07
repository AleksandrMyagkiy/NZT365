package com.nzt365.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

private data class ExerciseMedia(val url: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private fun mediaFor(name: String): ExerciseMedia {
    val s = name.lowercase()
    return when {
        listOf("велосип", "bike", "cycling", "cadence").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=1400&q=88", "CYCLING", Icons.Default.PedalBike)
        listOf("подтяг", "pull-up", "pullup", "chin-up", "drąż").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1598268030450-7a476f602473?auto=format&fit=crop&w=1400&q=88", "PULL", Icons.Default.FitnessCenter)
        listOf("отжим", "push-up", "push up").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?auto=format&fit=crop&w=1400&q=88", "PUSH", Icons.Default.FitnessCenter)
        listOf("брусь", "dip").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&w=1400&q=88", "DIPS", Icons.Default.FitnessCenter)
        listOf("bench", "жим лёжа", "жим лежа", "floor press", "chest press").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?auto=format&fit=crop&w=1400&q=88", "CHEST", Icons.Default.FitnessCenter)
        listOf("гантел", "dumbbell", "curl", "shoulder press", "lateral raise").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&w=1400&q=88", "DUMBBELLS", Icons.Default.FitnessCenter)
        listOf("тяга", "row", "deadlift", "румын", "romanian").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1400&q=88", "PULL / HINGE", Icons.Default.FitnessCenter)
        listOf("присед", "squat", "выпад", "lunge", "split squat").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1400&q=88", "LOWER BODY", Icons.Default.FitnessCenter)
        listOf("планк", "plank", "core", "пресс", "dead bug", "bird-dog", "knee raise").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=1400&q=88", "CORE", Icons.Default.SelfImprovement)
        listOf("бег", "run", "strides").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?auto=format&fit=crop&w=1400&q=88", "RUNNING", Icons.Default.DirectionsRun)
        listOf("размин", "warm", "mobility", "мобиль", "stretch", "flow").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=1400&q=88", "MOBILITY", Icons.Default.SelfImprovement)
        listOf("walk", "ходь", "recovery", "восстанов").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1400&q=88", "RECOVERY", Icons.Default.DirectionsWalk)
        else -> ExerciseMedia(
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1400&q=88", "TRAINING", Icons.Default.FitnessCenter)
    }
}

@Composable
fun ExercisePhoto(name: String, modifier: Modifier = Modifier) {
    val media = mediaFor(name)
    val shape = RoundedCornerShape(22.dp)
    Box(modifier = modifier.clip(shape).background(Color(0xFF0A131B))) {
        SubcomposeAsyncImage(
            model = media.url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        ) {
            when (painter.state) {
                is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                else -> PremiumMediaFallback(media)
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x22000000),
                    .45f to Color.Transparent,
                    1f to Color(0xEE071018)
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(media.label, color = NztAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(3.dp))
            Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PremiumMediaFallback(media: ExerciseMedia) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF142735), Color(0xFF0A151E), Color(0xFF18211A)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = Color(0x331F3D4B), shape = RoundedCornerShape(26.dp)) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Icon(media.icon, contentDescription = null, tint = NztAccent, modifier = Modifier.size(50.dp))
            }
        }
    }
}
