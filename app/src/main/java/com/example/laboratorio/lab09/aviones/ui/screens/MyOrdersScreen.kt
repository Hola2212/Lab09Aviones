package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.laboratorio.lab09.aviones.model.formatOrderDate
import com.example.laboratorio.lab09.aviones.model.formatQuetzales
import com.example.laboratorio.lab09.aviones.ui.components.ScreenScaffold
import com.example.laboratorio.lab09.aviones.ui.state.OrdersUiState

@Composable
fun MyOrdersScreen(
    uiState: OrdersUiState,
    onRetry: () -> Unit,
    onOrderClick: (String) -> Unit,
    onGoToCatalog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        title = "Mis pedidos",
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is OrdersUiState.Loading -> CircularProgressIndicator()

                is OrdersUiState.Empty -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Sin pedidos", style = MaterialTheme.typography.titleMedium)
                    Text("Aún no has enviado pedidos.")
                    Button(onClick = onGoToCatalog) { Text("Ir al catálogo") }
                }

                is OrdersUiState.Error -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Sin conexión", style = MaterialTheme.typography.titleMedium)
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Reintentar") }
                }

                is OrdersUiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.orders, key = { it.id }) { order ->
                        Card(
                            onClick = { onOrderClick(order.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("#${order.id}", style = MaterialTheme.typography.titleMedium)
                                    Text(formatOrderDate(order.date), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(formatQuetzales(order.total), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}