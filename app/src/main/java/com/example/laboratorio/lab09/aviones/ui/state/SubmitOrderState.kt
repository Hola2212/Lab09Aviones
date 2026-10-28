package com.example.laboratorio.lab09.aviones.ui.state

sealed interface SubmitOrderState{
    data object Idle: SubmitOrderState
    data object Submitting: SubmitOrderState
    data object Sent: SubmitOrderState
    data class Failed (val message: String): SubmitOrderState
}