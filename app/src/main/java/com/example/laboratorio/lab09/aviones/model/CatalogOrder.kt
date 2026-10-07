package com.example.laboratorio.lab09.aviones.model

enum class CatalogOrder{
    NAME,
    PRICE;

    companion object {
        val Default = NAME
        fun fromStored(value: String?): CatalogOrder = entries.firstOrNull{ it.name == value} ?: Default
    }
}

fun sortBooks (books: List<Book>, order: CatalogOrder): List<Book> = when(order){
    CatalogOrder.NAME -> books.sortedBy { it.name }
    CatalogOrder.PRICE -> books.sortedBy { it.priceCents }
}