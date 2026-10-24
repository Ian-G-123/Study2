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
import kotlinx.serialization.SerialName
import java.io.IOException

class AssignmentViewModel : ViewModel()
{
    var _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    var uiState: StateFlow<UiState> = _uiState.asStateFlow()

    var _allAssignments = MutableStateFlow<List<AssignmentPlan>>(emptyList())
    var allAssignments: StateFlow<List<AssignmentPlan>> = _allAssignments.asStateFlow()

    var json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    var API_KEY = Secrets.GEMINI_API_KEY
    var analyzeCount = 0
    var lastBitmapWidth = 0

    val generativeModel by lazy {
        GenerativeModel(modelName = "gemini-3.1-flash-lite",
            apiKey = Secrets.GEMINI_API_KEY,
            generationConfig = generationConfig { responseMimeType = "application/json" })
    }

    fun analyzeAssignment(bitmap: Bitmap)
    {
        _uiState.value = UiState.Loading
        analyzeCount = analyzeCount + 1
        lastBitmapWidth = bitmap.width
        val prompt = """
            Analyze the provided image and return ONLY a JSON object with this schema:
            {
              "assignment_name": "string",
              "due_date": "YYYY-MM-DD",
              "complexity_score": 5,
              "summary": "string",
              "milestones": [
                {
                  "task": "string (e.g. 'Research topics', 'Draft introduction')",
                  "duration_mins": 30,
                  "priority": "High",
                  "scheduled_date": "YYYY-MM-DD (spread these out before the due date)",
                  "scheduled_time": "HH:MM (24-hour format, e.g. 14:00)"
                }
              ]
            }
            Ensure milestones are specific steps and are scheduled realistically.
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

                var plan = json.decodeFromString<AssignmentPlan>(cleanedJson)
                var oldList = _allAssignments.value
                var newList = mutableListOf<AssignmentPlan>()
                for (i in 0 until oldList.size) newList.add(oldList[i])
                newList.add(plan)
                _allAssignments.value = newList
                _uiState.value = UiState.Success(plan)
            }
            catch (e: Exception)
            {
                Log.e("Study²", "Gemini Error", e)
                var msg = e.localizedMessage ?: "AI error"
                var displayMsg = ""
                if (msg.contains("credits")) displayMsg = "Gemini Error: Prepayment credits depleted. Add funds to your Google Cloud Billing account."
                else if (msg.contains("404"))
                {
                    displayMsg = "Gemini 3.1 Model not found. Ensure Gemini API is active in your project."
                }
                else displayMsg = "AI Error: " + msg
                _uiState.value = UiState.Error(displayMsg)
            }
        }
    }

    fun toggleTask(assignmentName: String, taskName: String)
    {
        var oldPlans = _allAssignments.value
        var updatedList = mutableListOf<AssignmentPlan>()
        for (i in 0 until oldPlans.size)
        {
            var plan = oldPlans[i]
            if (plan.assignmentName == assignmentName)
            {
                var newMilestones = mutableListOf<Task>()
                for (j in 0 until plan.milestones.size)
                {
                    var task = plan.milestones[j]
                    if (task.task == taskName) newMilestones.add(task.copy(isCompleted = !task.isCompleted)) else newMilestones.add(task)
                }
                updatedList.add(plan.copy(milestones = newMilestones))
            }
            else updatedList.add(plan)
        }
        _allAssignments.value = updatedList

        var currentState = _uiState.value
        if (currentState is UiState.Success && currentState.plan.assignmentName == assignmentName)
        {
            var updatedPlan: AssignmentPlan? = null
            for (i in 0 until updatedList.size)
            {
                if (updatedList[i].assignmentName == assignmentName) { updatedPlan = updatedList[i]; break }
            }
            if (updatedPlan != null)
            {
                _uiState.value = UiState.Success(updatedPlan)
            }
        }
    }

    fun reset() { _uiState.value = UiState.Initial }
}
