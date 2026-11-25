package com.example.ui.projects

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ProjectWorkspace
import com.example.ui.MainViewModel
import com.example.ui.components.HoloCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

@Composable
fun ProjectWorkspaceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val activeProjectId by viewModel.activeProjectId.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val notes by viewModel.activeProjectNotes.collectAsState()
    val tasks by viewModel.activeProjectTasks.collectAsState()
    val files by viewModel.activeProjectFiles.collectAsState()
    val memories by viewModel.memories.collectAsState()

    val activeProject: ProjectWorkspace? = projects.find { it.id == activeProjectId } ?: projects.firstOrNull()

    var selectedTab by remember { mutableStateOf(0) } // 0: Chats, 1: Notes, 2: Tasks, 3: Files, 4: Summary & Memory
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<ProjectWorkspace?>(null) }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }

    var newTaskInput by remember { mutableStateOf("") }
    var summaryEditInput by remember(activeProject?.id, activeProject?.summary) {
        mutableStateOf(activeProject?.summary.orEmpty())
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val project = activeProject ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: uri.lastPathSegment ?: "project_file"
            viewModel.attachFileToProject(project.id, uri, fileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("project_workspace_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloDark)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(34.dp)
                        .background(HoloSurfaceVariant, CircleShape)
                        .border(1.dp, CyanGlow, CircleShape)
                        .testTag("projects_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Project Workspaces",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                    Text(
                        text = "${projects.size} saved workspaces • Isolated chats, notes, tasks & files",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextCoolGrey
                    )
                }
            }

            OutlinedButton(
                onClick = { showCreateProjectDialog = true },
                border = BorderStroke(1.dp, CyanNeon),
                modifier = Modifier.testTag("create_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", style = Typography.labelSmall, color = CyanNeon)
            }
        }

        // Workspace selector strip
        if (projects.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projects.forEach { proj ->
                    val selected = proj.id == activeProject?.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) CyanDeep.copy(alpha = 0.55f) else HoloSurface,
                        border = BorderStroke(1.dp, if (selected) CyanNeon else HoloCardBorder),
                        modifier = Modifier
                            .clickable { viewModel.selectProject(proj.id) }
                            .testTag("project_chip_${proj.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = if (selected) CyanNeon else TextCoolGrey, modifier = Modifier.size(15.dp))
                            Text(
                                text = proj.name,
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) CyanNeon else TextWhite
                            )
                            Text(
                                text = "${proj.progressPercent}%",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (proj.progressPercent == 100) StatusSuccess else CyanBright
                            )
                        }
                    }
                }
            }
        }

        if (activeProject == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                HoloCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(36.dp))
                        Text("No Project Workspaces Yet", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                        Text(
                            text = "Create a project workspace to keep scoped conversations, notes, tasks, attached files, and project memory cleanly separated, Sir ji.",
                            style = Typography.bodySmall,
                            color = TextCoolGrey
                        )
                        Button(
                            onClick = { showCreateProjectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                            modifier = Modifier.testTag("empty_create_project_button")
                        ) {
                            Text("Create First Project", color = CyanNeon, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Active project progress header + delete
            HoloCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activeProject.name, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
                            if (activeProject.description.isNotBlank()) {
                                Text(activeProject.description, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextCoolGrey)
                            }
                        }
                        IconButton(
                            onClick = { projectToDelete = activeProject },
                            modifier = Modifier.size(30.dp).testTag("delete_project_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Project", tint = AlertRed, modifier = Modifier.size(18.dp))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (activeProject.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (activeProject.progressPercent == 100) StatusSuccess else CyanNeon,
                        trackColor = HoloSurfaceVariant
                    )
                    Text(
                        text = "Task Progress: ${activeProject.progressPercent}% (${tasks.count { it.isDone }}/${tasks.size} done)",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tabs: Chats | Notes | Tasks | Files | Summary
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                shape = RoundedCornerShape(16.dp),
                color = HoloSurface,
                border = BorderStroke(1.dp, HoloCardBorder.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    listOf("Chats", "Notes", "Tasks", "Files", "Summary").forEachIndexed { idx, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = idx }
                                .background(if (selectedTab == idx) CyanDeep else Color.Transparent, RoundedCornerShape(13.dp))
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selectedTab == idx) CyanNeon else TextCoolGrey
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> {
                    val projectChats = conversations.filter { it.projectId == activeProject.id }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Button(
                                onClick = {
                                    viewModel.openProjectChat(activeProject)
                                    onOpenChat()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                                modifier = Modifier.fillMaxWidth().testTag("start_project_chat_button")
                            ) {
                                Icon(Icons.Default.ChatBubble, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Scoped Chat in ${activeProject.name}", color = CyanNeon, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (projectChats.isEmpty()) {
                            item {
                                HoloCard(modifier = Modifier.fillMaxWidth()) {
                                    Text("No project-scoped chats yet. Start one above so history stays cleanly isolated.", style = Typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                        items(projectChats, key = { it.id }) { conv ->
                            HoloCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.selectConversation(conv.id, conv.title)
                                    onOpenChat()
                                }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(conv.title, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                                    Text(
                                        text = conv.lastMessagePreview ?: "Tap to open chat (${conv.messageCount} messages)",
                                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextCoolGrey,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            OutlinedButton(
                                onClick = { showAddNoteDialog = true },
                                border = BorderStroke(1.dp, CyanNeon),
                                modifier = Modifier.fillMaxWidth().testTag("add_project_note_button")
                            ) {
                                Icon(Icons.Default.NoteAdd, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Project Note", style = Typography.labelSmall, color = CyanNeon)
                            }
                        }
                        items(notes, key = { it.id }) { note ->
                            HoloCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(note.title, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(note.content, style = Typography.bodySmall, color = TextWhite)
                                    }
                                    IconButton(onClick = { viewModel.deleteProjectNote(note.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Note", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = newTaskInput,
                                onValueChange = { newTaskInput = it },
                                placeholder = { Text("Add project task...", color = TextMuted) },
                                modifier = Modifier.weight(1f).testTag("project_task_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = HoloCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )
                            Button(
                                onClick = {
                                    if (newTaskInput.isNotBlank()) {
                                        viewModel.addProjectTask(activeProject.id, newTaskInput)
                                        newTaskInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                                modifier = Modifier.testTag("add_project_task_button")
                            ) {
                                Text("Add", color = CyanNeon)
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tasks, key = { it.id }) { task ->
                                HoloCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).clickable {
                                                viewModel.toggleProjectTask(task.id, !task.isDone, activeProject.id)
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (task.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = if (task.isDone) "Completed" else "Pending",
                                                tint = if (task.isDone) StatusSuccess else CyanBright
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = task.title,
                                                style = Typography.bodyMedium.copy(
                                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                                                ),
                                                color = if (task.isDone) TextMuted else TextWhite
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteProjectTask(task.id, activeProject.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Task", tint = AlertRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                border = BorderStroke(1.dp, CyanNeon),
                                modifier = Modifier.fillMaxWidth().testTag("attach_project_file_button")
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attach Document or Code File", style = Typography.labelSmall, color = CyanNeon)
                            }
                        }
                        items(files, key = { it.id }) { fileRef ->
                            HoloCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fileRef.fileName, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyanBright)
                                        Text("${fileRef.sizeBytes} bytes", style = Typography.labelSmall.copy(fontSize = 10.sp), color = GoldAccent)
                                        if (fileRef.previewText.isNotBlank()) {
                                            Text(
                                                text = fileRef.previewText,
                                                style = Typography.bodySmall.copy(fontSize = 10.sp),
                                                color = TextCoolGrey,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteProjectFile(fileRef.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove File", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    val projectMemories = memories.filter {
                        it.projectTag.equals(activeProject.name, ignoreCase = true)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            HoloCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("PROJECT SUMMARY", style = Typography.labelLarge, color = CyanNeon)
                                    OutlinedTextField(
                                        value = summaryEditInput,
                                        onValueChange = { summaryEditInput = it },
                                        placeholder = { Text("Track milestones, decisions, architecture...", color = TextMuted) },
                                        modifier = Modifier.fillMaxWidth().testTag("project_summary_input"),
                                        minLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanNeon,
                                            unfocusedBorderColor = HoloCardBorder,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.updateProjectSummary(activeProject.id, summaryEditInput)
                                            Toast.makeText(context, "Project summary saved", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanDeep)
                                    ) {
                                        Text("Save Summary", color = CyanNeon)
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                text = "PROJECT-TAGGED MEMORIES (${projectMemories.size})",
                                style = Typography.labelLarge,
                                color = CyanBright
                            )
                        }
                        if (projectMemories.isEmpty()) {
                            item {
                                HoloCard(modifier = Modifier.fillMaxWidth()) {
                                    Text("Save memories with Project Tag \"${activeProject.name}\" in Memory to link them here.", style = Typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                        items(projectMemories, key = { it.id }) { mem ->
                            HoloCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(mem.key, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
                                    Text(mem.content, style = Typography.bodySmall, color = TextWhite)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateProjectDialog) {
        AlertDialog(
            onDismissRequest = { showCreateProjectDialog = false },
            title = { Text("CREATE PROJECT WORKSPACE", style = Typography.titleMedium, color = CyanNeon) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.testTag("new_project_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                    OutlinedTextField(
                        value = newProjectDesc,
                        onValueChange = { newProjectDesc = it },
                        label = { Text("Description (optional)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.createProject(newProjectName, newProjectDesc)
                            newProjectName = ""
                            newProjectDesc = ""
                            showCreateProjectDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_project_button")
                ) { Text("CREATE", color = CyanNeon) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProjectDialog = false }) { Text("CANCEL", color = TextCoolGrey) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAddNoteDialog && activeProject != null) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("ADD PROJECT NOTE", style = Typography.titleMedium, color = CyanNeon) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                    OutlinedTextField(
                        value = noteContentInput,
                        onValueChange = { noteContentInput = it },
                        label = { Text("Content") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanNeon, unfocusedBorderColor = HoloCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteContentInput.isNotBlank()) {
                            viewModel.addProjectNote(activeProject.id, noteTitleInput, noteContentInput)
                            noteTitleInput = ""
                            noteContentInput = ""
                            showAddNoteDialog = false
                        }
                    }
                ) { Text("SAVE", color = CyanNeon) }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("CANCEL", color = TextCoolGrey) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    projectToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("DELETE PROJECT?", style = Typography.titleMedium, color = AlertRed) },
            text = {
                Text(
                    text = "Delete \"${target.name}\" along with its project notes, tasks, and attached file links?",
                    style = Typography.bodyMedium,
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProject(target.id)
                    projectToDelete = null
                }) { Text("DELETE", color = AlertRed) }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text("CANCEL", color = CyanNeon) }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
