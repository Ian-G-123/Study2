package com.example.assignmentplanner

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.json.JSONObject

sealed class NotesState
{
    object Idle : NotesState()
    object Loading : NotesState()
    object Success : NotesState()
    data class Error(val message: String) : NotesState()
}

class NotesViewModel(application: Application) : AndroidViewModel(application)
{
    var authManager = AuthManager(application)
    var _state = MutableStateFlow<NotesState>(NotesState.Idle)
    var state: StateFlow<NotesState> = _state
    var uploadCount = 0
    var lastFileName = ""

    fun scanAndUpload(context: Context, bitmap: Bitmap, oldToken: String, courseName: String, unitName: String, topicName: String)
    {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = NotesState.Loading
            try
            {
                var token = authManager.getFreshAccessToken() ?: oldToken
                var bearer = "Bearer " + token

                var rootId = getOrCreateFolder(bearer, "Study²", null)
                var courseId = getOrCreateFolder(bearer, courseName.ifBlank { "Unsorted" }, rootId)
                var unitId = getOrCreateFolder(bearer, unitName.ifBlank { "Unit 1" }, courseId)
                var topicId = getOrCreateFolder(bearer, topicName.ifBlank { "General" }, unitId)

                var fileName = "Note_" + System.currentTimeMillis() + ".jpg"
                lastFileName = fileName
                var fileMetadata = DriveFile(name = fileName, parents = listOf(topicId), mimeType = "image/jpeg")
                var createdFile = DriveApiClient.service.createFileMetadata(bearer, fileMetadata)
                var fileId = createdFile.id
                if (fileId == null) throw Exception("Failed to initiate Drive file.")

                var stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                var mediaBody = stream.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())

                DriveApiClient.service.uploadFileMedia(bearer, fileId, mediaBody)

                uploadCount = uploadCount + 1
                _state.value = NotesState.Success
            }
            catch (e: Exception)
            {
                Log.e("Study²", "Drive Error", e)
                var msg = e.localizedMessage ?: "Connection error"
                var errorMsg = ""
                if (msg.contains("400")) errorMsg = "Upload Error (400). Please check your Google Cloud Dashboard to ensure Drive API is active."
                else if (msg.contains("403"))
                {
                    errorMsg = "Forbidden (403). Ensure you signed in with 'Manage Drive files' checked."
                }
                else errorMsg = "Error: " + msg
                _state.value = NotesState.Error(errorMsg)
            }
        }
    }

    suspend fun getOrCreateFolder(token: String, name: String, parentId: String?): String
    {
        var escapedName = name.replace("'", "\\'")
        var query = ""
        if (parentId == null)
        {
            query = "name = '" + escapedName + "' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        }
        else
        {
            query = "name = '" + escapedName + "' and '" + parentId + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        }

        var list = DriveApiClient.service.listFiles(token, query)
        if (list.files.isNotEmpty()) return list.files[0].id!!

        var folder = DriveFile(name = name, mimeType = "application/vnd.google-apps.folder",
            parents = if (parentId != null) listOf(parentId) else null)
        return DriveApiClient.service.createFolder(token, folder).id!!
    }

    fun reset()
    {
        _state.value = NotesState.Idle
    }
}
