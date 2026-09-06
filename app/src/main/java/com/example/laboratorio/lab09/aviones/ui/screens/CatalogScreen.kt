package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.ui.components.ScreenScaffold

/**
 * Pantalla de Catálogo — puramente presentacional (Paso 3).
 *
 * No crea su propia lista de libros, no guarda otra copia de los
 * favoritos y no conoce el back stack. Solo recibe datos por parámetro
 * y notifica eventos hacia arriba mediante callbacks.
 *
 * Responsable: ViewModel (datos y favoritos) / Navigation 3 (qué se ve).
 */
@Composable
fun CatalogScreen(
    books: List<Book>,
    favoriteBookIds: Set<String>,
    onBookClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        title = "Catálogo",
        modifier = modifier
        // Sin navigationIcon: la pantalla raíz no lleva botón de retroceso.
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            books.forEach { book ->
                BookCatalogItem(
                    book = book,
                    isFavorite = favoriteBookIds.contains(book.id),
                    onClick = { onBookClick(book.id) },
                    onToggleFavorite = { onToggleFavorite(book.id) }
                )
            }
        }
    }
}

@Composable
private fun BookCatalogItem(
    book: Book,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(book.name, style = MaterialTheme.typography.titleMedium)
                Text("Q${book.price}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos"
                )
            }
        }
    }
}