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
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.example.laboratorio.lab09.aviones.model.OrderLine
import com.example.laboratorio.lab09.aviones.ui.data.OrderLineEntity
import kotlinx.coroutines.launch
import com.example.laboratorio.lab09.aviones.ui.data.FavoriteEntity
import com.example.laboratorio.lab09.aviones.ui.data.StoreDatabase
import kotlin.random.Random
import com.example.laboratorio.lab09.aviones.model.OrderReceipt
import com.example.laboratorio.lab09.aviones.model.validateFiscalName
import com.example.laboratorio.lab09.aviones.model.validateName
import com.example.laboratorio.lab09.aviones.model.validateNit
import com.example.laboratorio.lab09.aviones.model.validatePhoneNumber
import com.example.laboratorio.lab09.aviones.ui.state.BillingType
import com.example.laboratorio.lab09.aviones.ui.state.CheckoutUiState
import com.example.laboratorio.lab09.aviones.ui.state.PayMethod
import com.example.laboratorio.lab09.aviones.ui.data.StorePreferences
import kotlinx.coroutines.flow.map


// En este código usamos chatGPT con el fin de poder colocar descripción de los libros y generar los titulos y los subjects
//Autor: Abigail Escobar
//IA utilizada: ChatGPT versión del 5 de septiembre del 2026
//Fecha: 5/9/2026
//Fecha de modificacion: 15/9/2026

class StoreViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = StoreDatabase.get(app).dao()
    private val profiles = BooksRepository.profiles
    private val initialBooks= BooksRepository.books
    private val catalogBooks = generateCatalog()
    private val storePreferences = StorePreferences(app.applicationContext)
    private val _uiState = MutableStateFlow(
        StoreUiState(
            books = catalogBooks,
            profiles = profiles
        )
    )

    val uiState: StateFlow<StoreUiState> = combine(
        _uiState,
        dao.observeFavorites(),
        dao.observeOrderLines(),
        storePreferences.catalogOrderFlow
    ) { memory, favorites, lines, order ->
        val query = memory.searchQuery
        val filtered = if (query.isBlank()) {
            memory.books
        } else {
            memory.books.filter { it.name.contains(query, ignoreCase = true) }
        }
        val sortedBooks = when (order.lowercase()) {
            "price" -> filtered.sortedBy { it.priceCents }
            else -> filtered.sortedBy { it.name }
        }

        memory.copy(
            books = sortedBooks,
            favoriteBookIds = favorites.map { it.bookId }.toSet(),
            orderLines = lines.map { OrderLine(bookId = it.bookId, quantity = it.quantity) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _uiState.value)

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
        viewModelScope.launch {
            if (bookId in uiState.value.favoriteBookIds) {
                dao.deleteFavorite(bookId)
            } else {
                dao.upsertFavorite(FavoriteEntity(bookId))
            }
        }
    }

    fun addBookToOrder(bookId: String, increment: Int = 1) {
        val current = uiState.value
        when (val result = addToOrder(current.books, current.orderLines, bookId, increment)) {
            is OrderUpdateResult.Rejected -> {
                // Rechazada: se muestra el motivo y NO se escribe nada en Room.
                _uiState.update { it.copy(orderError = result.reason, orderConfirmation = null) }
            }
            is OrderUpdateResult.Success -> {
                _uiState.update {
                    it.copy(
                        orderError = null,
                        orderConfirmation = "Se agregó $increment unidad(es) al pedido."
                    )
                }
                val line = result.updatedOrder.first { it.bookId == bookId }
                viewModelScope.launch {
                    dao.upsertOrderLine(OrderLineEntity(line.bookId, line.quantity))
                }
            }
        }
    }
    fun decreaseOrderQuantity(bookId: String) {
        val newOrder = applyDecrease(uiState.value.orderLines, bookId)
        val newLine = newOrder.firstOrNull { it.bookId == bookId }
        _uiState.update { it.copy(orderError = null, orderConfirmation = null) }
        viewModelScope.launch {
            if (newLine == null) dao.deleteOrderLine(bookId)   // llegó a 0
            else dao.upsertOrderLine(OrderLineEntity(newLine.bookId, newLine.quantity))
        }
    }
    fun removeBookFromOrder(bookId: String) {
        _uiState.update { it.copy(orderError = null, orderConfirmation = null) }
        viewModelScope.launch { dao.deleteOrderLine(bookId) }
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

    // PASO 4: Confirmación de Orden, Recibo inmutable y Reinicio del Pedido
    fun confirmOrder() {
        val currentState = _checkoutUiState.value
        val currentOrderLines = uiState.value.orderLines
        val totalUnits = currentOrderLines.sumOf { it.quantity }

        if (currentState.isFormCorrect && totalUnits > 0) {
            // Generar folio secuencial (#ORD-00001)
            val folioFormatted = String.format("#ORD-%05d", orderCounter++)

            // Calcular Total en centavos
            val totalCents = currentOrderLines.sumOf { line ->
                val book = uiState.value.books.find { it.id == line.bookId }
                (book?.priceCents ?: 0) * line.quantity
            }

            // Recibo inmutable con cliente, facturación, metodo de pago y total
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

            // Vaciar el pedido a 0 unidades y reiniciar el formulario
            viewModelScope.launch { dao.clearOrder() }
            _checkoutUiState.value = CheckoutUiState()
        }
    }
    // Flujo expuesto para la UI
    val catalogOrder: StateFlow<String> = uiState.map { _uiState.value.searchQuery}

        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "NAME"
        )

    // Función para cambiar la preferencia desde la UI
    fun setCatalogOrder(order: String) {
        viewModelScope.launch {
            storePreferences.saveCatalogOrder(order)
        }
    }
}