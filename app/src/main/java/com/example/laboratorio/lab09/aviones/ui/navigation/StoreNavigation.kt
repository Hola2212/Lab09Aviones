package com.example.laboratorio.lab09.aviones.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.laboratorio.lab09.aviones.ui.screens.BookDetailScreen
import com.example.laboratorio.lab09.aviones.ui.screens.CatalogScreen
import com.example.laboratorio.lab09.aviones.ui.screens.ProfileScreen
import com.example.laboratorio.lab09.aviones.ui.viewmodel.StoreViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface StoreNavKey: NavKey {
    @Serializable
    data object Catalog : StoreNavKey
    @Serializable
    data class Detail(val bookId: String) : StoreNavKey
    @Serializable
    data class Profile(val profileId: String) : StoreNavKey
}

@Composable
fun StoreNavigation(
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = viewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(StoreNavKey.Catalog)

    BackHandler(enabled = backStack.size > 1) { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryProvider = entryProvider {
            entry<StoreNavKey.Catalog> {
                CatalogScreen(
                    books = uiState.books,
                    favoriteBookIds = uiState.favoriteBookIds,
                    onBookClick = { bookId -> backStack.add(StoreNavKey.Detail(bookId)) },
                    onToggleFavorite = viewModel::changeFavorite
                )
            }

            entry<StoreNavKey.Detail> { key ->
                val book = uiState.books.firstOrNull { it.id == key.bookId }
                val profile = book?.let { current ->
                    uiState.profiles.firstOrNull { it.id == current.profileId}
                }
                BookDetailScreen(
                    book = book,
                    profile = profile,
                    isFavorite = (book != null && uiState.favoriteBookIds.contains(book.id)),
                    onToggleFavorite = { book?.let {viewModel.changeFavorite(it.id)}},
                    onOpenProfile = { profile?.let { backStack.add(StoreNavKey.Profile(it.id)) } },
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<StoreNavKey.Profile> { key ->
                val profile = uiState.profiles.firstOrNull { it.id == key.profileId }
                ProfileScreen(
                    profile = profile,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}