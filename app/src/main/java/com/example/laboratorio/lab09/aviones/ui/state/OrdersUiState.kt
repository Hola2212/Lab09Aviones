package com.example.laboratorio.lab09.aviones.ui.state

import com.example.laboratorio.lab09.aviones.model.OrderDto

sealed interface OrdersUiState{
    data object Loading: OrdersUiState
    data object Empty: OrdersUiState
    data class Success(val orders: List <OrderDto>): OrdersUiState
    data class Error (val message: String): OrdersUiState
}

sealed interface OrderDetailUiState{
    data object Loading: OrderDetailUiState
    data class Success(val order: OrderDto): OrderDetailUiState
    data object NotFound: OrderDetailUiState
    data class Error (val message: String): OrderDetailUiState
}