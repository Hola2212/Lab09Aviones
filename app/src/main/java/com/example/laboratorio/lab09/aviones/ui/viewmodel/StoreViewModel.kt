package com.example.laboratorio.lab09.aviones.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.ui.data.BooksRepository
import com.example.laboratorio.lab09.aviones.ui.state.StoreUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import kotlin.random.nextInt

// En este código usamos chatGPT con el fin de poder colocar descripción de los libros y generar los titulos y los subjects
//Autor: Abigail Escobar
//IA utilizada: ChatGPT versión del 5 de septiembre del 2026
//Fecha: 5/9/2026
//Fecha de modificacion: 15/9/2026

class StoreViewModel: ViewModel(){
    private val profiles = BooksRepository.profiles
    private val initialBooks= BooksRepository.books
    private val catalogBooks = generateCatalog()
    private val _uiState = MutableStateFlow(
        StoreUiState(
            books = catalogBooks,
            profiles = profiles
        )
    )

    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private fun generateCatalog():List<Book>{
        val random =Random(20260915)
        val titles = listOf(
            "El secreto",
            "La última carta",
            "El viaje",
            "La casa",
            "Historias de",
            "Memorias de",
            "El camino",
            "La sombra",
            "El jardín",
            "Los recuerdos",
            "El misterio",
            "La aventura",
            "Crónicas de",
            "El destino",
            "La promesa",
            "El tiempo",
            "Los días de",
            "El retrato",
            "La biblioteca",
            "El legado"
        )

        val subjects = listOf(
            "Macondo",
            "la luna",
            "la montaña",
            "la ciudad",
            "los viajeros",
            "un verano",
            "la memoria",
            "una familia",
            "el océano",
            "la noche",
            "un antiguo reino",
            "los sueños",
            "la amistad",
            "el bosque",
            "la primavera",
            "una pequeña aldea",
            "el horizonte",
            "los secretos",
            "la libertad",
            "el tiempo"
        )


        val generatedBooks = (1..497).map{ index ->
            val title=titles.random(random)
            val subject= subjects.random (random)
            val profile = profiles.random(random)
            val stock = when(index%10){
                0->0
                1->3
                else -> random.nextInt(
                    from= 4,
                    until = 21
                )
            }
            Book(
                id = "generated-book-$index",
                name = "$title $subject",
                description = "Una historia relacionada con $subject, "
                        + "presentada en una edición de nuestra tienda. "
                        + "Perfil asociado: ${profile.name}",
                priceCents = random.nextInt(from=4_500, until=22_001),
                stock=stock,
                imageUrl = "https://picsum.photos/seed/generated-book-$index/400/400",
                profileId = profile.id
            )
        }

        return initialBooks + generatedBooks

    }



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

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}