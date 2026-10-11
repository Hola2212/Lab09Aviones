package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.laboratorio.lab09.aviones.ui.state.MyOrdersUiState

// En este código usamos IA generativa como apoyo en el diseño del componente en Compose para resolver visualmente los estados de carga, lista, vacío y reintento.
// Autor: Raquel Vega
// IA utilizada: Gemini versión del 10 de octubre de 2026

@Composable
fun MyOrdersScreen(
    uiState: MyOrdersUiState,
    onRetry: () -> Unit,
    onOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is MyOrdersUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MyOrdersUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no has enviado pedidos.")
                }
            }
            is MyOrdersUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
            is MyOrdersUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.orders) { order ->
                        Card(
                            onClick = { onOrderClick(order.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Pedido #${order.id}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Fecha: ${order.date}")
                                Text(text = "Total: Q${order.amount}")
                            }
                        }
                    }
                }
            }
        }
    }
}