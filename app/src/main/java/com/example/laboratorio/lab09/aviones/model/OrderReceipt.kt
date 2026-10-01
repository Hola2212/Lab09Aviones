package com.example.laboratorio.lab09.aviones.model

import com.example.laboratorio.lab09.aviones.ui.state.BillingType
import com.example.laboratorio.lab09.aviones.ui.state.PayMethod

/**
 * Recibo inmutable generado al confirmar una compra válida (Paso 4).
 */
data class OrderReceipt(
    val folio: String,
    val clientName: String,
    val billingType: BillingType,
    val payMethod: PayMethod,
    val totalCents: Int,
    val nit: String? = null,
    val fiscalName: String? = null
)