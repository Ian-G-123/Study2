package com.example.assignmentplanner

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    user: UserInfo,
    vm: AssignmentViewModel,
    classroomVm: ClassroomViewModel,
    onNavigateToTodo: () -> Unit = {},
    onNavigateToGrades: () -> Unit = {},
    onNavigate: (String) -> Unit = { screen ->
        when (screen) {
            "todo" -> onNavigateToTodo()
            "grades" -> onNavigateToGrades()
            else -> {}
        }
    }
) {
    val localAssignments by vm.allAssignments.collectAsState()
    val classroomState by classroomVm.state.collectAsState()

    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    LaunchedEffect(user.accessToken) {
        if (classroomState is ClassroomState.Idle) {
            classroomVm.fetchAssignments(user.accessToken)
        }
    }

    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    var localTaskCount = 0
    for (i in 0 until localAssignments.size) {
        for (j in 0 until localAssignments[i].milestones.size) {
            if (!localAssignments[i].milestones[j].isCompleted) localTaskCount += 1
        }
    }

    var allClassroomTasks: List<ClassroomCourseWork> = emptyList()
    if (classroomState is ClassroomState.Success) {
        allClassroomTasks = (classroomState as ClassroomState.Success).items
    }

    val upcomingAssignments = mutableListOf<ClassroomCourseWork>()
    for (i in 0 until allClassroomTasks.size) {
        val dueKey = allClassroomTasks[i].dueDate.toSortKey()
        if (dueKey >= today) upcomingAssignments.add(allClassroomTasks[i])
    }

    val totalTasks = localTaskCount + upcomingAssignments.size
    var gpa = 0.0
    if (classroomState is ClassroomState.Success) {
        gpa = (classroomState as ClassroomState.Success).gpa
    }

    val padding = if (isTablet) 32.dp else 16.dp
    val titleSize = if (isTablet) 32.sp else 24.sp

    val limit = if (isTablet) 5 else 3
    val shownAssignments = upcomingAssignments.take(limit)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user.name.split(" ").firstOrNull() ?: user.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(if (isTablet) 56.dp else 44.dp)
                        .clickable { onNavigate("user") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user.name.firstOrNull() ?: 'U').toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isTablet) 22.sp else 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 12.dp)
            ) {
                DashStat(
                    label = "Tasks Pending",
                    value = totalTasks.toString(),
                    modifier = Modifier.weight(1f),
                    isTablet = isTablet,
                    onClick = { onNavigate("todo") }
                )
                DashStat(
                    label = "Current GPA",
                    value = "%.2f".format(gpa),
                    modifier = Modifier.weight(1f),
                    isTablet = isTablet,
                    onClick = { onNavigate("grades") }
                )
            }
        }

        // Quick Navigation Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = if (isTablet) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )

                val gridItems = listOf(
                    NavGridItem("Planner", Icons.Default.Assignment, MaterialTheme.colorScheme.primary) { onNavigate("todo") },
                    NavGridItem("Notes", Icons.Default.LibraryBooks, MaterialTheme.colorScheme.secondary) { onNavigate("notes") },
                    NavGridItem("Calendar", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.tertiary) { onNavigate("calendar") },
                    NavGridItem("Grades", Icons.Default.Grade, MaterialTheme.colorScheme.error) { onNavigate("grades") },
                    NavGridItem("Settings", Icons.Default.Settings, MaterialTheme.colorScheme.outline) { onNavigate("settings") },
                    NavGridItem("Account", Icons.Default.Person, MaterialTheme.colorScheme.primary) { onNavigate("user") }
                )

                val rowChunkSize = 3
                val rows = gridItems.chunked(rowChunkSize)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)
                    ) {
                        rowItems.forEach { navItem ->
                            Box(modifier = Modifier.weight(1f)) {
                                NavGridCard(navItem, isTablet)
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Assignments Window / Container
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upcoming Assignments",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = if (isTablet) 20.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { onNavigate("todo") }) {
                            Text("View All", fontSize = if (isTablet) 16.sp else 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (classroomState is ClassroomState.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (shownAssignments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "All caught up! No upcoming assignments.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            shownAssignments.forEach { assignment ->
                                ClassroomAssignmentRow(
                                    assignment = assignment,
                                    onClick = { onNavigate("todo") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NavGridItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun NavGridCard(item: NavGridItem, isTablet: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isTablet) 20.dp else 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(if (isTablet) 52.dp else 40.dp),
                shape = CircleShape,
                color = item.color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.color,
                        modifier = Modifier.size(if (isTablet) 28.dp else 22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontSize = if (isTablet) 15.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DashStat(label: String, value: String, modifier: Modifier, isTablet: Boolean, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(if (isTablet) 20.dp else 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = if (isTablet) 16.sp else 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = if (isTablet) 36.sp else 24.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun dashstat(label: String, value: String, modifier: Modifier, isTablet: Boolean, onClick: () -> Unit) {
    DashStat(label = label, value = value, modifier = modifier, isTablet = isTablet, onClick = onClick)
}
