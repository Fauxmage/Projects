package com.example.pebtip.ui.screens.login

import com.example.pebtip.ui.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//Log in screen states
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val tokenStorage = TokenStorage()

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    init {
        //Automatically check for stored token when app launches
        scope.launch(Dispatchers.Main.immediate){
            val storedToken = tokenStorage.getToken()
            //No tored token
            if (storedToken == null){
                _state.value = LoginState.Idle
                return@launch
            }
            //Token found
            val authenticated = authenticateToken(storedToken)
            _state.value = if (authenticated) {
                LoginState.Success
            } else {
                LoginState.Idle
            }
        }
    }

    //Called when user press log in button
    fun login(participantId: String, projectId: String){
        if (participantId.isBlank() || projectId.isBlank()){
            _state.value = LoginState.Error("Please fill in both fields")
            return
        }

        scope.launch {
            _state.value = LoginState.Loading

            //Get token from backend
            val token = getToken(participantId, projectId)
            if (token == null){
                _state.value = LoginState.Error("Could not reach server")
                return@launch
            }

            //Verify token is valid
            val authenticated = authenticateToken(token)
            //Persist token so user stay logged in
            if (authenticated){
                tokenStorage.saveToken(token)
                _state.value = LoginState.Success
            } else {
                _state.value = LoginState.Error("Invalid credentials")
            }
        }
    }

    //Called when user starts typing after error
    fun resetError() {
        if (_state.value is LoginState.Error) _state.value = LoginState.Idle
    }

    //Called when user logs out, clear stored token and returns to login
    fun logout(){
        tokenStorage.clearToken()
        _state.value = LoginState.Idle
    }
}

