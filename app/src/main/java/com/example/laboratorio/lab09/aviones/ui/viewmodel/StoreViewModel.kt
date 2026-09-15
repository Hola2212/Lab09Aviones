package com.example.laboratorio.lab09.aviones.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile
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
    private val _uiState = MutableStateFlow(
        StoreUiState(

            books = listOf(
                Book(
                    id = "book1",
                    name = "Cien años de soledad",
                    description = "La historia de la familia Buendía a través de varias generaciones en el pueblo ficticio de Macondo.",
                    price = 149.99,
                    profileId = "author1"
                ),
                Book(
                    id = "book2",
                    name = "El principito",
                    description = "Un relato sobre la amistad, el amor y la importancia de observar más allá de las apariencias.",
                    price = 89.99,
                    profileId = "author2"
                ),
                Book(
                    id = "book3",
                    name = "Orgullo y prejuicio",
                    description = "Una novela sobre relaciones, primeras impresiones y diferencias sociales en la Inglaterra del siglo XIX.",
                    price = 124.99,
                    profileId = "author3"
                )
            ),
            profiles = listOf(
                Profile(
                    id = "author1",
                    name = "Gabriel García Márquez",
                    role = "Autor",
                    location = "Colombia",
                    description = "Escritor colombiano y ganador del Premio Nobel de Literatura, reconocido como una figura destacada del realismo mágico."
                ),
                Profile(
                    id = "author2",
                    name = "Antoine de Saint-Exupéry",
                    role = "Autor y aviador",
                    location = "Francia",
                    description = "Escritor y aviador francés conocido mundialmente por sus obras inspiradas en la amistad, la humanidad y la aviación."
                ),
                Profile(
                    id = "author3",
                    name = "Jane Austen",
                    role = "Autora",
                    location = "Inglaterra",
                    description = "Escritora inglesa reconocida por sus novelas sobre las relaciones humanas y la sociedad de su época."
                )
            )
        )
    )

    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    fun changeFavorite(bookId: String) {
        _uiState.update { currentState ->
            val newFavorites = if (currentState.favoriteBookIds.contains(bookId)) {
                currentState.favoriteBookIds - bookId
            } else {
                currentState.favoriteBookIds + bookId
            }
            currentState.copy(favoriteBookIds = newFavorites)
        }
    }
}