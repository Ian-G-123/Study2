package com.example.assignmentplanner

import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import android.util.Log

data class DriveFile(
    val id: String? = null,
    val name: String? = null,
    val mimeType: String? = null,
    val parents: List<String>? = null
)

data class DriveFileList(val files: List<DriveFile>)

interface DriveApiService
{
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String
    ): DriveFileList

    @POST("drive/v3/files")
    suspend fun createFileMetadata(@Header("Authorization") bearerToken: String, @Body file: DriveFile): DriveFile

    @PATCH("upload/drive/v3/files/{fileId}?uploadType=media")
    suspend fun uploadFileMedia(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Body body: RequestBody
    ): DriveFile

    @POST("drive/v3/files")
    suspend fun createFolder(@Header("Authorization") bearerToken: String,
        @Body folder: DriveFile): DriveFile
}

object DriveApiClient
{
    const val BASE_URL = "https://www.googleapis.com/"
    var requestsMade = 0

    val service: DriveApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DriveApiService::class.java)
    }
}
