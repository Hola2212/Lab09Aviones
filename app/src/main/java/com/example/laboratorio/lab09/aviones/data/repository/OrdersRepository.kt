package com.example.laboratorio.lab09.aviones.data.repository

import com.example.laboratorio.lab09.aviones.model.Order
import com.example.laboratorio.lab09.aviones.model.OrderItem
import com.example.laboratorio.lab09.aviones.model.nowIsUTC
import com.example.laboratorio.lab09.aviones.ui.network.OrderApiService
import com.example.laboratorio.lab09.aviones.ui.network.OrderDto
import com.example.laboratorio.lab09.aviones.ui.network.OrderLineDto
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val isNotFound: Boolean = false) : NetworkResult<Nothing>
}

interface OrdersRepository {
    suspend fun getOrders(): NetworkResult<List<Order>>
    suspend fun getOrderById(id: String): NetworkResult<Order>
    suspend fun createOrder(items: List<OrderItem>): NetworkResult<Order>
}

class RetrofitOrdersRepository(
    private val apiService: OrderApiService
) : OrdersRepository {

    override suspend fun getOrders(): NetworkResult<List<Order>> =
        try {
            NetworkResult.Success(apiService.getOrders().map { it.toDomain() })
        } catch (e: IOException) {
            NetworkResult.Error("No pudimos cargar tus pedidos. Revisa tu conexión.")
        } catch (e: HttpException) {
            if (e.code() == 404) {
                NetworkResult.Success(emptyList())
            } else {
                NetworkResult.Error("El servidor no pudo atender la solicitud.")
            }
        } catch (e: SerializationException) {
            NetworkResult.Error("Recibimos una respuesta inesperada.")
        }

    override suspend fun getOrderById(id: String): NetworkResult<Order> =
        try {
            NetworkResult.Success(apiService.getOrderById(id).toDomain())
        } catch (e: IOException) {
            NetworkResult.Error("No pudimos cargar el pedido. Revisa tu conexión.")
        } catch (e: HttpException) {
            if (e.code() == 404) {
                NetworkResult.Error("Pedido no encontrado", isNotFound = true)
            } else {
                NetworkResult.Error("El servidor no pudo atender la solicitud.")
            }
        } catch (e: SerializationException) {
            NetworkResult.Error("Recibimos una respuesta inesperada.")
        }

    override suspend fun createOrder(items: List<OrderItem>): NetworkResult<Order> {
        val request = OrderDto(
            total = items.sumOf { it.price * it.quantity },
            amount = items.sumOf { it.quantity },
            date = nowIsUTC(),
            lines = items.map { it.toDto() }
        )
        return try {
            NetworkResult.Success(apiService.createOrder(request).toDomain())
        } catch (e: IOException) {
            NetworkResult.Error("No pudimos enviar tu pedido. Revisa tu conexión.")
        } catch (e: HttpException) {
            NetworkResult.Error("El servidor no pudo recibir tu pedido. Intenta de nuevo.")
        } catch (e: SerializationException) {
            NetworkResult.Error("Recibimos una respuesta inesperada del servidor.")
        }
    }
}

private fun OrderDto.toDomain() = Order(
    id = id,
    total = total,
    amount = amount,
    date = date,
    lines = lines.map { OrderItem(it.productId, it.name, it.price, it.quantity) }
)

private fun OrderItem.toDto() = OrderLineDto(
    productId = productId,
    name = name,
    price = price,
    quantity = quantity
)