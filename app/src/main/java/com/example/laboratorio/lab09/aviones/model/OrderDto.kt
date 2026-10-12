package com.example.laboratorio.lab09.aviones.model
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
@Serializable

data class OrderLineDto(
    val productId: String,
    val name: String = "",
    val price: Int,
    val quantity: Int
)
@Serializable
data class OrderDto(
    val id: String = "",
    val total: Int,
    val amount: Int,
    val date: String,
    val lines: List<OrderLineDto>
)

//Definimos un formato fijo para la fecha
private val storageFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(
    ZoneOffset.UTC)
private val displayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
fun nowIsUTC():String = storageFormat.format(Instant.now())
fun formatOrderDate(iso:String): String =
    try { displayFormat.format(Instant.parse(iso))} catch (e: DateTimeException) { iso }
