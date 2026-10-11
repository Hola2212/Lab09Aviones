package com.example.laboratorio.lab09.aviones.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.OrderUpdateResult
import com.example.laboratorio.lab09.aviones.model.addToOrder
import com.example.laboratorio.lab09.aviones.model.decreaseOrderQuantity as applyDecrease
import com.example.laboratorio.lab09.aviones.model.removeFromOrder
import com.example.laboratorio.lab09.aviones.ui.data.BooksRepository
import com.example.laboratorio.lab09.aviones.ui.state.StoreUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import com.example.laboratorio.lab09.aviones.model.OrderReceipt
import com.example.laboratorio.lab09.aviones.model.validateFiscalName
import com.example.laboratorio.lab09.aviones.model.validateName
import com.example.laboratorio.lab09.aviones.model.validateNit
import com.example.laboratorio.lab09.aviones.model.validatePhoneNumber
import com.example.laboratorio.lab09.aviones.ui.state.BillingType
import com.example.laboratorio.lab09.aviones.ui.state.CheckoutUiState
import com.example.laboratorio.lab09.aviones.ui.state.PayMethod
import com.example.laboratorio.lab09.aviones.ui.network.RetrofitClient
import com.example.laboratorio.lab09.aviones.ui.state.MyOrdersUiState
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


// En este código usamos chatGPT con el fin de poder colocar descripción de los libros y generar los titulos y los subjects
//Autor: Abigail Escobar
//IA utilizada: ChatGPT versión del 5 de septiembre del 2026
//Fecha: 5/9/2026
//Fecha de modificacion: 15/9/2026
//Fecha de modificacion: 10/10/2026

class StoreViewModel: ViewModel(){
    private val profiles = BooksRepository.profiles
    private val initialBooks= BooksRepository.books
    private val catalogBooks = generateCatalog()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
    private val _uiState = MutableStateFlow(
        StoreUiState(
            books = catalogBooks,
            profiles = profiles
        )
    )

    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private fun generateCatalog():List<Book>{
        val random =Random(20260915)
        val titles = listOf(
            "El secreto",
            "La última carta",
            "El viaje",
            "La casa",
            "Historias de",
            "Memorias de",
            "El camino",
            "La sombra",
            "El jardín",
            "Los recuerdos",
            "El misterio",
            "La aventura",
            "Crónicas de",
            "El destino",
            "La promesa",
            "El tiempo",
            "Los días de",
            "El retrato",
            "La biblioteca",
            "El legado"
        )

        val subjects = listOf(
            "Macondo",
            "la luna",
            "la montaña",
            "la ciudad",
            "los viajeros",
            "un verano",
            "la memoria",
            "una familia",
            "el océano",
            "la noche",
            "un antiguo reino",
            "los sueños",
            "la amistad",
            "el bosque",
            "la primavera",
            "una pequeña aldea",
            "el horizonte",
            "los secretos",
            "la libertad",
            "el tiempo"
        )


        val generatedBooks = (1..497).map{ index ->
            val title=titles.random(random)
            val subject= subjects.random (random)
            val profile = profiles.random(random)
            val stock = when(index%10){
                0->0
                1->3
                else -> random.nextInt(
                    from= 4,
                    until = 21
                )
            }
            Book(
                id = "generated-book-$index",
                name = "$title $subject",
                description = "Una historia relacionada con $subject, "
                        + "presentada en una edición de nuestra tienda. "
                        + "Perfil asociado: ${profile.name}",
                priceCents = random.nextInt(from=4_500, until=22_001),
                stock=stock,
                imageUrl = "https://picsum.photos/seed/generated-book-$index/400/400",
                profileId = profile.id
            )
        }

        return initialBooks + generatedBooks

    }



    fun changeFavorite(bookId: String) {
        _uiState.update { currentState ->
            val newFavorites = if (currentState.favoriteBookIds.contains(bookId)) {
                currentState.favoriteBookIds - bookId
            } else {
                currentState.favoriteBookIds + bookId
            }
            currentState.copy(favoriteBookIds = newFavorites)
        }
    }

    fun addBookToOrder(bookId: String, increment: Int = 1) {
        _uiState.update { current ->
            when (val result = addToOrder(current.books, current.orderLines, bookId, increment)) {
                is OrderUpdateResult.Success -> current.copy(
                    orderLines = result.updatedOrder,
                    orderError = null,
                    orderConfirmation = "Se agregó $increment unidad(es) al pedido."
                )
                is OrderUpdateResult.Rejected -> current.copy(
                    orderError = result.reason,
                    orderConfirmation = null
                )
            }
        }
    }
    fun decreaseOrderQuantity(bookId: String) {
        _uiState.update { current ->
            current.copy(
                orderLines = applyDecrease(current.orderLines, bookId),
                orderError = null,
                orderConfirmation = null
            )
        }
    }
    fun removeBookFromOrder(bookId: String) {
        _uiState.update { current ->
            current.copy(
                orderLines = removeFromOrder(current.orderLines, bookId),
                orderError = null,
                orderConfirmation = null
            )
        }
    }
    fun clearOrderFeedback() {
        _uiState.update { it.copy(orderError = null, orderConfirmation = null) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }


    // PASO 3 Y 4: CHECKOUT Y FACTURACIÓN DINÁMICA

    private val _checkoutUiState = MutableStateFlow(CheckoutUiState())
    val checkoutUiState: StateFlow<CheckoutUiState> = _checkoutUiState.asStateFlow()

    private val _lastReceipt = MutableStateFlow<OrderReceipt?>(null)
    val lastReceipt: StateFlow<OrderReceipt?> = _lastReceipt.asStateFlow()

    // Contador determinista para el folio (#ORD-00001, #ORD-00002)
    private var orderCounter = 1

    fun onNameChange(newName: String) {
        _checkoutUiState.update { state ->
            state.copy(
                name = newName,
                nameError = validateName(newName),
                nameTouched = true
            )
        }
    }

    fun onPhoneChange(newPhone: String) {
        _checkoutUiState.update { state ->
            state.copy(
                phone = newPhone,
                phoneError = validatePhoneNumber(newPhone),
                phoneTouched = true
            )
        }
    }

    // PASO 3: Facturación Condicional y Limpieza en Cascada
    fun onBillingTypeChange(newType: BillingType) {
        _checkoutUiState.update { state ->
            val isCF = newType == BillingType.CF

            // Si se cambia a CF, limpiamos errores y toque de NIT y Nombre Fiscal
            state.copy(
                billingType = newType,
                nitError = if (isCF) null else validateNit(state.nit),
                nitTouched = if (isCF) false else state.nitTouched,
                fiscalNameError = if (isCF) null else validateFiscalName(state.fiscalName),
                fiscalNameTouched = if (isCF) false else state.fiscalNameTouched
            )
        }
    }

    fun onNitChange(newNit: String) {
        _checkoutUiState.update { state ->
            state.copy(
                nit = newNit,
                nitError = validateNit(newNit),
                nitTouched = true
            )
        }
    }

    fun onFiscalNameChange(newFiscalName: String) {
        _checkoutUiState.update { state ->
            state.copy(
                fiscalName = newFiscalName,
                fiscalNameError = validateFiscalName(newFiscalName),
                fiscalNameTouched = true
            )
        }
    }

    fun onPayMethodChange(newMethod: PayMethod) {
        _checkoutUiState.update { state ->
            state.copy(payMethod = newMethod)
        }
    }

    private val _ordersState = MutableStateFlow<MyOrdersUiState>(MyOrdersUiState.Loading)
    val ordersState: StateFlow<MyOrdersUiState> = _ordersState.asStateFlow()

    fun loadOrders() {
        viewModelScope.launch {
            _ordersState.value = MyOrdersUiState.Loading
            _ordersState.value = try {
                val fetchedOrders = RetrofitClient.apiService.getOrders()
                if (fetchedOrders.isEmpty()) {
                    MyOrdersUiState.Empty
                } else {
                    MyOrdersUiState.Success(fetchedOrders)
                }
            } catch (e: java.io.IOException) {
                MyOrdersUiState.Error("Revisa tu conexión e intenta de nuevo.")
            } catch (e: retrofit2.HttpException) {
                MyOrdersUiState.Error("El servidor no pudo atender la solicitud.")
            } catch (e: kotlinx.serialization.SerializationException) {
                MyOrdersUiState.Error("Recibimos una respuesta inesperada.")
            }
        }
    }

    // PASO 4: Confirmación de Orden, Recibo inmutable y Reinicio del Pedido
    fun confirmOrder(onSuccess: () -> Unit) {
        val currentState = _checkoutUiState.value
        val currentOrderLines = _uiState.value.orderLines
        val totalUnits = currentOrderLines.sumOf { it.quantity }

        if (currentState.isFormCorrect && totalUnits > 0 && !_isSubmitting.value) {
            viewModelScope.launch {
                _isSubmitting.value = true
                try {
                    // 1. Mapear tus líneas de pedido al DTO que espera MockAPI
                    val orderLinesDto = currentOrderLines.map {
                        com.example.laboratorio.lab09.aviones.model.OrderLine(
                            bookId = it.bookId,
                            quantity = it.quantity
                        )
                    }

                    // 2. Total en centavos
                    val totalCents = currentOrderLines.sumOf { line ->
                        val book = _uiState.value.books.find { it.id == line.bookId }
                        (book?.priceCents ?: 0) * line.quantity
                    }

                    // 3. Crear el objeto para el POST (sin id: MockAPI lo asigna)
                    val newOrder = com.example.laboratorio.lab09.aviones.model.MyOrders(
                        total = totalCents,
                        amount = totalCents / 100,
                        date = java.time.LocalDate.now().toString(),
                        lines = orderLinesDto
                    )

                    // 4. Llamada a Retrofit con POST
                    val responseOrder = com.example.laboratorio.lab09.aviones.ui.network.RetrofitClient.apiService.createOrder(newOrder)

                    // 5. Recibo con el ID real devuelto por el servidor
                    val folioFormatted = "#ORD-${responseOrder.id.padStart(5, '0')}"

                    val receipt = OrderReceipt(
                        folio = folioFormatted,
                        clientName = currentState.name,
                        billingType = currentState.billingType,
                        payMethod = currentState.payMethod,
                        totalCents = totalCents,
                        nit = if (currentState.billingType == BillingType.NIT) currentState.nit else null,
                        fiscalName = if (currentState.billingType == BillingType.NIT) currentState.fiscalName else null
                    )

                    _lastReceipt.value = receipt

                    // 6. Vaciar el pedido y reiniciar el formulario SOLO TRAS EL ÉXITO
                    _uiState.update { it.copy(orderLines = emptyList()) }
                    _checkoutUiState.value = CheckoutUiState()

                    onSuccess()

                } catch (e: Exception) {
                    android.util.Log.e("StoreViewModel", "Error al crear la orden", e)
                } finally {
                    _isSubmitting.value = false
                }
            }
        }

    }
}