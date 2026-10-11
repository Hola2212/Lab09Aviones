package com.example.laboratorio.lab09.aviones.ui.state
import com.example.laboratorio.lab09.aviones.model.MyOrders

sealed interface MyOrdersUiState {
    object Loading : MyOrdersUiState
    data class Success(val orders: List<MyOrders>) : MyOrdersUiState
    object Empty : MyOrdersUiState
    data class Error(val message: String) : MyOrdersUiState
}