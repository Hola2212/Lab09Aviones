package com.example.laboratorio.lab09.aviones.ui.viewmodel

import android.util.Log
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
import com.example.laboratorio.lab09.aviones.model.CatalogOrder
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
import com.example.laboratorio.lab09.aviones.model.sortBooks
import com.example.laboratorio.lab09.aviones.model.validateFiscalName
import com.example.laboratorio.lab09.aviones.model.validateName
import com.example.laboratorio.lab09.aviones.model.validateNit
import com.example.laboratorio.lab09.aviones.model.validatePhoneNumber
import com.example.laboratorio.lab09.aviones.ui.state.BillingType
import com.example.laboratorio.lab09.aviones.ui.state.CheckoutUiState
import com.example.laboratorio.lab09.aviones.ui.state.PayMethod
import com.example.laboratorio.lab09.aviones.ui.data.StorePreferences
import kotlinx.coroutines.flow.map
import com.example.laboratorio.lab09.aviones.ui.network.RetrofitClient
import com.example.laboratorio.lab09.aviones.model.OrderDto
import com.example.laboratorio.lab09.aviones.model.OrderLineDto
import com.example.laboratorio.lab09.aviones.model.calculateOrderTotalCents
import com.example.laboratorio.lab09.aviones.model.nowIsUTC
import com.example.laboratorio.lab09.aviones.ui.state.SubmitOrderState
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

import com.example.laboratorio.lab09.aviones.ui.data.CatalogSeeder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException


// En este código usamos chatGPT con el fin de poder colocar descripción de los libros y generar los titulos y los subjects
//Autor: Abigail Escobar
//IA utilizada: ChatGPT versión del 5 de septiembre del 2026
//Fecha: 5/9/2026
//Fecha de modificacion: 15/9/2026
//Fecha de modificacion: 10/10/2026
//Fecha de modificacion: 27/10/2026

class StoreViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = StoreDatabase.get(app).dao()
    private val profiles = BooksRepository.profiles
    private val initialBooks= BooksRepository.books
    private val catalogBooks = generateCatalog()
    private val storePreferences = StorePreferences(app.applicationContext)


    private val _canSeedCatalog = MutableStateFlow(true)
    val canSeedCatalog: StateFlow<Boolean> = _canSeedCatalog.asStateFlow()

    private val _seedMessage = MutableStateFlow("")
    val seedMessage: StateFlow<String> = _seedMessage.asStateFlow()

    fun seedCatalog() {
        if (!_canSeedCatalog.value) return

        if (FirebaseAuth.getInstance().currentUser == null) {
            _seedMessage.value =
                "La sesión todavía no está lista. Espera unos segundos."
            return
        }

        _canSeedCatalog.value = false
        _seedMessage.value = "Cargando los 500 productos…"

        viewModelScope.launch {
            try {
                CatalogSeeder.seed(catalogBooks)
                _seedMessage.value =
                    "Los cinco lotes fueron confirmados. Verifica Firebase Console."
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e("CatalogSeeder", "La carga se detuvo.", exception)
                _seedMessage.value =
                    "La carga se detuvo. Revisa Logcat y Firestore antes de reintentar."
            }
        }
    }

    private val _submitState = MutableStateFlow<SubmitOrderState>(SubmitOrderState.Idle)
    val submitState: StateFlow<SubmitOrderState> = _submitState.asStateFlow()
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

        memory.copy(
            books = sortBooks (memory.books, order),
            favoriteBookIds = favorites.map { it.bookId }.toSet(),
            orderLines = lines.map { OrderLine(bookId = it.bookId, quantity = it.quantity) },
            catalogOrder = order,
            isLoaded = true
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
        val checkout = _checkoutUiState.value
        val state= uiState.value
        if (!checkout.isFormCorrect || state.totalOrderUnits == 0) return
        if (_submitState.value is SubmitOrderState.Submitting) return

        _submitState.value = SubmitOrderState.Submitting
        viewModelScope.launch{
            val totalCents = calculateOrderTotalCents(state.books, state.orderLines)
            val lines = state.orderLines.mapNotNull { line ->
                state.books.firstOrNull { it.id == line.bookId }?.let { book ->
                    OrderLineDto(
                        productId = book.id,
                        name = book.name,
                        price = book.priceCents,
                        quantity = line.quantity
                    )
                }
            }
            val request = OrderDto(
                total = totalCents,
                amount = state.totalOrderUnits,
                date = nowIsUTC(),
                lines = lines
            )
            try {
                val saved = RetrofitClient.apiService.createOrder(request)
                _lastReceipt.value = OrderReceipt(
                    folio = "#ORD-${saved.id.padStart(5, '0')}",
                    clientName = checkout.name,
                    billingType = checkout.billingType,
                    payMethod = checkout.payMethod,
                    totalCents = totalCents,
                    nit = if (checkout.billingType == BillingType.NIT) checkout.nit else null,
                    fiscalName = if (checkout.billingType == BillingType.NIT) checkout.fiscalName else null
                )
                // Solo tras la respuesta del servidor se vacía el pedido y el formulario.
                dao.clearOrder()
                _checkoutUiState.value = CheckoutUiState()
                _submitState.value = SubmitOrderState.Sent
            } catch (e: IOException) {
                Log.e("StoreViewModel", "Sin conexión al enviar", e)
                _submitState.value = SubmitOrderState.Failed(
                    "No pudimos enviar tu pedido. Revisa tu conexión."
                )
            } catch (e: HttpException) {
                Log.e("StoreViewModel", "HTTP ${e.code()} al enviar", e)
                _submitState.value = SubmitOrderState.Failed(
                    "El servidor no pudo recibir tu pedido. Intenta de nuevo."
                )
            } catch (e: SerializationException) {
                Log.e("StoreViewModel", "JSON inesperado al enviar", e)
                _submitState.value = SubmitOrderState.Failed(
                    "Recibimos una respuesta inesperada del servidor."
                )
            }
        }
    }

    fun onOrderSentHandled() {
        if (_submitState.value is SubmitOrderState.Sent) _submitState.value = SubmitOrderState.Idle
    }

    fun dismissSubmitError() {
        if (_submitState.value is SubmitOrderState.Failed) _submitState.value = SubmitOrderState.Idle
    }

    // Función para cambiar la preferencia desde la UI
    fun setCatalogOrder(order: CatalogOrder) {
        viewModelScope.launch {
            storePreferences.saveCatalogOrder(order.name)
        }
    }
}