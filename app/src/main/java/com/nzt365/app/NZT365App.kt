package com.nzt365.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import java.time.LocalDate

class NZT365App : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity !is MainActivity) {
                    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                }

                if (activity is V4WorkoutActivity && savedInstanceState == null) {
                    val date = activity.intent.getStringExtra("date")?.let {
                        runCatching { LocalDate.parse(it) }.getOrNull()
                    } ?: LocalDate.now()
                    val replacement = activity.getSharedPreferences("nzt_fit_v21", MODE_PRIVATE)
                        .getString("replacement_$date", null)
                    if (!replacement.isNullOrBlank()) {
                        activity.startActivity(
                            Intent(activity, ProWorkoutActivity::class.java)
                                .putExtra("date", date.toString())
                        )
                        activity.finish()
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
