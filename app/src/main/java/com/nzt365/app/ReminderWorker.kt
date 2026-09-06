package com.nzt365.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters): Worker(context,params){
    override fun doWork(): Result {
        val nm=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel=NotificationChannel("daily","NZT 365",NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(channel)
        val notification=NotificationCompat.Builder(applicationContext,"daily")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("NZT 365")
            .setContentText("Проверь задачи на сегодня и закрой день без нуля.")
            .setAutoCancel(true).build()
        nm.notify(365,notification)
        return Result.success()
    }
}
