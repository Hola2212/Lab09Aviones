package com.example.laboratorio.lab09.aviones.ui.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val bookId: String
)

@Entity(tableName = "order_lines")
data class OrderLineEntity(
    @PrimaryKey val bookId: String,
    val quantity: Int
)