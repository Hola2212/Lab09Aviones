package com.example.laboratorio.lab09.aviones.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.laboratorio.lab09.aviones.StoreApplication
import com.example.laboratorio.lab09.aviones.data.repository.BooksRepository
import com.example.laboratorio.lab09.aviones.data.repository.CartAndFavoritesRepository
import com.example.laboratorio.lab09.aviones.data.repository.NetworkResult
import com.example.laboratorio.lab09.aviones.data.repository.OrdersRepository
import com.example.laboratorio.lab09.aviones.data.repository.UserPreferencesRepository
import com.example.laboratorio.lab09.aviones.model.Book
import com.example.laboratorio.lab09.aviones.model.CatalogOrder
import com.example.laboratorio.lab09.aviones.model.OrderLine
import com.example.laboratorio.lab09.aviones.model.OrderItem
import com.example.laboratorio.lab09.aviones.model.OrderReceipt
import com.example.laboratorio.lab09.aviones.model.sortBooks
import com.example.laboratorio.lab09.aviones.model.validateFiscalName
import com.example.laboratorio.lab09.aviones.model.validateName
import com.example.laboratorio.lab09.aviones.model.validateNit
import com.example.laboratorio.lab09.aviones.model.validatePhoneNumber
import com.example.laboratorio.lab09.aviones.ui.state.BillingType
import com.example.laboratorio.lab09.aviones.ui.state.CheckoutUiState
import com.example.laboratorio.lab09.aviones.ui.state.PayMethod
import com.example.laboratorio.lab09.aviones.ui.state.StoreUiState
import com.example.laboratorio.lab09.aviones.ui.state.SubmitOrderState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class StoreViewModel(
    private val booksRepository: BooksRepository,
    private val cartAndFavoritesRepository: CartAndFavoritesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private var requiresOrderReview = false
    private val _searchQuery = MutableStateFlow("")
    private val _booksStream = MutableStateFlow<List<Book>>(emptyList())
    private val _isCatalogLoading = MutableStateFlow(true)
    private val _catalogErrorMessage = MutableStateFlow<String?>(null)
    private val _orderError = MutableStateFlow<String?>(null)
    private val _orderConfirmation = MutableStateFlow<String?>(null)

    private val _reservingBookIds = MutableStateFlow<Set<String>>(emptySet())
    val reservingBookIds: StateFlow<Set<String>> = _reservingBookIds.asStateFlow()

    private val _checkoutUiState = MutableStateFlow(CheckoutUiState())
    val checkoutUiState: StateFlow<CheckoutUiState> = _checkoutUiState.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitOrderState>(SubmitOrderState.Idle)
    val submitState: StateFlow<SubmitOrderState> = _submitState.asStateFlow()

    private val _lastReceipt = MutableStateFlow<OrderReceipt?>(null)
    val lastReceipt: StateFlow<OrderReceipt?> = _lastReceipt.asStateFlow()

    private var catalogJob: Job? = null

    init {
        loadCatalog()
    }

    private data class DataState(
        val books: List<Book>,
        val favorites: Set<String>,
        val lines: List<OrderLine>,
        val order: CatalogOrder
    )

    private data class UiExtraState(
        val query: String,
        val orderError: String?,
        val orderConfirmation: String?,
        val isLoading: Boolean,
        val errorMessage: String?
    )

    val uiState: StateFlow<StoreUiState> = combine(
        combine(
            _booksStream,
            cartAndFavoritesRepository.favoritesStream,
            cartAndFavoritesRepository.orderLinesStream,
            userPreferencesRepository.catalogOrderStream
        ) { books, favs, lines, order ->
            DataState(books, favs, lines, order)
        },
        combine(
            _searchQuery,
            _orderError,
            _orderConfirmation,
            _isCatalogLoading,
            _catalogErrorMessage
        ) { query, err, conf, loading, errMsg ->
            UiExtraState(query, err, conf, loading, errMsg)
        }
    ) { dataState, extraState ->
        val sorted = sortBooks(dataState.books, dataState.order)
        StoreUiState(
            books = sorted,
            profiles = booksRepository.getProfiles(),
            favoriteBookIds = dataState.favorites,
            searchQuery = extraState.query,
            orderLines = dataState.lines,
            orderError = extraState.orderError,
            orderConfirmation = extraState.orderConfirmation,
            catalogOrder = dataState.order,
            isLoaded = !extraState.isLoading || dataState.books.isNotEmpty(),
            isCatalogLoading = extraState.isLoading,
            catalogErrorMessage = extraState.errorMessage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StoreUiState(profiles = booksRepository.getProfiles())
    )

    fun retryCatalog() {
        loadCatalog()
    }

    private fun loadCatalog() {
        catalogJob?.cancel()
        _isCatalogLoading.value = true
        _catalogErrorMessage.value = null

        catalogJob = viewModelScope.launch {
            var receivedSnapshot = false

            val timeoutJob = launch {
                delay(5_000)

                if (!receivedSnapshot) {
                    _isCatalogLoading.value = false
                    _catalogErrorMessage.value =
                        "No se pudo actualizar el catálogo. " +
                                "Revisa tu conexión e intenta nuevamente."
                }
            }

            try {
                booksRepository.getBooksStream()
                    .catch { exception ->
                        timeoutJob.cancel()

                        Log.e(
                            "StoreViewModel",
                            "Falló la lectura del catálogo.",
                            exception
                        )

                        _isCatalogLoading.value = false
                        _catalogErrorMessage.value =
                            "No se pudo actualizar el catálogo. " +
                                    "Los datos mostrados pueden estar desactualizados."
                    }
                    .collect { books ->
                        receivedSnapshot = true
                        timeoutJob.cancel()
                        _booksStream.value = books
                        _isCatalogLoading.value = false
                        _catalogErrorMessage.value = null
                    }
            } finally {
                timeoutJob.cancel()
            }
        }
    }

    fun changeFavorite(bookId: String) {
        viewModelScope.launch {
            val isFav = bookId in uiState.value.favoriteBookIds
            cartAndFavoritesRepository.toggleFavorite(bookId, isFav)
        }
    }

    fun addBookToOrder(bookId: String) {
        updateOrderQuantity(bookId) { currentQuantity ->
            currentQuantity + 1
        }
    }

    fun decreaseOrderQuantity(bookId: String) {
        updateOrderQuantity(bookId) { currentQuantity ->
            (currentQuantity - 1).coerceAtLeast(0)
        }
    }

    fun removeBookFromOrder(bookId: String) {
        updateOrderQuantity(bookId) { 0 }
    }

    private fun updateOrderQuantity(
        bookId: String,
        calculateQuantity: (Int) -> Int
    ) {
        if (requiresOrderReview) {
            showOrderError(
                "El pedido necesita revisión por una operación incompleta. " +
                        "No se modificarán más existencias."
            )
            return
        }
        if (_submitState.value is SubmitOrderState.Submitting) return
        if (bookId in _reservingBookIds.value) return

        _orderError.value = null
        _orderConfirmation.value = null
        _reservingBookIds.update { it + bookId }

        viewModelScope.launch {
            var stockChanged = false

            try {
                val currentQuantity =
                    cartAndFavoritesRepository.getOrderQuantity(bookId)

                val newQuantity = calculateQuantity(currentQuantity)
                val difference = newQuantity - currentQuantity

                if (difference == 0) return@launch

                if (difference > 0) {
                    check(difference == 1) {
                        "Solo se puede reservar una unidad por operación."
                    }
                    booksRepository.reserveUnit(bookId).getOrThrow()
                } else {
                    booksRepository.releaseUnits(
                        bookId = bookId,
                        units = -difference
                    ).getOrThrow()
                }

                stockChanged = true

                cartAndFavoritesRepository.saveOrderLine(
                    bookId = bookId,
                    quantity = newQuantity
                )

                _orderConfirmation.value = if (difference > 0) {
                    "Se agregó 1 unidad al pedido."
                } else {
                    "Se actualizó la cantidad del pedido."
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    "StoreViewModel",
                    "No se pudo actualizar el pedido: bookId=$bookId",
                    exception
                )
                if (stockChanged) {
                    requiresOrderReview = true
                }
                val message = if (stockChanged) {
                    "Las existencias cambiaron, pero no se pudo guardar " +
                            "el pedido. No repitas la operación sin revisar los datos."
                } else {
                    exception.message ?: "No se pudo actualizar el pedido."
                }

                showOrderError(message)
            } finally {
                _reservingBookIds.update { it - bookId }
            }
        }
    }

    private fun showOrderError(message: String) {
        _orderError.value = message
        _orderConfirmation.value = null
    }


    fun clearOrderFeedback() {
        _orderError.value = null
        _orderConfirmation.value = null
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setCatalogOrder(order: CatalogOrder) {
        viewModelScope.launch {
            userPreferencesRepository.saveCatalogOrder(order)
        }
    }

    // CHECKOUT Y FACTURACIÓN DINÁMICA
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

    fun onBillingTypeChange(newType: BillingType) {
        _checkoutUiState.update { state ->
            val isCF = newType == BillingType.CF
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

    fun confirmOrder() {
        if (_submitState.value is SubmitOrderState.Submitting) return

        if (requiresOrderReview) {
            _submitState.value = SubmitOrderState.Failed(
                "El pedido necesita revisión por una operación incompleta."
            )
            return
        }

        if (_reservingBookIds.value.isNotEmpty()) {
            _submitState.value = SubmitOrderState.Failed(
                "Espera a que terminen las reservas antes de confirmar."
            )
            return
        }

        val checkout = _checkoutUiState.value
        if (!checkout.isFormCorrect) return

        _submitState.value = SubmitOrderState.Submitting

        viewModelScope.launch {
            var remoteOrderCreated = false

            try {
                val lines = cartAndFavoritesRepository.orderLinesStream.first()

                check(lines.isNotEmpty()) {
                    "El pedido está vacío."
                }

                val booksById = _booksStream.value.associateBy { it.id }

                val items = lines.map { line ->
                    check(line.quantity > 0) {
                        "El pedido contiene una cantidad inválida."
                    }

                    val book = booksById[line.bookId]
                        ?: error(
                            "Un producto del pedido no está disponible en el catálogo. " +
                                    "Revisa el pedido antes de enviarlo."
                        )

                    OrderItem(
                        productId = book.id,
                        name = book.name,
                        price = book.priceCents,
                        quantity = line.quantity
                    )
                }

                when (val result = ordersRepository.createOrder(items)) {
                    is NetworkResult.Success -> {
                        remoteOrderCreated = true
                        val saved = result.data

                        _lastReceipt.value = OrderReceipt(
                            folio = "#ORD-${saved.id.padStart(5, '0')}",
                            clientName = checkout.name,
                            billingType = checkout.billingType,
                            payMethod = checkout.payMethod,
                            totalCents = saved.total,
                            nit = if (checkout.billingType == BillingType.NIT) {
                                checkout.nit
                            } else {
                                null
                            },
                            fiscalName = if (
                                checkout.billingType == BillingType.NIT
                            ) {
                                checkout.fiscalName
                            } else {
                                null
                            }
                        )

                        cartAndFavoritesRepository.clearOrder()
                        _checkoutUiState.value = CheckoutUiState()
                        _submitState.value = SubmitOrderState.Sent
                    }

                    is NetworkResult.Error -> {
                        _submitState.value =
                            SubmitOrderState.Failed(result.message)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    "StoreViewModel",
                    "No se pudo completar la confirmación.",
                    exception
                )

                if (remoteOrderCreated) {
                    requiresOrderReview = true
                }

                _submitState.value = SubmitOrderState.Failed(
                    if (remoteOrderCreated) {
                        "El pedido fue enviado, pero falló la limpieza local. " +
                                "No lo envíes nuevamente. Revisa Mis pedidos."
                    } else {
                        exception.message ?: "No se pudo confirmar el pedido."
                    }
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StoreApplication)
                val container = app.container
                StoreViewModel(
                    booksRepository = container.booksRepository,
                    cartAndFavoritesRepository = container.cartAndFavoritesRepository,
                    userPreferencesRepository = container.userPreferencesRepository,
                    ordersRepository = container.ordersRepository
                )
            }
        }
    }
}