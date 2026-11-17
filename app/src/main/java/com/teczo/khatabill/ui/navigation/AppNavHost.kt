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
import com.teczo.khatabill.ui.components.AppBottomBar
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
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.ui.billing.UpiQrScreen
import com.teczo.khatabill.ui.books.BookViewModel
import com.teczo.khatabill.ui.books.DayBookScreen
import com.teczo.khatabill.ui.books.MoneyBookScreen
import com.teczo.khatabill.ui.credit.CollectionsScreen
import com.teczo.khatabill.ui.expenses.ExpenseEditScreen
import com.teczo.khatabill.ui.expenses.ExpenseListScreen
import com.teczo.khatabill.ui.reports.ReportHubScreen
import com.teczo.khatabill.ui.reports.ReportKind
import com.teczo.khatabill.ui.reports.ReportScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import com.teczo.khatabill.ui.credit.ReminderScreen
import com.teczo.khatabill.ui.more.MoreEntry
import com.teczo.khatabill.ui.more.MoreViewModel
import com.teczo.khatabill.ui.more.MoreScreen
import com.teczo.khatabill.ui.more.moneyIcon
import com.teczo.khatabill.ui.parties.NEW_ID
import com.teczo.khatabill.ui.parties.PartyDetailScreen
import com.teczo.khatabill.ui.payments.NO_PARTY
import com.teczo.khatabill.ui.payments.PaymentListScreen
import com.teczo.khatabill.ui.payments.RecordPaymentScreen
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
                AppBottomBar(selectedRoute = route, onSelect = { target ->
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
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
                    val next = if (startViewModel.hasBusiness.value) Routes.HOME else Routes.ONBOARDING
                    navController.navigate(next) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
                })
            }
            composable(Routes.ONBOARDING) {
                BusinessSetupScreen(onFinished = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onCollections = { navController.navigate(Routes.COLLECTIONS) },
                    onNewSale = { navController.navigate(Routes.invoiceNew(DocumentType.SALE.name)) },
                    onAddPayment = { navController.navigate(Routes.paymentNew(NO_PARTY)) },
                    onAddCustomer = { navController.navigate(Routes.partyEdit(NEW_ID)) },
                    onOpenInvoice = { id -> navController.navigate(Routes.invoiceDetail(id)) },
                )
            }
            composable(Routes.PARTIES) {
                PartyListScreen(
                    onAdd = { navController.navigate(Routes.partyEdit(NEW_ID)) },
                    onOpen = { id -> navController.navigate(Routes.partyDetail(id)) },
                )
            }
            composable(Routes.PARTY_DETAIL, arguments = listOf(navArgument("partyId") { type = NavType.StringType })) {
                PartyDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.partyEdit(id)) },
                    onAddPayment = { id -> navController.navigate(Routes.paymentNew(id)) },
                    onRemind = { id -> navController.navigate(Routes.reminder(id)) },
                    onOpenInvoice = { id -> navController.navigate(Routes.invoiceDetail(id)) },
                )
            }
            composable(Routes.PARTY_EDIT, arguments = listOf(navArgument("partyId") { type = NavType.StringType })) {
                PartyEditScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.PAYMENT_NEW, arguments = listOf(navArgument("partyId") { type = NavType.StringType })) {
                RecordPaymentScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.PAYMENTS) {
                PaymentListScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.paymentNew(NO_PARTY)) },
                )
            }
            composable(Routes.COLLECTIONS) {
                CollectionsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenParty = { id -> navController.navigate(Routes.partyDetail(id)) },
                    onRemind = { id -> navController.navigate(Routes.reminder(id)) },
                )
            }
            composable(Routes.REMINDER, arguments = listOf(navArgument("partyId") { type = NavType.StringType })) {
                ReminderScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EXPENSES) {
                ExpenseListScreen(onBack = { navController.popBackStack() }, onAdd = { navController.navigate(Routes.EXPENSE_NEW) })
            }
            composable(Routes.EXPENSE_NEW) { ExpenseEditScreen(onDone = { navController.popBackStack() }) }
            composable(Routes.REPORTS) {
                ReportHubScreen(onBack = { navController.popBackStack() }, onOpen = { kind -> navController.navigate(Routes.report(kind.name)) })
            }
            composable(Routes.REPORT, arguments = listOf(navArgument("kind") { type = NavType.StringType })) {
                ReportScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BOOK, arguments = listOf(navArgument("book") { type = NavType.StringType })) { entry ->
                if (entry.arguments?.getString("book") == "day") DayBookScreen(onBack = { navController.popBackStack() })
                else MoneyBookScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.UPI_QR, arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })) {
                UpiQrScreen(onBack = { navController.popBackStack() })
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
                InvoiceDetailScreen(
                    onBack = { navController.popBackStack() },
                    onShowQr = { id -> navController.navigate(Routes.upiQr(id)) },
                )
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
                val moreViewModel: MoreViewModel = hiltViewModel()
                val remindersEnabled by moreViewModel.remindersEnabled.collectAsStateWithLifecycle()
                MoreScreen(
                    remindersEnabled = remindersEnabled,
                    onRemindersChange = moreViewModel::setRemindersEnabled,
                    entries = listOf(
                        MoreEntry(R.string.more_payments, moneyIcon) { navController.navigate(Routes.PAYMENTS) },
                        MoreEntry(R.string.more_collections, Icons.Filled.Notifications) { navController.navigate(Routes.COLLECTIONS) },
                        MoreEntry(R.string.more_expenses, Icons.Filled.ShoppingCart) { navController.navigate(Routes.EXPENSES) },
                        MoreEntry(R.string.more_day_book, Icons.Filled.DateRange) { navController.navigate(Routes.book("day")) },
                        MoreEntry(R.string.more_cash_book, Icons.Filled.DateRange) { navController.navigate(Routes.book("cash")) },
                        MoreEntry(R.string.more_bank_book, Icons.Filled.DateRange) { navController.navigate(Routes.book("bank")) },
                        MoreEntry(R.string.more_reports, Icons.AutoMirrored.Filled.List) { navController.navigate(Routes.REPORTS) },
                    ),
                    currentLanguage = currentLanguage,
                    onLanguage = languageViewModel::select,
                )
            }
        }
    }
}
