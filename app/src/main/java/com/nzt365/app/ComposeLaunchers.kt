package com.nzt365.app

import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
) = androidx.activity.compose.rememberLauncherForActivityResult(contract = contract, onResult = onResult)
