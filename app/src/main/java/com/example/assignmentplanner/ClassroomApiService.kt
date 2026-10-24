package com.example.assignmentplanner

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import java.util.concurrent.TimeUnit
import android.util.Log

data class ClassroomCourse(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("section") val section: String? = null,
    @SerializedName("courseState") val courseState: String = "")

data class ClassroomCoursesResponse(@SerializedName("courses") val courses: List<ClassroomCourse>? = null)

data class ClassroomDueDate(@SerializedName("year") val year: Int = 0, @SerializedName("month") val month: Int = 0, @SerializedName("day") val day: Int = 0)

data class ClassroomDriveFile(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("alternateLink") val alternateLink: String = ""
)

data class ClassroomDriveMaterial(@SerializedName("driveFile") val driveFile: ClassroomDriveFile? = null)

data class ClassroomLink(
    @SerializedName("url") val url: String = "",
    @SerializedName("title") val title: String = ""
)

data class ClassroomLinkMaterial(@SerializedName("link") val link: ClassroomLink? = null)

data class ClassroomMaterial(@SerializedName("driveFile") val driveFile: ClassroomDriveMaterial? = null,
                             @SerializedName("link") val link: ClassroomLinkMaterial? = null)

data class ClassroomCourseWork(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("maxPoints") val maxPoints: Double? = null,
    @SerializedName("dueDate") val dueDate: ClassroomDueDate? = null,
    @SerializedName("creationTime") val creationTime: String = "",
    @SerializedName("materials") val materials: List<ClassroomMaterial>? = null,
    @SerializedName("courseId") val courseId: String = "",
    @SerializedName("alternateLink") val alternateLink: String = "")

data class ClassroomCourseWorkResponse(@SerializedName("courseWork") val courseWork: List<ClassroomCourseWork>? = null)

data class ClassroomStudentSubmission(
    @SerializedName("id") val id: String = "",
    @SerializedName("courseWorkId") val courseWorkId: String = "",
    @SerializedName("assignedGrade") val assignedGrade: Double? = null,
    @SerializedName("draftGrade") val draftGrade: Double? = null,
    @SerializedName("state") val state: String = ""
)

data class ClassroomStudentSubmissionsResponse(@SerializedName("studentSubmissions") val studentSubmissions: List<ClassroomStudentSubmission>? = null)

interface ClassroomApiService
{

    @GET("v1/courses")
    suspend fun getCourses(
        @Header("Authorization") bearerToken: String,
        @Query("courseStates") courseState: String = "ACTIVE",
        @Query("studentId") studentId: String = "me"
    ): ClassroomCoursesResponse

    @GET("v1/courses/{courseId}/courseWork")
    suspend fun getCourseWork(
        @Header("Authorization") bearerToken: String,
        @Path("courseId") courseId: String,
        @Query("orderBy") orderBy: String = "dueDate asc",
        @Query("courseWorkStates") state: String = "PUBLISHED"
    ): ClassroomCourseWorkResponse

    @GET("v1/courses/{courseId}/courseWork/-/studentSubmissions")
    suspend fun getStudentSubmissions(@Header("Authorization") bearerToken: String, @Path("courseId") courseId: String): ClassroomStudentSubmissionsResponse
}

object ClassroomApiClient
{
    const val BASE_URL = "https://classroom.googleapis.com/"
    var requestCount = 0
    var lastRequestUrl = ""

    var loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    var okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val service: ClassroomApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClassroomApiService::class.java)
    }
}

fun ClassroomDueDate?.toDisplayString(): String
{
    if (this == null)
    {
        return "No due date"
    }
    else
    {
        return "%04d-%02d-%02d".format(year, month, day)
    }
}

fun ClassroomDueDate?.toSortKey(): String
{
    if (this == null) { return "9999-99-99" }
    return "%04d-%02d-%02d".format(year, month, day)
}
