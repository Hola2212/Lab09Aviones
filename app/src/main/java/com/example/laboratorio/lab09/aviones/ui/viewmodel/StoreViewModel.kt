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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

class StoreViewModel(
    private val booksRepository: BooksRepository,
    private val cartAndFavoritesRepository: CartAndFavoritesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _booksStream = MutableStateFlow<List<Book>>(emptyList())
    private val _isCatalogLoading = MutableStateFlow(true)
    private val _catalogErrorMessage = MutableStateFlow<String?>(null)
    private val _orderError = MutableStateFlow<String?>(null)
    private val _orderConfirmation = MutableStateFlow<String?>(null)

    private val _reservingBookIds = MutableStateFlow<Set<String>>(emptySet())
    val reservingBookIds: StateFlow<Set<String>> = _reservingBookIds.asStateFlow()

    private val _concurrencyTestResult = MutableStateFlow<String?>(null)
    val concurrencyTestResult: StateFlow<String?> = _concurrencyTestResult.asStateFlow()

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
            val timeoutJob = launch {
                delay(5_000)
                if (_booksStream.value.isEmpty()) {
                    _isCatalogLoading.value = false
                    _catalogErrorMessage.value =
                        "No pudimos conectar con el catálogo. Revisa tu conexión."
                }
            }

            booksRepository.getBooksStream()
                .catch {
                    timeoutJob.cancel()
                    _isCatalogLoading.value = false
                    if (_booksStream.value.isEmpty()) {
                        _catalogErrorMessage.value =
                            "No pudimos cargar el catálogo. Revisa tu conexión."
                    }
                }
                .collect { books ->
                    timeoutJob.cancel()
                    _booksStream.value = books
                    _isCatalogLoading.value = false
                    _catalogErrorMessage.value = null
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
        if (bookId in _reservingBookIds.value) return
        val book = uiState.value.books.firstOrNull { it.id == bookId }
        if (book == null) {
            showOrderError("El producto solicitado no existe.")
            return
        }
        if (book.stock <= 0) {
            showOrderError("Este producto está agotado.")
            return
        }
        _orderError.value = null
        _orderConfirmation.value = null
        _reservingBookIds.update { it + bookId }
        viewModelScope.launch {
            booksRepository.reserveUnit(bookId)
                .onSuccess {
                    val quantity = uiState.value.unitsInOrder(bookId) + 1
                    cartAndFavoritesRepository.saveOrderLine(bookId, quantity)
                    _orderConfirmation.value = "Se agregó 1 unidad al pedido."
                }
                .onFailure { showOrderError(it.message.orEmpty()) }
            _reservingBookIds.update { it - bookId }
        }
    }

    fun decreaseOrderQuantity(bookId: String) {
        val line = uiState.value.orderLines.firstOrNull { it.bookId == bookId } ?: return
        releaseFromOrder(bookId, units = 1, remaining = line.quantity - 1)
    }

    fun removeBookFromOrder(bookId: String) {
        val line = uiState.value.orderLines.firstOrNull { it.bookId == bookId } ?: return
        releaseFromOrder(bookId, units = line.quantity, remaining = 0)
    }

    private fun releaseFromOrder(bookId: String, units: Int, remaining: Int) {
        if (bookId in _reservingBookIds.value) return
        _orderError.value = null
        _orderConfirmation.value = null
        _reservingBookIds.update { it + bookId }
        viewModelScope.launch {
            booksRepository.releaseUnits(bookId, units)
                .onSuccess { cartAndFavoritesRepository.saveOrderLine(bookId, remaining) }
                .onFailure { showOrderError(it.message.orEmpty()) }
            _reservingBookIds.update { it - bookId }
        }
    }

    private fun showOrderError(message: String) {
        _orderError.value = message
        _orderConfirmation.value = null
    }

    fun onConcurrencyTest(bookId: String, attempts: Int) {
        viewModelScope.launch {
            _concurrencyTestResult.value = "Ejecutando $attempts intentos..."
            val results = List(attempts) {
                async { booksRepository.reserveUnit(bookId) }
            }.awaitAll()
            val successes = results.count { it.isSuccess }
            _concurrencyTestResult.value =
                "Éxitos: $successes · Rechazos: ${attempts - successes}"
        }
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
        val checkout = _checkoutUiState.value
        val state = uiState.value
        if (!checkout.isFormCorrect || state.totalOrderUnits == 0) return
        if (_submitState.value is SubmitOrderState.Submitting) return

        _submitState.value = SubmitOrderState.Submitting
        viewModelScope.launch {
            val items = state.orderLines.mapNotNull { line ->
                state.books.firstOrNull { it.id == line.bookId }?.let { book ->
                    OrderItem(
                        productId = book.id,
                        name = book.name,
                        price = book.priceCents,
                        quantity = line.quantity
                    )
                }
            }

            when (val result = ordersRepository.createOrder(items)) {
                is NetworkResult.Success -> {
                    val saved = result.data
                    _lastReceipt.value = OrderReceipt(
                        folio = "#ORD-${saved.id.padStart(5, '0')}",
                        clientName = checkout.name,
                        billingType = checkout.billingType,
                        payMethod = checkout.payMethod,
                        totalCents = saved.total,
                        nit = if (checkout.billingType == BillingType.NIT) checkout.nit else null,
                        fiscalName = if (checkout.billingType == BillingType.NIT) checkout.fiscalName else null
                    )
                    cartAndFavoritesRepository.clearOrder()
                    _checkoutUiState.value = CheckoutUiState()
                    _submitState.value = SubmitOrderState.Sent
                }
                is NetworkResult.Error -> {
                    _submitState.value = SubmitOrderState.Failed(result.message)
                }
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