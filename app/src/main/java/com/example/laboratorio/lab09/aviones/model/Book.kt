package com.example.laboratorio.lab09.aviones.model

data class Book(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val stock:Int,
    val imageUrl: String,
    val profileId: String
)
