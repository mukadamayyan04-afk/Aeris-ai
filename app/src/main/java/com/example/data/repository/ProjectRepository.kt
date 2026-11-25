package com.example.data.repository

import com.example.data.database.ProjectDao
import com.example.data.database.entities.ProjectEntity
import com.example.data.database.entities.ProjectFileEntity
import com.example.data.database.entities.ProjectNoteEntity
import com.example.data.database.entities.ProjectTaskEntity
import com.example.domain.model.ProjectFileRef
import com.example.domain.model.ProjectNote
import com.example.domain.model.ProjectTask
import com.example.domain.model.ProjectWorkspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(
    private val projectDao: ProjectDao
) {
    fun getAllProjects(): Flow<List<ProjectWorkspace>> =
        projectDao.getAllProjects().map { list -> list.map { it.toDomain() } }

    suspend fun getProjectById(projectId: Long): ProjectWorkspace? =
        projectDao.getProjectById(projectId)?.toDomain()

    suspend fun createProject(name: String, description: String = "", summary: String = ""): Long {
        val trimmed = name.trim().ifBlank { "Untitled Project" }
        return projectDao.insertProject(
            ProjectEntity(
                name = trimmed,
                description = description.trim(),
                summary = summary.trim()
            )
        )
    }

    suspend fun updateProjectSummary(projectId: Long, summary: String) {
        val existing = projectDao.getProjectById(projectId) ?: return
        projectDao.updateProjectSummaryAndProgress(
            projectId = projectId,
            summary = summary.trim(),
            progressPercent = existing.progressPercent
        )
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

    fun getNotesForProject(projectId: Long): Flow<List<ProjectNote>> =
        projectDao.getNotesForProject(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun addNote(projectId: Long, title: String, content: String): Long {
        return projectDao.insertNote(
            ProjectNoteEntity(
                projectId = projectId,
                title = title.trim().ifBlank { "Note" },
                content = content.trim()
            )
        )
    }

    suspend fun deleteNote(noteId: Long) {
        projectDao.deleteNoteById(noteId)
    }

    fun getTasksForProject(projectId: Long): Flow<List<ProjectTask>> =
        projectDao.getTasksForProject(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun addTask(projectId: Long, title: String): Long {
        val id = projectDao.insertTask(
            ProjectTaskEntity(
                projectId = projectId,
                title = title.trim(),
                isDone = false
            )
        )
        recomputeProgress(projectId)
        return id
    }

    suspend fun toggleTask(taskId: Long, isDone: Boolean, projectId: Long) {
        projectDao.setTaskDone(taskId, isDone)
        recomputeProgress(projectId)
    }

    suspend fun deleteTask(taskId: Long, projectId: Long) {
        projectDao.deleteTaskById(taskId)
        recomputeProgress(projectId)
    }

    private suspend fun recomputeProgress(projectId: Long) {
        val tasks = projectDao.getTasksListForProject(projectId)
        val project = projectDao.getProjectById(projectId) ?: return
        val progress = if (tasks.isEmpty()) 0 else ((tasks.count { it.isDone } * 100) / tasks.size)
        projectDao.updateProjectSummaryAndProgress(
            projectId = projectId,
            summary = project.summary,
            progressPercent = progress
        )
    }

    fun getFilesForProject(projectId: Long): Flow<List<ProjectFileRef>> =
        projectDao.getFilesForProject(projectId).map { list -> list.map { it.toDomain() } }

    fun getAllFiles(): Flow<List<ProjectFileRef>> =
        projectDao.getAllFiles().map { list -> list.map { it.toDomain() } }

    suspend fun addFile(
        projectId: Long,
        fileName: String,
        uriString: String,
        previewText: String = "",
        sizeBytes: Long = 0L
    ): Long {
        return projectDao.insertFile(
            ProjectFileEntity(
                projectId = projectId,
                fileName = fileName,
                uriString = uriString,
                previewText = previewText.take(2500),
                sizeBytes = sizeBytes
            )
        )
    }

    suspend fun deleteFile(fileId: Long) {
        projectDao.deleteFileById(fileId)
    }
}
