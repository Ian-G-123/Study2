package com.example.assignmentplanner

import androidx.compose.foundation.background
 import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color

@Composable
fun TodoListScreen(vm: AssignmentViewModel, classroomVm: ClassroomViewModel = viewModel(), user: UserInfo, onNavigateToScan: () -> Unit)
{
    val classroomState by classroomVm.state.collectAsState()
    var selectedAssignment by remember { mutableStateOf<ClassroomCourseWork?>(null) }
    var showNoDueDate by remember { mutableStateOf(false) }

    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    LaunchedEffect(user.accessToken) {
        classroomVm.fetchAssignments(user.accessToken)
    }

    val scannedAssignments by vm.allAssignments.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(if (isTablet) 32.dp else 16.dp), horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 8.dp)) {
            Button(onClick = onNavigateToScan, modifier = Modifier.weight(1f).height(if (isTablet) 64.dp else 48.dp),
                shape = RoundedCornerShape(if (isTablet) 16.dp else 8.dp), contentPadding = PaddingValues(horizontal = if (isTablet) 24.dp else 8.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(if (isTablet) 24.dp else 18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan Project Assignment to Plan", fontSize = if (isTablet) 18.sp else 12.sp)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (classroomState is ClassroomState.Loading)
            {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Syncing...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else if (classroomState is ClassroomState.Error)
            {
                Column(modifier = Modifier.fillMaxSize().padding(if (isTablet) 64.dp else 32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(if (isTablet) 72.dp else 48.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Error", fontWeight = FontWeight.Bold, fontSize = if (isTablet) 24.sp else 18.sp)
                    Text((classroomState as ClassroomState.Error).message, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { classroomVm.refresh(user.accessToken) }) { Text("Retry") }
                }
            }
            else if (classroomState is ClassroomState.Success)
            {
                var allItems = (classroomState as ClassroomState.Success).items
                var withDueDate = mutableListOf<ClassroomCourseWork>()
                var noDueDate = mutableListOf<ClassroomCourseWork>()
                for (i in 0 until allItems.size)
                {
                    if (allItems[i].dueDate != null) withDueDate.add(allItems[i])
                }
                for (i in 0 until allItems.size)
                {
                    if (allItems[i].dueDate == null) noDueDate.add(allItems[i])
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTablet) 32.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp)) {
                    item { Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 8.dp)) }

                    if (scannedAssignments.isNotEmpty())
                    {
                        item {
                            Text("Local", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = if (isTablet) 18.sp else 14.sp)
                        }
                        items(scannedAssignments) { plan ->
                            for (t in 0 until plan.milestones.size)
                            {
                                var task = plan.milestones[t]
                                taskitem(task = task, isDone = task.isCompleted, isTablet = isTablet, onToggle = { vm.toggleTask(plan.assignmentName, task.task) })
                            }
                        }
                    }

                    if (withDueDate.isNotEmpty())
                    {
                        item {
                            Text("Classroom", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = if (isTablet) 18.sp else 14.sp)
                        }
                        items(withDueDate) { assignment ->
                            ClassroomAssignmentRow(assignment = assignment, onClick = { selectedAssignment = assignment })
                        }
                    }

                    if (noDueDate.isNotEmpty())
                    {
                        item {
                            OutlinedButton(onClick = { showNoDueDate = true }, modifier = Modifier.fillMaxWidth().height(if (isTablet) 64.dp else 48.dp), shape = RoundedCornerShape(12.dp)) {
                                Text("No Due Date (" + noDueDate.size + ")", fontSize = if (isTablet) 16.sp else 14.sp)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 16.dp)) }
                }
            }
        }
    }

    selectedAssignment?.let { assignment ->
        AssignmentDetailDialog(assignment = assignment, onDismiss = { selectedAssignment = null })
    }

    if (showNoDueDate)
    {
        var noDueDateItems = mutableListOf<ClassroomCourseWork>()
        if (classroomState is ClassroomState.Success)
        {
            var src = (classroomState as ClassroomState.Success).items
            for (i in 0 until src.size)
            {
                if (src[i].dueDate == null) noDueDateItems.add(src[i])
            }
        }
        NoDueDateDialog(items = noDueDateItems, onSelect = { selectedAssignment = it; showNoDueDate = false }, onDismiss = { showNoDueDate = false })
    }
}

@Composable
fun taskitem(task: Task, isDone: Boolean, isTablet: Boolean, onToggle: () -> Unit)
{
    Card(modifier = Modifier.fillMaxWidth().clickable { onToggle() }, shape = RoundedCornerShape(if (isTablet) 12.dp else 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(if (isTablet) 2.dp else 1.dp)) {
        Row(modifier = Modifier.padding(if (isTablet) 16.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isDone, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.task, fontSize = if (isTablet) 18.sp else 14.sp, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                var infoText = ""
                if (task.durationMins > 0) infoText = infoText + task.durationMins + " mins"
                if (!task.scheduledDate.isNullOrBlank())
                {
                    if (infoText.isNotEmpty()) infoText = infoText + " • "
                    infoText = infoText + task.scheduledDate
                }
                if (!task.scheduledTime.isNullOrBlank())
                {
                    if (infoText.isNotEmpty()) infoText = infoText + " at "
                    infoText = infoText + task.scheduledTime
                }
                if (infoText.isNotBlank())
                {
                    Text(text = infoText, fontSize = if (isTablet) 14.sp else 12.sp, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
