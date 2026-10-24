package com.example.assignmentplanner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun StudyScheduleScreen(viewModel: StudyViewModel = viewModel(), onNavigateBack: () -> Unit = {})
{
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (uiState is StudyUiState.Initial)
        {
            studyinitial(onBitmapReady = { viewModel.analyzeNotes(it) }, onNavigateBack = onNavigateBack)
        }
        else if (uiState is StudyUiState.Loading) studyloading()
        else if (uiState is StudyUiState.Success) studysuccess((uiState as StudyUiState.Success).guide) { viewModel.reset() }
        else if (uiState is StudyUiState.Error) studyerror((uiState as StudyUiState.Error).errorMessage) { viewModel.reset() }
    }
}

@Composable
fun studyinitial(onBitmapReady: (Bitmap) -> Unit, onNavigateBack: () -> Unit = {})
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
        Column(modifier = Modifier.fillMaxSize().padding(if (isTablet) 48.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("AI Study Guide", style = MaterialTheme.typography.headlineSmall, fontSize = if (isTablet) 36.sp else 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = {
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
                },
                modifier = Modifier.fillMaxWidth(if (isTablet) 0.6f else 1f).height(if (isTablet) 72.dp else 56.dp)) {
                Icon(Icons.Default.DocumentScanner, null)
                Spacer(Modifier.width(12.dp))
                Text("Scan Notes", fontSize = if (isTablet) 20.sp else 16.sp)
            }
        }

        IconButton(onClick = { onNavigateBack() }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Notes Library", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun studyloading()
{
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))
        Text("Creating Guide...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun studysuccess(guide: StudyGuide, onReset: () -> Unit)
{
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(guide.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        items(guide.sections) { section ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(section.header, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    for (p in 0 until section.points.size)
                    {
                        Text("• " + section.points[p], style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
        item {
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("New Scan") }
        }
    }
}

@Composable
fun studyerror(message: String, onRetry: () -> Unit)
{
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(24.dp))
        Text("AI Error", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(message, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}
