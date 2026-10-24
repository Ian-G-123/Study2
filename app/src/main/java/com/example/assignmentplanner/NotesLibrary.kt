package com.example.assignmentplanner

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(notesVm: NotesViewModel, classroomVm: ClassroomViewModel, user: UserInfo, onNavigateToStudyGuide: () -> Unit)
{
    val context = LocalContext.current
    val notesState by notesVm.state.collectAsState()
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

    var courseName by remember { mutableStateOf("") }
    var unitName by remember { mutableStateOf("") }
    var topicName by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(notesState) {
        if (notesState is NotesState.Success)
        {
            scope.launch {
                snackbarHostState.showSnackbar("Note uploaded to Google Drive")
                delay(2000)
                notesVm.reset()
            }
            courseName = ""
            unitName = ""
            topicName = ""
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(innerPadding)
                .padding(if (isTablet) 32.dp else 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 8.dp)) {
                Button(onClick = { }, modifier = Modifier.weight(1f).height(if (isTablet) 64.dp else 48.dp), shape = RoundedCornerShape(if (isTablet) 16.dp else 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(if (isTablet) 24.dp else 18.dp))
                    Spacer(Modifier.width(if (isTablet) 12.dp else 4.dp))
                    Text("Scan", fontSize = if (isTablet) 18.sp else 14.sp)
                }
                OutlinedButton(onClick = onNavigateToStudyGuide, modifier = Modifier.weight(1f).height(if (isTablet) 64.dp else 48.dp), shape = RoundedCornerShape(if (isTablet) 16.dp else 8.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(if (isTablet) 24.dp else 18.dp))
                    Spacer(Modifier.width(if (isTablet) 12.dp else 4.dp))
                    Text("Guide", fontSize = if (isTablet) 18.sp else 14.sp)
                }
            }

            Text("Notes Library", style = MaterialTheme.typography.titleMedium, fontSize = if (isTablet) 24.sp else 18.sp, fontWeight = FontWeight.Bold)

            val fieldModifier = Modifier.fillMaxWidth().height(if (isTablet) 72.dp else 56.dp)

            OutlinedTextField(value = courseName, onValueChange = { courseName = it }, label = { Text("Course") },
                modifier = fieldModifier, shape = RoundedCornerShape(if (isTablet) 12.dp else 8.dp))

            OutlinedTextField(value = unitName, onValueChange = { unitName = it }, label = { Text("Unit") }, modifier = fieldModifier,
                shape = RoundedCornerShape(if (isTablet) 12.dp else 8.dp))

            OutlinedTextField(value = topicName, onValueChange = { topicName = it }, label = { Text("Topic") },
                modifier = fieldModifier,
                shape = RoundedCornerShape(if (isTablet) 12.dp else 8.dp))

            val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK)
                {
                    val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                    scanResult?.pages?.get(0)?.imageUri?.let { uri ->
                        val bitmap = ImageUtil.decodeSampledBitmapFromUri(context, uri)
                        bitmap?.let {
                            notesVm.scanAndUpload(context, it, user.accessToken, courseName, unitName, topicName)
                        }
                    }
                }
            }

            Button(onClick = {
                    val activity = ImageUtil.findActivity(context)
                    if (activity != null)
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
                },
                modifier = Modifier.fillMaxWidth().height(if (isTablet) 72.dp else 56.dp),
                shape = RoundedCornerShape(if (isTablet) 16.dp else 12.dp),
                enabled = courseName.isNotBlank() && topicName.isNotBlank() && notesState !is NotesState.Loading) {
                if (notesState is NotesState.Loading)
                {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                }
                else
                {
                    Icon(Icons.Default.DocumentScanner, null, modifier = Modifier.size(if (isTablet) 24.dp else 18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Upload to Drive", fontSize = if (isTablet) 20.sp else 16.sp)
                }
            }

            if (notesState is NotesState.Error)
            {
                Text((notesState as NotesState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp), fontSize = if (isTablet) 18.sp else 14.sp)
            }
        }
    }
}
