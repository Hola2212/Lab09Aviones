package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile
import com.example.laboratorio.lab09.aviones.ui.theme.Lab09AvionesTheme

private val fakeBook = Book(id = "b1", name = "Producto de prueba", description = "Descripción larga de prueba", price = 99.0, profileId = "p1")
private val fakeProfile = Profile(id = "p1", name = "Perfil de prueba", role = "Rol", location = "Ubicación", description = "Bio de prueba")

@Preview(showBackground = true)
@Composable
fun CatalogScreenPreview() {
    var favorites by remember { mutableStateOf(setOf<String>()) }
    Lab09AvionesTheme {
        CatalogScreen(
            books = listOf(fakeBook, fakeBook.copy(id = "b2", name = "Producto 2"), fakeBook.copy(id = "b3", name = "Producto 3")),
            favoriteBookIds = favorites,
            onBookClick = {},
            onToggleFavorite = { id -> favorites = if (id in favorites) favorites - id else favorites + id }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookDetailScreenPreview() {
    var isFav by remember { mutableStateOf(false) }
    Lab09AvionesTheme {
        BookDetailScreen(
            book = fakeBook,
            profile = fakeProfile,
            isFavorite = isFav,
            onToggleFavorite = { isFav = !isFav },
            onOpenProfile = {},
            onBack = {}
        )
    }
}