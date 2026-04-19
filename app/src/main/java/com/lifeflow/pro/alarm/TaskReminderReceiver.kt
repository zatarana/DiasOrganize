package com.lifeflow.pro.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lifeflow.pro.R

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_tasks), NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.task_notification_title))
            .setContentText(context.getString(R.string.task_notification_body, taskId))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(taskId.toInt(), notification)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val CHANNEL_ID = "tasks_channel"
    }
}
