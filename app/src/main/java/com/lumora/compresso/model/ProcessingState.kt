package com.lumora.compresso.model

sealed interface ProcessingState {
    data object Idle : ProcessingState
    data object Processing : ProcessingState
    data object Done : ProcessingState
    data class Error(val message: String) : ProcessingState
}