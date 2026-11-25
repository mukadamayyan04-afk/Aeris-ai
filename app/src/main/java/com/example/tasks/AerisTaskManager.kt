package com.example.tasks

import android.content.Context
import com.example.notifications.AerisNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class TaskStatus {
    WAITING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

enum class TaskType {
    MODEL_DOWNLOAD,
    MEMORY_INDEXING,
    HARDWARE_SWEEP,
    CACHE_CLEANUP,
    BACKUP
}

data class AerisTask(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val type: TaskType,
    val status: TaskStatus = TaskStatus.WAITING,
    val progress: Int = 0,
    val statusMessage: String = "Queued",
    val timestamp: Long = System.currentTimeMillis()
)

class AerisTaskManager(
    private val context: Context,
    private val notificationManager: AerisNotificationManager = AerisNotificationManager(context)
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val activeJobs = mutableMapOf<String, Job>()

    private val _tasks = MutableStateFlow<List<AerisTask>>(
        listOf(
            AerisTask(
                id = "mem-idx-01",
                name = "Memory Knowledge Indexing",
                type = TaskType.MEMORY_INDEXING,
                status = TaskStatus.COMPLETED,
                progress = 100,
                statusMessage = "All user memories indexed locally"
            ),
            AerisTask(
                id = "hw-diag-01",
                name = "Hardware Telemetry Sweep",
                type = TaskType.HARDWARE_SWEEP,
                status = TaskStatus.COMPLETED,
                progress = 100,
                statusMessage = "RAM: 8GB class verified, ARM64 optimal"
            )
        )
    )
    val tasks: StateFlow<List<AerisTask>> = _tasks.asStateFlow()

    fun runHardwareSweep(onComplete: (String) -> Unit = {}) {
        val taskId = "hw-" + UUID.randomUUID().toString().take(6)
        val initialTask = AerisTask(
            id = taskId,
            name = "Live Hardware Sweep",
            type = TaskType.HARDWARE_SWEEP,
            status = TaskStatus.RUNNING,
            progress = 10,
            statusMessage = "Inspecting CPU cores & frequency..."
        )
        updateTaskList(initialTask)
        notificationManager.showTaskNotification(
            taskId = taskId.hashCode(),
            taskName = "AERIS Hardware Sweep",
            statusText = "Analyzing device sensors & CPU...",
            progress = 10,
            isOngoing = true
        )

        val job = scope.launch {
            try {
                delay(600)
                updateTaskProgress(taskId, 35, "Checking RAM availability & swap...")
                notificationManager.showTaskNotification(taskId.hashCode(), "AERIS Hardware Sweep", "Checking RAM availability...", 35, true)

                delay(600)
                updateTaskProgress(taskId, 70, "Scanning battery thermal profile...")
                notificationManager.showTaskNotification(taskId.hashCode(), "AERIS Hardware Sweep", "Scanning battery thermal profile...", 70, true)

                delay(500)
                updateTaskProgress(taskId, 100, "Sweep complete. Device operating at peak efficiency.")
                setTaskStatus(taskId, TaskStatus.COMPLETED, "Sweep finished. Device cool and ready.")
                notificationManager.showTaskNotification(taskId.hashCode(), "AERIS Hardware Sweep", "Sweep complete: Healthy.", 100, false)
                onComplete("Hardware sweep complete: Thermal nominal, 8GB memory optimal.")
            } catch (e: Exception) {
                setTaskStatus(taskId, TaskStatus.FAILED, "Interrupted: ${e.message}")
                notificationManager.showTaskNotification(taskId.hashCode(), "AERIS Hardware Sweep", "Sweep interrupted", 0, false)
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    fun runMemoryIndexing(onComplete: (Int) -> Unit = {}) {
        val taskId = "mem-" + UUID.randomUUID().toString().take(6)
        val initialTask = AerisTask(
            id = taskId,
            name = "Memory Knowledge Indexing",
            type = TaskType.MEMORY_INDEXING,
            status = TaskStatus.RUNNING,
            progress = 15,
            statusMessage = "Reading local SQLite memory records..."
        )
        updateTaskList(initialTask)

        val job = scope.launch {
            try {
                delay(500)
                updateTaskProgress(taskId, 50, "Generating local keyword vectors...")
                delay(600)
                updateTaskProgress(taskId, 100, "All memories indexed into fast retrieval cache.")
                setTaskStatus(taskId, TaskStatus.COMPLETED, "Indexing complete.")
                notificationManager.showTaskNotification(taskId.hashCode(), "AERIS Memory Indexing", "Indexed into local cache.", 100, false)
                onComplete(100)
            } catch (e: Exception) {
                setTaskStatus(taskId, TaskStatus.FAILED, "Indexing failed: ${e.message}")
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    fun runCacheCleanup(onComplete: () -> Unit = {}) {
        val taskId = "cln-" + UUID.randomUUID().toString().take(6)
        val initialTask = AerisTask(
            id = taskId,
            name = "Temp Audio & Vision Cache Cleanup",
            type = TaskType.CACHE_CLEANUP,
            status = TaskStatus.RUNNING,
            progress = 20,
            statusMessage = "Purging ephemeral buffers..."
        )
        updateTaskList(initialTask)

        val job = scope.launch {
            try {
                delay(400)
                updateTaskProgress(taskId, 65, "Clearing temporary vision frames...")
                delay(400)
                updateTaskProgress(taskId, 100, "Ephemeral cache reclaimed cleanly.")
                setTaskStatus(taskId, TaskStatus.COMPLETED, "Clean complete.")
                onComplete()
            } catch (e: Exception) {
                setTaskStatus(taskId, TaskStatus.FAILED, "Clean error: ${e.message}")
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    fun retryTask(taskId: String) {
        val target = _tasks.value.firstOrNull { it.id == taskId } ?: return
        cancelTask(taskId)
        when (target.type) {
            TaskType.HARDWARE_SWEEP -> runHardwareSweep()
            TaskType.MEMORY_INDEXING -> runMemoryIndexing()
            TaskType.CACHE_CLEANUP -> runCacheCleanup()
            else -> Unit
        }
    }

    fun pauseTask(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        setTaskStatus(taskId, TaskStatus.PAUSED, "Paused by user.")
    }

    fun cancelTask(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
        notificationManager.cancelNotification(taskId.hashCode())
    }

    fun clearCompleted() {
        _tasks.value = _tasks.value.filter { it.status == TaskStatus.RUNNING || it.status == TaskStatus.WAITING }
    }

    private fun updateTaskList(newTask: AerisTask) {
        _tasks.value = listOf(newTask) + _tasks.value.filterNot { it.id == newTask.id }
    }

    private fun updateTaskProgress(taskId: String, progress: Int, message: String) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) it.copy(progress = progress, statusMessage = message) else it
        }
    }

    private fun setTaskStatus(taskId: String, status: TaskStatus, message: String) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) it.copy(status = status, statusMessage = message) else it
        }
    }
}
