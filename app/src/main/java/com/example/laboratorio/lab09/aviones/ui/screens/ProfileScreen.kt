package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.laboratorio.lab09.aviones.model.Profile
import com.example.laboratorio.lab09.aviones.ui.components.ScreenScaffold

/**
 * Pantalla de Perfil asociado — puramente presentacional (Paso 3).
 *
 * Muestra al fabricante/autor/vendedor asociado al producto: nombre, rol,
 * ubicación y descripción. No crea su propia lista de perfiles, no guarda
 * otra copia de datos de negocio, no obtiene su propio ViewModel y no
 * modifica el back stack directamente: solo notifica hacia arriba mediante
 * el callback onBack cuando el usuario quiere regresar.
 */
@Composable
fun ProfileScreen(
    profile: Profile?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        title = profile?.name ?: "Perfil",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
            }
        },
        modifier = modifier
    ) { padding ->
        if (profile == null) {
            Text(
                text = "No se encontró el perfil.",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@ScreenScaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(profile.name, style = MaterialTheme.typography.headlineSmall)
            Text(profile.role, style = MaterialTheme.typography.titleMedium)

            HorizontalDivider()

            Text("Ubicación", style = MaterialTheme.typography.titleSmall)
            Text(profile.location, style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()

            Text("Acerca de", style = MaterialTheme.typography.titleSmall)
            Text(profile.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}