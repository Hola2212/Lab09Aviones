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
import com.example.laboratorio.lab09.aviones.model.OrderDto
import com.example.laboratorio.lab09.aviones.model.OrderLine
import com.example.laboratorio.lab09.aviones.model.OrderLineDto
import com.example.laboratorio.lab09.aviones.model.OrderReceipt
import com.example.laboratorio.lab09.aviones.model.OrderUpdateResult
import com.example.laboratorio.lab09.aviones.model.addToOrder
import com.example.laboratorio.lab09.aviones.model.calculateOrderTotalCents
import com.example.laboratorio.lab09.aviones.model.decreaseOrderQuantity as applyDecrease
import com.example.laboratorio.lab09.aviones.model.nowIsUTC
import com.example.laboratorio.lab09.aviones.model.removeFromOrder
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

    fun addBookToOrder(bookId: String, increment: Int = 1) {
        val current = uiState.value
        when (val result = addToOrder(current.books, current.orderLines, bookId, increment)) {
            is OrderUpdateResult.Rejected -> {
                _orderError.value = result.reason
                _orderConfirmation.value = null
            }
            is OrderUpdateResult.Success -> {
                _orderError.value = null
                _orderConfirmation.value = "Se agregó $increment unidad(es) al pedido."
                val line = result.updatedOrder.first { it.bookId == bookId }
                viewModelScope.launch {
                    cartAndFavoritesRepository.saveOrderLine(line.bookId, line.quantity)
                }
            }
        }
    }

    fun decreaseOrderQuantity(bookId: String) {
        val newOrder = applyDecrease(uiState.value.orderLines, bookId)
        val newLine = newOrder.firstOrNull { it.bookId == bookId }
        _orderError.value = null
        _orderConfirmation.value = null
        viewModelScope.launch {
            if (newLine == null) {
                cartAndFavoritesRepository.removeOrderLine(bookId)
            } else {
                cartAndFavoritesRepository.saveOrderLine(newLine.bookId, newLine.quantity)
            }
        }
    }

    fun removeBookFromOrder(bookId: String) {
        _orderError.value = null
        _orderConfirmation.value = null
        viewModelScope.launch {
            cartAndFavoritesRepository.removeOrderLine(bookId)
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

            when (val result = ordersRepository.createOrder(request)) {
                is NetworkResult.Success -> {
                    val saved = result.data
                    _lastReceipt.value = OrderReceipt(
                        folio = "#ORD-${saved.id.padStart(5, '0')}",
                        clientName = checkout.name,
                        billingType = checkout.billingType,
                        payMethod = checkout.payMethod,
                        totalCents = totalCents,
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