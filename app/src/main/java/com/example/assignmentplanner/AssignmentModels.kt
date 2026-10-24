package com.example.assignmentplanner

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Transient

@Serializable
data class AssignmentPlan(@SerialName("assignment_name") val assignmentName: String = "Unknown Assignment",
    @SerialName("due_date") val dueDate: String = "Not Specified", @SerialName("complexity_score") val complexityScore: Int = 5,
    val summary: String = "No Summary Available",
    val deliverables: List<String> = emptyList(),
    val milestones: List<Task> = emptyList())

@Serializable
data class Task(
    val task: String = "Review Assignment",
    @SerialName("duration_mins") val durationMins: Int = 30,
    val priority: String = "Medium",
    val isCompleted: Boolean = false,
    @SerialName("scheduled_date") val scheduledDate: String? = null,
    @SerialName("scheduled_time") val scheduledTime: String? = null)

@Serializable
data class StudyGuide(val title: String = "Study Guide", val sections: List<StudySection> = emptyList())

@Serializable
data class StudySection(
    val header: String,
    val points: List<String>
)
