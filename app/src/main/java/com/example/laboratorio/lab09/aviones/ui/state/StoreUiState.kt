package com.example.laboratorio.lab09.aviones.ui.state

import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile

data class StoreUiState(
    val books: List<Book> = emptyList(),
    val profiles: List<Profile> = emptyList(),
    val favoriteBookIds: Set<String> = emptySet()
)