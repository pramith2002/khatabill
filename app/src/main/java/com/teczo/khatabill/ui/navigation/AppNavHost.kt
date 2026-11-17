package com.teczo.khatabill.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.billing.InvoiceDetailScreen
import com.teczo.khatabill.ui.billing.InvoiceListScreen
import com.teczo.khatabill.ui.billing.NewInvoiceScreen
import com.teczo.khatabill.ui.home.HomeScreen
import com.teczo.khatabill.ui.home.PlaceholderScreen
import com.teczo.khatabill.ui.language.LanguageOptions
import com.teczo.khatabill.ui.language.LanguagePickerScreen
import com.teczo.khatabill.ui.language.LanguageViewModel
import com.teczo.khatabill.ui.items.ItemDetailScreen
import com.teczo.khatabill.ui.items.ItemEditScreen
import com.teczo.khatabill.ui.items.ItemListScreen
import com.teczo.khatabill.ui.items.NEW_ITEM_ID
import com.teczo.khatabill.ui.onboarding.BusinessSetupScreen
import com.teczo.khatabill.ui.parties.NEW_ID
import com.teczo.khatabill.ui.parties.PartyEditScreen
import com.teczo.khatabill.ui.parties.PartyListScreen

@Composable
fun AppNavHost(
    languageViewModel: LanguageViewModel = hiltViewModel(),
    startViewModel: StartViewModel = hiltViewModel(),
) {
    val language by languageViewModel.language.collectAsStateWithLifecycle()
    val start by startViewModel.state.collectAsStateWithLifecycle()
    if (start == StartState.LOADING || language == null) {
        Box(Modifier.fillMaxSize()) // DataStore and database are still loading; a blink at most
        return
    }
    val currentLanguage = language.orEmpty()

    // The first screen is decided once; later state changes navigate explicitly (never reset the back stack).
    val startRoute = remember {
        when (start) {
            StartState.NEEDS_LANGUAGE -> Routes.LANGUAGE
            StartState.NEEDS_BUSINESS -> Routes.ONBOARDING
            else -> Routes.HOME
        }
    }
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (route in bottomTabs.map { it.route }) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LANGUAGE) {
                LanguagePickerScreen(onSelect = { tag ->
                    languageViewModel.select(tag)
                    navController.navigate(Routes.ONBOARDING) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
                })
            }
            composable(Routes.ONBOARDING) {
                BusinessSetupScreen(onFinished = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.PARTIES) {
                PartyListScreen(
                    onAdd = { navController.navigate(Routes.partyEdit(NEW_ID)) },
                    onOpen = { id -> navController.navigate(Routes.partyEdit(id)) },
                )
            }
            composable(Routes.PARTY_EDIT, arguments = listOf(navArgument("partyId") { type = NavType.StringType })) {
                PartyEditScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.BILLING) {
                InvoiceListScreen(
                    onNew = { type -> navController.navigate(Routes.invoiceNew(type.name)) },
                    onOpen = { id -> navController.navigate(Routes.invoiceDetail(id)) },
                )
            }
            composable(Routes.INVOICE_NEW, arguments = listOf(navArgument("type") { type = NavType.StringType })) {
                NewInvoiceScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.navigate(Routes.invoiceDetail(id)) { popUpTo(Routes.INVOICE_NEW) { inclusive = true } }
                    },
                )
            }
            composable(Routes.INVOICE_DETAIL, arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })) {
                InvoiceDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ITEMS) {
                ItemListScreen(
                    onAdd = { navController.navigate(Routes.itemEdit(NEW_ITEM_ID)) },
                    onOpen = { id -> navController.navigate(Routes.itemDetail(id)) },
                )
            }
            composable(Routes.ITEM_EDIT, arguments = listOf(navArgument("itemId") { type = NavType.StringType })) {
                ItemEditScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.ITEM_DETAIL, arguments = listOf(navArgument("itemId") { type = NavType.StringType })) {
                ItemDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.itemEdit(id)) },
                )
            }
            composable(Routes.MORE) {
                PlaceholderScreen(R.string.nav_more) {
                    Text(stringResource(R.string.language_section))
                    LanguageOptions(current = currentLanguage, onSelect = languageViewModel::select)
                }
            }
        }
    }
}
