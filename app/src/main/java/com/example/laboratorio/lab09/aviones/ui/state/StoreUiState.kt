package com.example.laboratorio.lab09.aviones.ui.state

import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile
import com.example.laboratorio.lab09.aviones.model.OrderLine

data class StoreUiState(
    val books: List<Book> = emptyList(),
    val profiles: List<Profile> = emptyList(),
    val favoriteBookIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val orderLines: List<OrderLine> = emptyList(),
    val orderError: String? = null,
    val orderConfirmation : String? = null
){
    val filteredBooks: List<Book> get() = filterBooksByQuery(books, searchQuery)
    val totalOrderUnits : Int get() = orderLines.sumOf{ it.quantity}
    fun unitsInOrder(bookId: String): Int =
        orderLines.firstOrNull { it.bookId == bookId }?.quantity ?: 0
}

fun filterBooksByQuery(books: List<Book>, query: String): List<Book> {
    val cleaned = query.trim().lowercase()
    if (cleaned.isEmpty()) return books
    return books.filter { it.name.lowercase().contains(cleaned) }
}