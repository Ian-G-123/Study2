package com.example.assignmentplanner

import android.content.Context
import android.net.Uri
import android.util.Log
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

val KEY_THEME = stringPreferencesKey("app_theme")

data class UserInfo(val name: String,
                    val email: String,
                    val photoUrl: String?, val accessToken: String)

class AuthManager(val appContext: Context)
{
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    var tokenRefreshCount = 0
    var lastToken: String? = null

    companion object
    {
        const val ANDROID_CLIENT_ID = Secrets.ANDROID_CLIENT_ID
        const val REDIRECT_URI = "com.example.assignmentplanner:/oauth2redirect"

        val KEY_NAME = stringPreferencesKey("user_name")
        val KEY_EMAIL = stringPreferencesKey("user_email")
        val KEY_PHOTO = stringPreferencesKey("user_photo")
        val KEY_AUTH_STATE = stringPreferencesKey("auth_state_json")

        val SCOPES = listOf("openid", "email", "profile",
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
            "https://www.googleapis.com/auth/classroom.student-submissions.me.readonly",
            "https://www.googleapis.com/auth/drive.file")

        val SERVICE_CONFIG = AuthorizationServiceConfiguration(Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"), Uri.parse("https://oauth2.googleapis.com/token"))
    }

    val savedUser: Flow<UserInfo?> = appContext.dataStore.data.map { prefs ->
        var result: UserInfo? = null
        var stateJson = prefs[KEY_AUTH_STATE]
        if (stateJson != null)
        {
            try
            {
                var state = AuthState.jsonDeserialize(stateJson)
                if (state.isAuthorized)
                {
                    var token = state.accessToken ?: state.idToken
                    if (token != null)
                    {
                        var name = prefs[KEY_NAME] ?: "User"
                        var email = prefs[KEY_EMAIL] ?: ""
                        var photo = prefs[KEY_PHOTO]
                        result = UserInfo(name, email, photo, token)
                    }
                }
            }
            catch (e: Exception)
            {
                result = null
            }
        }
        result
    }

    suspend fun getFreshAccessToken(): String?
    {
        var prefs = appContext.dataStore.data.first()
        var stateJson = prefs[KEY_AUTH_STATE]
        if (stateJson == null) return null
        var state = AuthState.jsonDeserialize(stateJson)

        if (!state.needsTokenRefresh && state.accessToken != null)
        {
            return state.accessToken
        }

        tokenRefreshCount = tokenRefreshCount + 1
        var authService = AuthorizationService(appContext)
        return suspendCancellableCoroutine { cont ->
            state.performActionWithFreshTokens(authService) { accessToken, _, _ ->
                if (accessToken != null)
                {
                    scope.launch {
                        appContext.dataStore.edit {
                            it[KEY_AUTH_STATE] = state.jsonSerializeString()
                        }
                    }
                    cont.resume(accessToken)
                }
                else
                {
                    cont.resume(null)
                }
                authService.dispose()
            }
        }
    }

    fun buildAuthRequest(): AuthorizationRequest
    {
        return AuthorizationRequest.Builder(SERVICE_CONFIG, ANDROID_CLIENT_ID, ResponseTypeValues.CODE, Uri.parse(REDIRECT_URI))
            .setScopes(SCOPES)
            .setPrompt("select_account")
            .build()
    }

    suspend fun handleAuthResponse(authService: AuthorizationService, authResponse: AuthorizationResponse): UserInfo?
    {
        try
        {
            var tokenRequest = authResponse.createTokenExchangeRequest()

            var tokenResponse = suspendCancellableCoroutine<TokenResponse?> { cont ->
                authService.performTokenRequest(tokenRequest) { response, _ ->
                    cont.resume(response)
                }
            }
            if (tokenResponse == null) return null

            var state = AuthState(authResponse, tokenResponse, null)
            var accessToken = state.accessToken ?: state.idToken
            if (accessToken == null) return null

            var userInfo = fetchUserInfoFromEndpoint(accessToken) ?: UserInfo("User", "", null, accessToken)
            saveAuthState(state, userInfo)
            return userInfo.copy(accessToken = accessToken)
        }
        catch (e: Exception)
        {
            Log.e("Study²", "handleAuthResponse failed: " + e.message)
            return null
        }
    }

    suspend fun fetchUserInfoFromEndpoint(accessToken: String): UserInfo?
    {
        try
        {
            return withContext(Dispatchers.IO) {
                var url = java.net.URL("https://www.googleapis.com/oauth2/v3/userinfo")
                var conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer " + accessToken)
                var out: UserInfo? = null
                if (conn.responseCode == 200)
                {
                    var response = conn.inputStream.bufferedReader().readText()
                    var json = JSONObject(response)
                    out = UserInfo(name = json.optString("name", "User"), email = json.optString("email", ""),
                        photoUrl = json.optString("picture").takeIf { it.isNotBlank() }, accessToken = accessToken)
                }
                out
            }
        }
        catch (e: Exception)
        {
            return null
        }
    }

    suspend fun signOut()
    {
        appContext.dataStore.edit { it.clear() }
    }

    suspend fun saveAuthState(state: AuthState, user: UserInfo)
    {
        appContext.dataStore.edit { prefs ->
            prefs[KEY_AUTH_STATE] = state.jsonSerializeString()
            prefs[KEY_NAME] = user.name
            prefs[KEY_EMAIL] = user.email
            prefs[KEY_PHOTO] = user.photoUrl ?: ""
        }
    }
}
