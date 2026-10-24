package com.example.assignmentplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GradesScreen(classroomVm: ClassroomViewModel)
{
    val state by classroomVm.state.collectAsState()

    if (state is ClassroomState.Success)
    {
        val success = state as ClassroomState.Success

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(if (LocalConfiguration.current.screenWidthDp >= 600) 32.dp else 16.dp)) {
            gpacard(success.gpa, LocalConfiguration.current.screenWidthDp >= 600)

            Spacer(modifier = Modifier.height(if (LocalConfiguration.current.screenWidthDp >= 600) 32.dp else 16.dp))

            Text("Course Grades", style = MaterialTheme.typography.titleLarge, fontSize = if (LocalConfiguration.current.screenWidthDp >= 600) 28.sp else 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            if (LocalConfiguration.current.screenWidthDp >= 600)
            {
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    items(success.grades) { summary ->
                        coursegradecard(summary, true)
                    }
                }
            }
            else
            {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(success.grades) { summary ->
                        coursegradecard(summary, false)
                    }
                }
            }
        }
    }
    else
    {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun gpacard(gpa: Double, isTablet: Boolean)
{
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(if (isTablet) 32.dp else 24.dp)) {
        Column(modifier = Modifier.padding(if (isTablet) 48.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Cumulative GPA", style = MaterialTheme.typography.titleMedium, fontSize = if (isTablet) 20.sp else 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("%.2f".format(gpa), style = MaterialTheme.typography.displayLarge, fontSize = if (isTablet) 80.sp else 56.sp,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun coursegradecard(summary: GradeSummary, isTablet: Boolean)
{
    var showWhatIf by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.courseName, style = MaterialTheme.typography.titleMedium, fontSize = if (isTablet) 20.sp else 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(summary.gradedCount.toString() + " assignments", style = MaterialTheme.typography.bodySmall, fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(summary.averageGrade?.toInt().toString() + "%", style = MaterialTheme.typography.headlineSmall, fontSize = if (isTablet) 28.sp else 24.sp,
                    fontWeight = FontWeight.Bold, color = gradecolor(summary.averageGrade ?: 0.0))
            }

            TextButton(onClick = { showWhatIf = true }, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(if (isTablet) 20.dp else 16.dp))
                Spacer(Modifier.width(8.dp))
                Text("What-If Simulator", fontSize = if (isTablet) 16.sp else 14.sp)
            }
        }
    }

    if (showWhatIf)
    {
        whatifdialog(summary) { showWhatIf = false }
    }
}

@Composable
fun whatifdialog(summary: GradeSummary, onDismiss: () -> Unit)
{
    var newAssignmentScore by remember { mutableStateOf("") }
    var newAssignmentMax by remember { mutableStateOf("100") }

    var currentTotal = 0.0
    for (i in 0 until summary.submissions.size)
    {
        if (summary.submissions[i].assignedGrade != null) { currentTotal = currentTotal + summary.submissions[i].assignedGrade!! }
    }
    var currentMax = 0.0
    for (i in 0 until summary.submissions.size)
    {
        var sub = summary.submissions[i]
        if (sub.assignedGrade != null)
        {
            var found = 0.0
            for (j in 0 until summary.coursework.size)
            {
                if (summary.coursework[j].id == sub.courseWorkId) { found = summary.coursework[j].maxPoints ?: 0.0; break }
            }
            currentMax = currentMax + found
        }
    }

    val simulatedGrade = remember(newAssignmentScore, newAssignmentMax) {
        val score = newAssignmentScore.toDoubleOrNull() ?: 0.0
        val max = newAssignmentMax.toDoubleOrNull() ?: 1.0
        var r = 0.0
        if (currentMax + max > 0)
        {
            r = ((currentTotal + score) / (currentMax + max)) * 100
        }
        r
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("What-If Calculator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(value = newAssignmentScore, onValueChange = { newAssignmentScore = it }, label = { Text("Score") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                TextField(value = newAssignmentMax, onValueChange = { newAssignmentMax = it }, label = { Text("Total Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current:")
                    Text(summary.averageGrade?.toInt().toString() + "%", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Simulated:", fontWeight = FontWeight.Bold)
                    Text(simulatedGrade.toInt().toString() + "%", fontWeight = FontWeight.ExtraBold, color = gradecolor(simulatedGrade))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        })
}

fun gradecolor(pct: Double): Color
{
    if (pct >= 90) return Color(0xFF4CAF50)
    else if (pct >= 80) return Color(0xFF2196F3)
    else if (pct >= 70) { return Color(0xFFFF9800) }
    else return Color(0xFFD32F2F)
}
