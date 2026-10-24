package com.example.assignmentplanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import java.io.IOException

data class GradeSummary(val courseId: String, val courseName: String,
    val averageGrade: Double?, val gradedCount: Int,
    val submissions: List<ClassroomStudentSubmission> = emptyList(),
    val coursework: List<ClassroomCourseWork> = emptyList())

sealed class ClassroomState
{
    object Idle : ClassroomState()
    object Loading : ClassroomState()
    data class Success(val items: List<ClassroomCourseWork>, val grades: List<GradeSummary> = emptyList(), val gpa: Double = 0.0) : ClassroomState()
    data class Error(val message: String) : ClassroomState()
}

class ClassroomViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application)
{
    var authManager = AuthManager(application)
    var _state = MutableStateFlow<ClassroomState>(ClassroomState.Idle)
    var state: StateFlow<ClassroomState> = _state.asStateFlow()
    var fetchCount = 0
    var lastCourseCount = 0

    fun fetchAssignments(oldToken: String)
    {
        if (_state.value is ClassroomState.Loading) return
        viewModelScope.launch {
            _state.value = ClassroomState.Loading
            fetchCount = fetchCount + 1
            try
            {
                var token = authManager.getFreshAccessToken() ?: oldToken
                var bearer = "Bearer " + token
                var coursesResponse = ClassroomApiClient.service.getCourses(bearer)
                var courses = coursesResponse.courses ?: emptyList()
                lastCourseCount = courses.size

                var allWork = mutableListOf<ClassroomCourseWork>()
                var gradeSummaries = mutableListOf<GradeSummary>()

                for (c in 0 until courses.size)
                {
                    var course = courses[c]
                    try
                    {
                        var workResponse = ClassroomApiClient.service.getCourseWork(bearer, course.id)
                        var coursework = workResponse.courseWork ?: emptyList()
                        allWork.addAll(coursework)

                        var submissionsResponse = ClassroomApiClient.service.getStudentSubmissions(bearer, course.id)
                        var submissions = submissionsResponse.studentSubmissions ?: emptyList()

                        var graded = mutableListOf<ClassroomStudentSubmission>()
                        for (s in 0 until submissions.size)
                        {
                            if (submissions[s].assignedGrade != null) graded.add(submissions[s])
                        }
                        if (graded.isNotEmpty())
                        {
                            var totalPercent = 0.0
                            var validCount = 0

                            for (g in 0 until graded.size)
                            {
                                var sub = graded[g]
                                var workItem: ClassroomCourseWork? = null
                                for (w in 0 until coursework.size)
                                {
                                    if (coursework[w].id == sub.courseWorkId) { workItem = coursework[w]; break }
                                }
                                if (workItem?.maxPoints != null && workItem.maxPoints > 0)
                                {
                                    totalPercent = totalPercent + (sub.assignedGrade!! / workItem.maxPoints)
                                    validCount = validCount + 1
                                }
                            }

                            if (validCount > 0)
                            {
                                gradeSummaries.add(GradeSummary(courseId = course.id, courseName = course.name,
                                    averageGrade = (totalPercent / validCount) * 100, gradedCount = validCount,
                                    submissions = submissions, coursework = coursework))
                            }
                        }
                    } catch (e: Exception) { }
                }

                var gpa = 0.0
                if (gradeSummaries.isNotEmpty())
                {
                    var sum = 0.0
                    for (i in 0 until gradeSummaries.size)
                    {
                        var pct = gradeSummaries[i].averageGrade ?: 0.0
                        var pts = 0.0
                        if (pct >= 93) pts = 4.0
                        else if (pct >= 90) pts = 3.7
                        else if (pct >= 87) pts = 3.3
                        else if (pct >= 83) pts = 3.0
                        else if (pct >= 80) pts = 2.7
                        else if (pct >= 77) pts = 2.3
                        else if (pct >= 73) pts = 2.0
                        else if (pct >= 70) pts = 1.7
                        else if (pct >= 67) pts = 1.3
                        else if (pct >= 65) pts = 1.0
                        else pts = 0.0
                        sum = sum + pts
                    }
                    gpa = sum / gradeSummaries.size
                }

                var sorted = mutableListOf<ClassroomCourseWork>()
                for (i in 0 until allWork.size)
                {
                    var item = allWork[i]
                    var key = item.dueDate.toSortKey()
                    var pos = sorted.size
                    while (pos > 0 && sorted[pos - 1].dueDate.toSortKey() > key)
                    {
                        pos = pos - 1
                    }
                    sorted.add(pos, item)
                }
                _state.value = ClassroomState.Success(sorted, gradeSummaries, gpa)
            }
            catch (e: Exception)
            {
                Log.e("Study²", "Fetch error", e)
                var msg = ""
                if (e.message?.contains("401") == true)
                {
                    msg = "Session expired. Please Sign Out and Sign In again."
                }
                else
                {
                    msg = e.localizedMessage ?: "Failed to load"
                }
                _state.value = ClassroomState.Error(msg)
            }
        }
    }

    fun refresh(accessToken: String)
    {
        _state.value = ClassroomState.Idle
        fetchAssignments(accessToken)
    }
}
