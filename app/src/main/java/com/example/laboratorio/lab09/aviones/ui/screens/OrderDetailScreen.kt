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
import com.example.laboratorio.lab09.aviones.ui.state.OrderDetailUiState

@Composable
fun OrderDetailScreen(
    uiState: OrderDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onBackToOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        title = "Detalle del pedido",
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
                is OrderDetailUiState.Loading -> CircularProgressIndicator()

                is OrderDetailUiState.NotFound -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Pedido no encontrado", style = MaterialTheme.typography.titleMedium)
                    Text("Este pedido ya no existe en el servidor.")
                    Button(onClick = onBackToOrders) { Text("Volver a Mis pedidos") }
                }

                is OrderDetailUiState.Error -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Reintentar") }
                }

                is OrderDetailUiState.Success -> {
                    val order = uiState.order
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Pedido #${order.id}", style = MaterialTheme.typography.headlineSmall)
                            Text("Fecha: ${formatOrderDate(order.date)}")
                            Spacer(Modifier.height(8.dp))
                        }
                        items(order.lines) { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${line.name.ifBlank { line.productId }} (x${line.quantity})",
                                    modifier = Modifier.weight(1f)
                                )
                                Text(formatQuetzales(line.price * line.quantity))
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Total: ${formatQuetzales(order.total)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}