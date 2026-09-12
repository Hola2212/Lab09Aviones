package com.example.laboratorio.lab09.aviones.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.laboratorio.lab09.aviones.ui.data.BooksRepository
import com.example.laboratorio.lab09.aviones.ui.state.StoreUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// En este código usamos chatGPT con el fin de poder colocar descripción de los libros
//Autor: Abigail Escobar
//IA utilizada: ChatGPT versión del 5 de septiembre del 2026
//Fecha: 5/9/2026

class StoreViewModel: ViewModel(){
    private val _uistate = MutableStateFlow(
        StoreUiState(
            books = BooksRepository.books,
            profiles = BooksRepository.profiles
        )
    )

    val uiState: StateFlow<StoreUiState> = _uistate.asStateFlow()

    fun changeFavorite(bookId: String) {
        _uistate.update { currentState ->
            val newFavorites = if (currentState.favoriteBookIds.contains(bookId)) {
                currentState.favoriteBookIds - bookId
            } else {
                currentState.favoriteBookIds + bookId
            }
            currentState.copy(favoriteBookIds = newFavorites)
        }
    }
}