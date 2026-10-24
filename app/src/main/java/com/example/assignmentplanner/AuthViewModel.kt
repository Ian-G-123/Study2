package com.example.assignmentplanner

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

sealed class UserAuthState
{
    object Loading : UserAuthState()
    object SignedOut : UserAuthState()
    data class SignedIn(val user: UserInfo) : UserAuthState()
    data class Error(val message: String) : UserAuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application)
{
    var authManager = AuthManager(application)
    var authService = AuthorizationService(application)

    var _authState = MutableStateFlow<UserAuthState>(UserAuthState.Loading)
    var authState: StateFlow<UserAuthState> = _authState.asStateFlow()
    var signInAttempts = 0
    var lastErrorText = ""

    init
    {
        viewModelScope.launch {
            authManager.savedUser.collectLatest { saved ->
                if (saved != null) _authState.value = UserAuthState.SignedIn(saved)
                else _authState.value = UserAuthState.SignedOut
            }
        }
    }

    fun buildSignInIntent(): Intent
    {
        signInAttempts = signInAttempts + 1
        var authRequest = authManager.buildAuthRequest()
        return authService.getAuthorizationRequestIntent(authRequest)
    }

    fun handleSignInResult(intent: Intent?)
    {
        if (intent == null)
        {
            _authState.value = UserAuthState.Error("Sign-in cancelled")
            return
        }
        val authResponse = AuthorizationResponse.fromIntent(intent)
        val authException = AuthorizationException.fromIntent(intent)

        if (authException != null)
        {
            lastErrorText = "Auth error: " + (authException.errorDescription ?: authException.message)
            _authState.value = UserAuthState.Error("Auth error: " + (authException.errorDescription ?: authException.message))
        }
        else if (authResponse != null)
        {
            _authState.value = UserAuthState.Loading
            viewModelScope.launch {
                var user = authManager.handleAuthResponse(authService, authResponse)
                if (user == null)
                {
                    _authState.value = UserAuthState.Error("Failed to get user info. Try again.")
                }
            }
        }
        else _authState.value = UserAuthState.Error("No response received. Try again.")
    }

    fun signOut()
    {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun clearError()
    {
        _authState.value = UserAuthState.SignedOut
    }

    override fun onCleared()
    {
        super.onCleared()
        authService.dispose()
    }
}
