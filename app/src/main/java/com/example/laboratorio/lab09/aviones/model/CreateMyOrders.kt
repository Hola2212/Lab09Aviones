package com.example.laboratorio.lab09.aviones.model


import kotlinx.serialization.Serializable


@Serializable

data class CreateMyOrders(
    val total: Int,
    val amount: Double,
    val date: String,
    val lines: List<OrderLine>
)
