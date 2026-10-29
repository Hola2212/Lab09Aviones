package com.example.laboratorio.lab09.aviones.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.laboratorio.lab09.aviones.StoreApplication
import com.example.laboratorio.lab09.aviones.data.repository.NetworkResult
import com.example.laboratorio.lab09.aviones.data.repository.OrdersRepository
import com.example.laboratorio.lab09.aviones.ui.state.OrderDetailUiState
import com.example.laboratorio.lab09.aviones.ui.state.OrdersUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _ordersState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val ordersState: StateFlow<OrdersUiState> = _ordersState.asStateFlow()

    private val _detailState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val detailState: StateFlow<OrderDetailUiState> = _detailState.asStateFlow()

    private var listJob: Job? = null
    private var detailJob: Job? = null

    // Se llama desde un evento (toque), nunca desde un composable: rotar no repite la solicitud.
    fun loadOrders() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _ordersState.value = OrdersUiState.Loading
            _ordersState.value = when (val result = ordersRepository.getOrders()) {
                is NetworkResult.Success -> {
                    if (result.data.isEmpty()) OrdersUiState.Empty else OrdersUiState.Success(result.data)
                }
                is NetworkResult.Error -> OrdersUiState.Error(result.message)
            }
        }
    }

    fun loadOrder(id: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailState.value = OrderDetailUiState.Loading
            _detailState.value = when (val result = ordersRepository.getOrderById(id)) {
                is NetworkResult.Success -> OrderDetailUiState.Success(result.data)
                is NetworkResult.Error -> {
                    if (result.isNotFound) OrderDetailUiState.NotFound
                    else OrderDetailUiState.Error(result.message)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StoreApplication)
                OrdersViewModel(ordersRepository = app.container.ordersRepository)
            }
        }
    }
}