package com.example.laboratorio.lab09.aviones.model
import kotlinx.serialization.Serializable

@Serializable
data class OrderLine(
    val bookId: String,
    val quantity: Int
)