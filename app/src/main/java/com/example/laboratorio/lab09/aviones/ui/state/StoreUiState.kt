package com.example.laboratorio.lab09.aviones.ui.state

import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile

data class StoreUiState(
    val books: List<Book> = emptyList(),
    val profiles: List<Profile> = emptyList(),
    val favoriteBookIds: Set<String> = emptySet(),
    val searchQuery: String = ""
){
    val filteredBooks: List<Book> get() = filterBooksByQuery(books, searchQuery)
}

fun filterBooksByQuery(books: List<Book>, query: String): List<Book> {
    val cleaned = query.trim().lowercase()
    if (cleaned.isEmpty()) return books
    return books.filter { it.name.lowercase().contains(cleaned) }
}