package com.example.assignmentplanner

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.io.IOException

sealed interface StudyUiState
{
    object Initial : StudyUiState
    object Loading : StudyUiState
    data class Success(val guide: StudyGuide) : StudyUiState
    data class Error(val errorMessage: String) : StudyUiState
}

class StudyViewModel : ViewModel()
{
    var _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Initial)
    var uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    var json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    var API_KEY = Secrets.GEMINI_API_KEY
    var scansDone = 0

    val generativeModel by lazy {
        GenerativeModel(modelName = "gemini-3.1-flash-lite", apiKey = API_KEY,
            generationConfig = generationConfig { responseMimeType = "application/json" })
    }

    fun analyzeNotes(bitmap: Bitmap)
    {
        _uiState.value = StudyUiState.Loading
        scansDone = scansDone + 1
        val prompt = """
            Analyze these study notes. Generate a structured study guide based ONLY on the content in the image.
            Do not add outside information or external web links.
            Return ONLY a JSON object:
            {
              "title": "string",
              "sections": [
                {
                  "header": "string",
                  "points": ["string"]
                }
              ]
            }
        """.trimIndent()

        viewModelScope.launch(Dispatchers.IO) {
            try
            {
                var response = generativeModel.generateContent(content {
                    image(bitmap)
                    text(prompt)
                })

                var output = response.text ?: ""
                var cleanedJson = output.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                var guide = json.decodeFromString<StudyGuide>(cleanedJson)
                _uiState.value = StudyUiState.Success(guide)
            }
            catch (e: Exception)
            {
                Log.e("Study²", "AI Error", e)
                _uiState.value = StudyUiState.Error(e.localizedMessage ?: "Analysis failed")
            }
        }
    }

    fun reset() { _uiState.value = StudyUiState.Initial }
}
