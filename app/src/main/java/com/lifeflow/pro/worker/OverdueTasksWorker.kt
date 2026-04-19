package com.lifeflow.pro.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifeflow.pro.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OverdueTasksWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        taskRepository.markOverdueTasks()
        Result.success()
    }.getOrElse { Result.retry() }
}
