package com.example.laboratorio.lab09.aviones.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laboratorio.lab09.aviones.model.OrderReceipt
import com.example.laboratorio.lab09.aviones.model.formatQuetzales
import com.example.laboratorio.lab09.aviones.ui.components.ScreenScaffold
import com.example.laboratorio.lab09.aviones.ui.state.BillingType
import com.example.laboratorio.lab09.aviones.ui.state.PayMethod
import com.example.laboratorio.lab09.aviones.ui.viewmodel.StoreViewModel

@Composable
fun ConfirmationScreen(
    receipt: OrderReceipt?,
    onReturnToCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {

    ScreenScaffold(
        title = "Confirmación",
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            receipt?.let { order ->
                Text(
                    text = "¡Compra realizada con éxito!",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Folio: ${order.folio}", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Cliente: ${order.clientName}")
                        Text(
                            text = "Facturación: ${if (order.billingType == BillingType.CF) "Consumidor Final" else "NIT: ${order.nit} (${order.fiscalName})"}"
                        )
                        Text(
                            text = "Método de Pago: ${if (order.payMethod == PayMethod.CASH) "Efectivo contra entrega" else "Transferencia bancaria"}"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Total Pagado: ${formatQuetzales(order.totalCents)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                Button(
                    onClick = onReturnToCatalog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver al catálogo")
                }
            } ?: run {
                Text("No hay ningún recibo disponible.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onReturnToCatalog) {
                    Text("Volver al catálogo")
                }
            }
        }
    }
}
