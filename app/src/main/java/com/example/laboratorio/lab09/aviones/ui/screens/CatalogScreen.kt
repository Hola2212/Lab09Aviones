package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.ui.components.ScreenScaffold
import kotlinx.coroutines.launch
import com.example.laboratorio.lab09.aviones.model.formatQuetzales
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import com.example.laboratorio.lab09.aviones.ui.components.ProductImage
import androidx.compose.material3.TextButton
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
    totalCount: Int,
    favoriteBookIds: Set<String>,
    searchQuery: String,
    gridState: LazyGridState,
    onQueryChange: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenOrder: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val handleQueryChange: (String) -> Unit = { newQuery ->
        onQueryChange(newQuery)
        scope.launch {
            gridState.scrollToItem(0)
        }
    }

    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 2 } }

    ScreenScaffold(
        title = "Catálogo",
        modifier = modifier,
        floatingActionButton = {
            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = { scope.launch { gridState.animateScrollToItem(0) } }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Volver arriba"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpenOrder) {
                    Text("Ver mi pedido")
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = handleQueryChange,
                label = { Text("Buscar productos") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { handleQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                text = "${books.size} de $totalCount productos",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No encontramos productos.")
                        TextButton(onClick = { handleQueryChange("") }) {
                            Text("Limpiar búsqueda")
                        }
                    }
                }
            } else {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        BookCatalogItem(
                            book = book,
                            isFavorite = favoriteBookIds.contains(book.id),
                            onClick = { onBookClick(book.id) },
                            onToggleFavorite = { onToggleFavorite(book.id) },
                            modifier= Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCatalogItem(
    book: Book,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier=Modifier
) {
    DisposableEffect(book.id) {
        Log.d("CatalogProbe", "ENTER id=${book.id}")

        onDispose {
            Log.d("CatalogProbe", "EXIT id=${book.id}")
        }
    }
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column {
            ProductImage(
                imageUrl = book.imageUrl,
                contentDescription = "Portada de ${book.name}",
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatQuetzales(book.priceCents),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        contentDescription = if (isFavorite) {
                            "Quitar de favoritos"
                        } else {
                            "Agregar a favoritos"
                        }
                    )
                }
            }
        }

    }
}