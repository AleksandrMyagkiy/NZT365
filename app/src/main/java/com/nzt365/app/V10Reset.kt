package com.nzt365.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import java.io.File

object V10Reset {
    fun everything(context: Context) {
        runCatching { WorkManager.getInstance(context).cancelUniqueWork("nzt_daily") }

        // Clear every preference namespace used by all previous NZT generations.
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        prefsDir.listFiles()?.forEach { xml ->
            val name = xml.name.removeSuffix(".xml")
            runCatching { context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit() }
        }

        // Imported books, cached comic pages, progress photos and other local app data.
        context.filesDir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
        context.cacheDir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }

        val restart = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(restart)
        (context as? Activity)?.finishAffinity()
    }
}
