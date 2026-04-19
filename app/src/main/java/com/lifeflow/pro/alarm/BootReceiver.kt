package com.lifeflow.pro.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeflow.pro.data.db.AppDatabase
import com.lifeflow.pro.domain.model.TaskConstants
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, BootReceiverEntryPoint::class.java)
                val database = entryPoint.database()
                val scheduler = TaskAlarmScheduler(context.applicationContext)
                val now = LocalDateTime.now()
                database.taskDao().getAllOnce()
                    .asSequence()
                    .filter { it.status != TaskConstants.STATUS_COMPLETED }
                    .filter { !it.dueDate.isNullOrBlank() && !it.dueTime.isNullOrBlank() }
                    .filter {
                        runCatching { LocalDateTime.parse("${it.dueDate}T${it.dueTime}") }.getOrNull()?.isAfter(now) == true
                    }
                    .forEach { task ->
                        scheduler.schedule(task.id, task.dueDate!!, task.dueTime!!)
                    }
            }
            pendingResult.finish()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun database(): AppDatabase
}
