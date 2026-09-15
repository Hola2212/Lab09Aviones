package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.Profile
import com.example.laboratorio.lab09.aviones.ui.components.ScreenScaffold

/**
 * Pantalla de Detalle — puramente presentacional (Paso 3).
 *
 * El único estado que maneja esta pantalla es efímero y local: mostrar
 * u ocultar la ficha técnica, mediante remember { mutableStateOf(...) }.
 * Ese estado NO vive en el ViewModel porque no es información de negocio,
 * es solo una decisión visual de esta pantalla.
 *
 * El libro, el perfil asociado y si es favorito llegan por parámetro;
 * esta pantalla nunca obtiene su propio ViewModel ni modifica el back stack.
 */
@Composable
fun BookDetailScreen(
    book: Book?,
    profile: Profile?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        title = book?.name ?: "Detalle",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
            }
        },
        modifier = modifier
    ) { padding ->
        if (book == null) {
            Text(
                text = "No se encontró el producto.",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@ScreenScaffold
        }

        // Estado efímero de UI: pertenece solo a esta pantalla, no al ViewModel.
        var showTechSheet by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(book.name, style = MaterialTheme.typography.headlineSmall)
            Text("Q${book.price}", style = MaterialTheme.typography.titleMedium)
            Text(book.description, style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos"
                    )
                }
                Text(if (isFavorite) "En tus favoritos" else "Agregar a favoritos")
            }

            TextButton(onClick = { showTechSheet = !showTechSheet }) {
                Text(if (showTechSheet) "Ocultar ficha técnica" else "Ver ficha técnica")
            }
            if (showTechSheet) {
                Text(
                    text = "Ficha técnica: edición estándar, disponible para entrega inmediata.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()

            if (profile != null) {
                Text("Autor: ${profile.name}", style = MaterialTheme.typography.titleSmall)
                Button(onClick = onOpenProfile) {
                    Text("Ver perfil del autor")
                }
            }
        }
    }
}