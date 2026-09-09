package com.nzt365.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private data class V4Media(val keys: List<String>, val url: String)

private val v4Media = listOf(
    V4Media(listOf("отжим", "push-up", "push up", "жим", "press"), "https://images.unsplash.com/photo-1764426445448-95103b0024a6?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("подтяг", "pull-up", "pull up", "тяга сверху"), "https://images.unsplash.com/photo-1772450014229-2a8b006893e9?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("велосип", "cycling", "bike"), "https://images.unsplash.com/photo-1783458604921-d9cdde22ef32?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("присед", "squat", "выпад", "split squat", "lunge"), "https://images.pexels.com/photos/4164456/pexels-photo-4164456.jpeg?auto=compress&fit=crop&w=1400&h=900"),
    V4Media(listOf("планк", "plank", "dead bug", "bird-dog", "bird dog", "пресс", "core", "ролик"), "https://images.unsplash.com/photo-1566241142559-40e1dab266c6?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("гантел", "dumbbell", "curl", "сгибан", "бицеп", "трицеп"), "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("румын", "deadlift", "тяга", "row", "good morning"), "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("бег", "running", "run", "спринт", "interval"), "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("мобил", "mobility", "растяж", "stretch", "размин", "warm"), "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?auto=format&fit=crop&w=1400&q=82"),
    V4Media(listOf("резин", "band", "face pull"), "https://images.unsplash.com/photo-1599058917212-d750089bc07e?auto=format&fit=crop&w=1400&q=82")
)

private const val fallbackUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1400&q=82"

fun v4ExerciseImage(name: String): String {
    val n = name.lowercase()
    return v4Media.firstOrNull { media -> media.keys.any { n.contains(it) } }?.url ?: fallbackUrl
}

@Composable
fun V4HeroImage(
    name: String,
    modifier: Modifier = Modifier.fillMaxWidth().height(220.dp),
    darken: Boolean = true
) {
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(NztSurface2)) {
        AsyncImage(
            model = v4ExerciseImage(name),
            contentDescription = name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (darken) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color(0x33000000),
                        1f to Color(0xE6071018)
                    )
                )
            )
        }
    }
}
