package com.example.assignmentplanner

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class CalendarMode { Week, Month }

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarScreen(vm: AssignmentViewModel, classroomVm: ClassroomViewModel, user: UserInfo)
{
    val assignments by vm.allAssignments.collectAsState()
    val classroomState by classroomVm.state.collectAsState()

    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var calendarMode by remember { mutableStateOf(CalendarMode.Week) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedClassroomAssignment by remember { mutableStateOf<ClassroomCourseWork?>(null) }

    val selectedDateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val selectedDateDisplay = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))

    var localDueToday = mutableListOf<AssignmentPlan>()
    for (i in 0 until assignments.size)
    {
        if (assignments[i].dueDate == selectedDateStr) localDueToday.add(assignments[i])
    }

    var tasksScheduledToday = mutableListOf<Pair<String, Task>>()
    for (i in 0 until assignments.size)
    {
        for (j in 0 until assignments[i].milestones.size)
        {
            if (assignments[i].milestones[j].scheduledDate == selectedDateStr) tasksScheduledToday.add(Pair(assignments[i].assignmentName, assignments[i].milestones[j]))
        }
    }

    var classroomDueToday = mutableListOf<ClassroomCourseWork>()
    if (classroomState is ClassroomState.Success)
    {
        var its = (classroomState as ClassroomState.Success).items
        for (i in 0 until its.size)
        {
            if (its[i].dueDate.toDisplayString() == selectedDateStr) classroomDueToday.add(its[i])
        }
    }

    val hasEvent: (LocalDate) -> Boolean = { date ->
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        var found = false
        for (i in 0 until assignments.size)
        {
            if (assignments[i].dueDate == dateStr) found = true
            for (j in 0 until assignments[i].milestones.size)
            {
                if (assignments[i].milestones[j].scheduledDate == dateStr) found = true
            }
        }
        if (classroomState is ClassroomState.Success)
        {
            var its2 = (classroomState as ClassroomState.Success).items
            for (i in 0 until its2.size)
            {
                if (its2[i].dueDate.toDisplayString() == dateStr) found = true
            }
        }
        found
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(if (isTablet) 32.dp else 16.dp, 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (calendarMode == CalendarMode.Week) "Week" else "Month", style = MaterialTheme.typography.titleMedium,
                fontSize = if (isTablet) 24.sp else 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            IconButton(onClick = {
                calendarMode = if (calendarMode == CalendarMode.Week) CalendarMode.Month else CalendarMode.Week
            }) {
                Icon(if (calendarMode == CalendarMode.Week) Icons.Default.CalendarMonth else Icons.Default.ViewWeek, null, modifier = Modifier.size(if (isTablet) 32.dp else 24.dp))
            }
        }

        if (calendarMode == CalendarMode.Week)
        {
            weekselector(selectedDate = selectedDate, isTablet = isTablet, onDateSelected = { selectedDate = it }, hasEvent = hasEvent)
        }
        else
        {
            monthgrid(currentMonth = currentMonth, selectedDate = selectedDate, onDateSelected = { selectedDate = it },
                onMonthChange = { currentMonth = it }, hasEvent = hasEvent, isTablet = isTablet)
        }

        Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 8.dp))

        Text(selectedDateDisplay, style = MaterialTheme.typography.titleSmall, fontSize = if (isTablet) 20.sp else 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = if (isTablet) 32.dp else 16.dp), color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(8.dp))

        if (localDueToday.isEmpty() && classroomDueToday.isEmpty() && tasksScheduledToday.isEmpty())
        {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Nothing scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (isTablet) 18.sp else 14.sp)
            }
        }
        else
        {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (isTablet) 32.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tasksScheduledToday.isNotEmpty())
                {
                    item {
                        Text("Tasks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    items(tasksScheduledToday) { (assignmentName, task) ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.task, fontWeight = FontWeight.Medium)
                                    Text(assignmentName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (task.scheduledTime != null)
                                {
                                    Text(task.scheduledTime, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (localDueToday.isNotEmpty() || classroomDueToday.isNotEmpty())
                {
                    item {
                        Text("Due", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                    items(localDueToday) { plan ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(if (isTablet) 16.dp else 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp)) {
                                Text(plan.assignmentName, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 20.sp else 16.sp, color = MaterialTheme.colorScheme.primary)
                                Text(plan.summary, style = MaterialTheme.typography.bodySmall, fontSize = if (isTablet) 16.sp else 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    items(classroomDueToday) { assignment ->
                        ClassroomAssignmentRow(assignment = assignment, onClick = { selectedClassroomAssignment = assignment })
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    selectedClassroomAssignment?.let { assignment ->
        AssignmentDetailDialog(assignment = assignment, onDismiss = { selectedClassroomAssignment = null })
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun monthgrid(currentMonth: YearMonth, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, onMonthChange: (YearMonth) -> Unit, hasEvent: (LocalDate) -> Boolean, isTablet: Boolean)
{
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstOfMonth.dayOfWeek.value % 7

    var days = mutableListOf<LocalDate?>()
    for (i in 0 until firstDayOfWeek) days.add(null)
    for (i in 1..daysInMonth) days.add(currentMonth.atDay(i))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTablet) 32.dp else 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, null)
            }
            Text(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("S", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("M", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("T", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("W", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("T", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("F", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("S", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val rows = days.chunked(7)
        for (r in 0 until rows.size)
        {
            var week = rows[r]
            Row(modifier = Modifier.fillMaxWidth().height(if (isTablet) 80.dp else 50.dp)) {
                for (k in 0 until week.size)
                {
                    var date = week[k]
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                            .background(if (date == selectedDate) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable(enabled = date != null) { date?.let { onDateSelected(it) } },
                        contentAlignment = Alignment.Center) {
                        if (date != null)
                        {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal,
                                    color = if (date == selectedDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                if (hasEvent(date))
                                {
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                    }
                }
                if (week.size < 7)
                {
                    for (q in 0 until 7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun weekselector(selectedDate: LocalDate, isTablet: Boolean, onDateSelected: (LocalDate) -> Unit, hasEvent: (LocalDate) -> Boolean)
{
    val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    var days = mutableListOf<LocalDate>()
    for (i in 0..6) days.add(startOfWeek.plusDays(i.toLong()))

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = if (isTablet) 16.dp else 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTablet) 32.dp else 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDateSelected(selectedDate.minusWeeks(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(if (isTablet) 32.dp else 24.dp))
            }
            Text(startOfWeek.format(DateTimeFormatter.ofPattern("MMMM d")) + " - " +
                startOfWeek.plusDays(6).format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                style = MaterialTheme.typography.bodyMedium, fontSize = if (isTablet) 18.sp else 14.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = { onDateSelected(selectedDate.plusWeeks(1)) }) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(if (isTablet) 32.dp else 24.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (d in 0 until days.size)
            {
                var date = days[d]
                var isSelected = date == selectedDate
                var isToday = date == LocalDate.now()
                var hasSomething = hasEvent(date)

                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(if (isTablet) 70.dp else 45.dp).clip(RoundedCornerShape(if (isTablet) 16.dp else 12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else Color.Transparent)
                        .clickable { onDateSelected(date) }
                        .padding(vertical = if (isTablet) 16.dp else 8.dp)) {
                    Text(text = date.dayOfWeek.name.take(3), style = MaterialTheme.typography.labelSmall, fontSize = if (isTablet) 14.sp else 10.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyLarge, fontSize = if (isTablet) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

                    if (hasSomething)
                    {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary))
                    }
                    else
                    {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}
