package com.example.laboratorio.lab09.aviones.data.repository

import android.util.Log
import com.example.laboratorio.lab09.aviones.model.OrderDto
import com.example.laboratorio.lab09.aviones.ui.network.OrderApiService
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val isNotFound: Boolean = false) : NetworkResult<Nothing>
}

interface OrdersRepository {
    suspend fun getOrders(): NetworkResult<List<OrderDto>>
    suspend fun getOrderById(id: String): NetworkResult<OrderDto>
    suspend fun createOrder(order: OrderDto): NetworkResult<OrderDto>
}

class RetrofitOrdersRepository(
    private val apiService: OrderApiService
) : OrdersRepository {

    override suspend fun getOrders(): NetworkResult<List<OrderDto>> {
        return try {
            val orders = apiService.getOrders()
            NetworkResult.Success(orders)
        } catch (e: IOException) {
            Log.e(TAG, "Sin conexión al listar pedidos", e)
            NetworkResult.Error("No pudimos cargar tus pedidos. Revisa tu conexión.")
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ${e.code()} al listar pedidos", e)
            if (e.code() == 404) {
                NetworkResult.Success(emptyList())
            } else {
                NetworkResult.Error("El servidor no pudo atender la solicitud.")
            }
        } catch (e: SerializationException) {
            Log.e(TAG, "JSON inesperado al listar pedidos", e)
            NetworkResult.Error("Recibimos una respuesta inesperada.")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconocido al listar pedidos", e)
            NetworkResult.Error("Ocurrió un error inesperado.")
        }
    }

    override suspend fun getOrderById(id: String): NetworkResult<OrderDto> {
        return try {
            val order = apiService.getOrderById(id)
            NetworkResult.Success(order)
        } catch (e: IOException) {
            Log.e(TAG, "Sin conexión en detalle de pedido", e)
            NetworkResult.Error("No pudimos cargar el pedido. Revisa tu conexión.")
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ${e.code()} en detalle de pedido", e)
            if (e.code() == 404) {
                NetworkResult.Error("Pedido no encontrado", isNotFound = true)
            } else {
                NetworkResult.Error("El servidor no pudo atender la solicitud.")
            }
        } catch (e: SerializationException) {
            Log.e(TAG, "JSON inesperado en detalle de pedido", e)
            NetworkResult.Error("Recibimos una respuesta inesperada.")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconocido en detalle de pedido", e)
            NetworkResult.Error("Ocurrió un error inesperado.")
        }
    }

    override suspend fun createOrder(order: OrderDto): NetworkResult<OrderDto> {
        return try {
            val created = apiService.createOrder(order)
            NetworkResult.Success(created)
        } catch (e: IOException) {
            Log.e(TAG, "Sin conexión al enviar pedido", e)
            NetworkResult.Error("No pudimos enviar tu pedido. Revisa tu conexión.")
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ${e.code()} al enviar pedido", e)
            NetworkResult.Error("El servidor no pudo recibir tu pedido. Intenta de nuevo.")
        } catch (e: SerializationException) {
            Log.e(TAG, "JSON inesperado al enviar pedido", e)
            NetworkResult.Error("Recibimos una respuesta inesperada del servidor.")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconocido al enviar pedido", e)
            NetworkResult.Error("Ocurrió un error inesperado al enviar tu pedido.")
        }
    }

    private companion object {
        const val TAG = "OrdersRepository"
    }
}
