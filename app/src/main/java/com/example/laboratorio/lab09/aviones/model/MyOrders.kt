package com.example.laboratorio.lab09.aviones.model
import kotlinx.serialization.Serializable

@Serializable

data class MyOrders(
    val id: String = "",
    val total: Int,
    val amount: Int,
    val date: String,
    val lines: List<OrderLine>
)
