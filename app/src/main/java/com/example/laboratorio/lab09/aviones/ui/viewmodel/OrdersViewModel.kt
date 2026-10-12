package com.example.laboratorio.lab09.aviones.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laboratorio.lab09.aviones.ui.network.RetrofitClient
import com.example.laboratorio.lab09.aviones.ui.state.OrderDetailUiState
import com.example.laboratorio.lab09.aviones.ui.state.OrdersUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class OrdersViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

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
            _ordersState.value = try {
                val orders = api.getOrders()
                if (orders.isEmpty()) OrdersUiState.Empty else OrdersUiState.Success(orders)
            } catch (e: IOException) {
                Log.e("OrdersViewModel", "Sin conexión al listar", e)
                OrdersUiState.Error("No pudimos cargar tus pedidos. Revisa tu conexión.")
            } catch (e: HttpException) {
                Log.e("OrdersViewModel", "HTTP ${e.code()} al listar", e)
                if (e.code() == 404) OrdersUiState.Empty
                else OrdersUiState.Error("El servidor no pudo atender la solicitud.")
            } catch (e: SerializationException) {
                Log.e("OrdersViewModel", "JSON inesperado al listar", e)
                OrdersUiState.Error("Recibimos una respuesta inesperada.")
            }
        }
    }

    fun loadOrder(id: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailState.value = OrderDetailUiState.Loading
            _detailState.value = try {
                OrderDetailUiState.Success(api.getOrderById(id))
            } catch (e: IOException) {
                Log.e("OrdersViewModel", "Sin conexión en detalle", e)
                OrderDetailUiState.Error("No pudimos cargar el pedido. Revisa tu conexión.")
            } catch (e: HttpException) {
                Log.e("OrdersViewModel", "HTTP ${e.code()} en detalle", e)
                if (e.code() == 404) OrderDetailUiState.NotFound
                else OrderDetailUiState.Error("El servidor no pudo atender la solicitud.")
            } catch (e: SerializationException) {
                Log.e("OrdersViewModel", "JSON inesperado en detalle", e)
                OrderDetailUiState.Error("Recibimos una respuesta inesperada.")
            }
        }
    }
}