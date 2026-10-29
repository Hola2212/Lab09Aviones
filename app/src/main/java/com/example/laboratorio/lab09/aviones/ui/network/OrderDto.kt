package com.example.laboratorio.lab09.aviones.ui.network

import kotlinx.serialization.Serializable

@Serializable
data class OrderLineDto(
    val productId: String,
    val name: String = "",
    val price: Int,
    val quantity: Int
)

@Serializable
data class OrderDto(
    val id: String = "",
    val total: Int,
    val amount: Int,
    val date: String,
    val lines: List<OrderLineDto>
)