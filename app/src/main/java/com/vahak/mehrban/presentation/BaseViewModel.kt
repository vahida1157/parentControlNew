package com.vahak.mehrban.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI architecture.
 * @param State Data representing the UI.
 * @param Event User actions triggered from the UI.
 * @param Effect One-time UI events (Navigation, Toasts, Snackbars).
 */
abstract class BaseViewModel<State, Event, Effect>(initialState: State) : ViewModel() {

    // The persistent UI State
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    // The one-off UI Effects
    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    // Abstract function every ViewModel must implement to handle user actions
    abstract fun onEvent(event: Event)

    // Helper to easily update the state
    protected fun updateState(reducer: State.() -> State) {
        _state.update(reducer)
    }

    // Helper to easily trigger one-off effects (like navigation)
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}