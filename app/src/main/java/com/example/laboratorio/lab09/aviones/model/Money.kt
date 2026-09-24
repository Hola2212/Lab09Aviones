package com.example.laboratorio.lab09.aviones.model

fun formatQuetzales(amountCents: Int): String {
    val quetzales = amountCents / 100
    val cents = amountCents % 100

    return "Q$quetzales.${cents.toString().padStart(2, '0')}"
}