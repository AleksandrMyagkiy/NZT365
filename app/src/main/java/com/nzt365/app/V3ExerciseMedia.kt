package com.nzt365.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * v3 media facade. For now it uses the in-app animated exercise renderer so the
 * workout screen always has a valid offline visual and never falls back to a
 * missing/broken bitmap. The facade lets us swap in richer per-exercise media
 * without changing workout screens.
 */
@Composable
fun ExercisePhoto(name: String, modifier: Modifier = Modifier) {
    ExerciseIllustration(name = name, modifier = modifier)
}
