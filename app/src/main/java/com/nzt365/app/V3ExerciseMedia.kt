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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

private data class ExerciseMedia(
    val url: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun mediaFor(name: String): ExerciseMedia {
    val s = name.lowercase()
    return when {
        listOf("велосип", "bike", "cycling", "cadence").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1541625602330-2277a4c46182?auto=format&fit=crop&w=1400&q=90",
            "CYCLING",
            Icons.Default.PedalBike
        )
        listOf("ходь", "ходом", "walk", "walking", "recovery walk", "cooldown walk").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?auto=format&fit=crop&w=1400&q=90",
            "WALK / RECOVERY",
            Icons.Default.DirectionsWalk
        )
        listOf("подтяг", "pull-up", "pullup", "chin-up", "drąż").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1598268030450-7a476f602473?auto=format&fit=crop&w=1400&q=90",
            "PULL",
            Icons.Default.FitnessCenter
        )
        listOf("отжим", "push-up", "push up").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?auto=format&fit=crop&w=1400&q=90",
            "PUSH-UP",
            Icons.Default.FitnessCenter
        )
        listOf("брусь", "dip").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?auto=format&fit=crop&w=1400&q=90",
            "DIPS",
            Icons.Default.FitnessCenter
        )
        listOf("bench", "жим лёжа", "жим лежа", "floor press", "chest press").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1534367507873-d2d7e24c797f?auto=format&fit=crop&w=1400&q=90",
            "CHEST PRESS",
            Icons.Default.FitnessCenter
        )
        listOf("гантел", "dumbbell", "curl", "shoulder press", "lateral raise").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&w=1400&q=90",
            "DUMBBELLS",
            Icons.Default.FitnessCenter
        )
        listOf("barbell", "штанг", "romanian", "deadlift", "румын").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1400&q=90",
            "BARBELL",
            Icons.Default.FitnessCenter
        )
        listOf("тяга", "row", "band row", "australian row").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1400&q=90",
            "ROW / PULL",
            Icons.Default.FitnessCenter
        )
        listOf("присед", "squat", "выпад", "lunge", "split squat").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1400&q=90",
            "LOWER BODY",
            Icons.Default.FitnessCenter
        )
        listOf("планк", "plank").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1566241142559-40e1dab266c6?auto=format&fit=crop&w=1400&q=90",
            "PLANK",
            Icons.Default.SelfImprovement
        )
        listOf("core", "пресс", "dead bug", "bird-dog", "knee raise", "raise").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=1400&q=90",
            "CORE",
            Icons.Default.SelfImprovement
        )
        listOf("бег", "run", "strides").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?auto=format&fit=crop&w=1400&q=90",
            "RUNNING",
            Icons.Default.DirectionsRun
        )
        listOf("размин", "warm", "mobility", "мобиль", "stretch", "flow").any { it in s } -> ExerciseMedia(
            "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=1400&q=90",
            "WARM-UP / MOBILITY",
            Icons.Default.SelfImprovement
        )
        else -> ExerciseMedia(
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1400&q=90",
            "TRAINING",
            Icons.Default.FitnessCenter
        )
    }
}

@Composable
fun ExercisePhoto(name: String, modifier: Modifier = Modifier) {
    val media = mediaFor(name)
    val shape = RoundedCornerShape(22.dp)

    Box(modifier = modifier.clip(shape).background(Color(0xFF0A131B))) {
        ExerciseImageLayer(media)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x16000000),
                    .56f to Color.Transparent,
                    1f to Color(0xC6071018)
                )
            )
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            color = Color(0xBB071018),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                media.label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = NztAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
        }
    }
}

@Composable
fun ExerciseThumbnail(name: String, modifier: Modifier = Modifier) {
    val media = mediaFor(name)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A131B))
    ) {
        ExerciseImageLayer(media)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x66071018))
                )
            )
        )
    }
}

@Composable
private fun ExerciseImageLayer(media: ExerciseMedia) {
    SubcomposeAsyncImage(
        model = media.url,
        contentDescription = media.label,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    ) {
        when (painter.state) {
            is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            else -> PremiumMediaFallback(media)
        }
    }
}

@Composable
private fun PremiumMediaFallback(media: ExerciseMedia) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(
                    Color(0xFF152A38),
                    Color(0xFF0B1720),
                    Color(0xFF192219)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0x331F3D4B),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                Icon(
                    media.icon,
                    contentDescription = null,
                    tint = NztAccent,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
    }
}
