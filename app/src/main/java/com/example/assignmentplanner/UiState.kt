package com.example.assignmentplanner

import com.example.assignmentplanner.AssignmentPlan
import kotlinx.coroutines.flow.MutableStateFlow
import android.util.Log

sealed interface UiState
{
    object Initial : UiState
    object Loading : UiState
    data class Success(val plan: AssignmentPlan) : UiState
    data class Error(val errorMessage: String) : UiState
}

var uiStateDebugCounter = 0
var lastUiStateWasError = false
