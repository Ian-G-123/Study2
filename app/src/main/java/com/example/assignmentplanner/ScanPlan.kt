package com.example.assignmentplanner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun AssignmentScreen(viewModel: AssignmentViewModel = viewModel(), onNavigateBack: () -> Unit = {})
{
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (uiState is UiState.Initial)
        {
            initialstate(onBitmapReady = { viewModel.analyzeAssignment(it) }, onNavigateBack = onNavigateBack)
        }
        else if (uiState is UiState.Loading) loadingstate()
        else if (uiState is UiState.Success)
        {
            successstate(plan = (uiState as UiState.Success).plan,
                onToggle = { assignmentName, taskName -> viewModel.toggleTask(assignmentName, taskName) },
                onReset = { viewModel.reset() })
        }
        else if (uiState is UiState.Error) errorstate((uiState as UiState.Error).errorMessage) { viewModel.reset() }
    }
}

@Composable
fun initialstate(onBitmapReady: (Bitmap) -> Unit, onNavigateBack: () -> Unit = {})
{
    val context = LocalContext.current
    val activity = ImageUtil.findActivity(context)
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.get(0)?.imageUri?.let { uri ->
                val bitmap = ImageUtil.decodeSampledBitmapFromUri(context, uri)
                bitmap?.let { onBitmapReady(it) }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = ImageUtil.decodeSampledBitmapFromUri(context, it)
            bitmap?.let { onBitmapReady(it) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && activity != null)
        {
            val options = GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setGalleryImportAllowed(true)
                .build()
            GmsDocumentScanning.getClient(options).getStartScanIntent(activity).addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(if (isTablet) 48.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))

            Surface(modifier = Modifier.size(if (isTablet) 180.dp else 120.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(if (isTablet) 90.dp else 60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Project Plan", style = MaterialTheme.typography.headlineSmall, fontSize = if (isTablet) 36.sp else 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(48.dp))

            actioncard("Scan Document", Icons.Default.DocumentScanner, MaterialTheme.colorScheme.primary) {
                if (activity != null)
                {
                    if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    {
                        val options = GmsDocumentScannerOptions.Builder()
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                            .setGalleryImportAllowed(true)
                            .build()
                        GmsDocumentScanning.getClient(options).getStartScanIntent(activity).addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                    }
                    else
                    {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            actioncard("Gallery Import", Icons.Default.Image, MaterialTheme.colorScheme.secondary) { galleryLauncher.launch("image/*") }
        }

        IconButton(onClick = {
                onNavigateBack()
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Planner", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun actioncard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit)
{
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Icon(icon, null, modifier = Modifier.padding(12.dp), tint = color)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun loadingstate()
{
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))
        Text("Analyzing Document...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun successstate(plan: AssignmentPlan, onToggle: (String, String) -> Unit, onReset: () -> Unit)
{
    var doneCount = 0
    for (i in 0 until plan.milestones.size)
    {
        if (plan.milestones[i].isCompleted) doneCount = doneCount + 1
    }
    var progress = 0f
    if (plan.milestones.isEmpty()) progress = 0f else progress = doneCount.toFloat() / plan.milestones.size

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { assignmentheader(plan) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Progress", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        items(plan.milestones) { task ->
            taskrow(task = task, onToggle = { onToggle(plan.assignmentName, task.task) })
        }
        item {
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Scan Another Assignment")
            }
        }
    }
}

@Composable
fun assignmentheader(plan: AssignmentPlan)
{
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = plan.assignmentName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Due: " + plan.dueDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Complexity: " + plan.complexityScore + "/10", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (plan.summary.isNotBlank())
            {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = plan.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun taskrow(task: Task, onToggle: () -> Unit)
{
    Card(modifier = Modifier.fillMaxWidth().clickable { onToggle() }, shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.task, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                    Text(text = infoText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun errorstate(message: String, onRetry: () -> Unit)
{
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "AI Error", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
