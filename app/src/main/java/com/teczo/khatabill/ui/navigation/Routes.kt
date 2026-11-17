package com.teczo.khatabill.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.teczo.khatabill.R

object Routes {
    const val LANGUAGE = "language"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PARTIES = "parties"
    const val BILLING = "billing"
    const val ITEMS = "items"
    const val MORE = "more"

    const val PARTY_EDIT = "party_edit/{partyId}"
    const val ITEM_EDIT = "item_edit/{itemId}"
    const val ITEM_DETAIL = "item_detail/{itemId}"
    const val INVOICE_NEW = "invoice_new/{type}"
    const val QUICK_BILL = "quick_bill"
    const val INVOICE_DETAIL = "invoice_detail/{invoiceId}"
    const val PARTY_DETAIL = "party_detail/{partyId}"
    const val PAYMENT_NEW = "payment_new/{partyId}"
    const val PAYMENTS = "payments"
    const val COLLECTIONS = "collections"
    const val EXPENSES = "expenses"
    const val EXPENSE_NEW = "expense_new"
    const val REPORTS = "reports"
    const val REPORT = "report/{kind}"
    const val BOOK = "book/{book}"
    const val REMINDER = "reminder/{partyId}"
    const val UPI_QR = "upi_qr/{invoiceId}"

    fun partyEdit(id: String) = "party_edit/$id"
    fun itemEdit(id: String) = "item_edit/$id"
    fun itemDetail(id: String) = "item_detail/$id"
    fun invoiceNew(type: String) = "invoice_new/$type"
    fun invoiceDetail(id: String) = "invoice_detail/$id"
    fun partyDetail(id: String) = "party_detail/$id"
    fun paymentNew(partyId: String) = "payment_new/$partyId"
    fun upiQr(invoiceId: String) = "upi_qr/$invoiceId"
    fun reminder(partyId: String) = "reminder/$partyId"
    fun report(kind: String) = "report/$kind"
    fun book(book: String) = "book/$book"
}

data class TabItem(val route: String, @StringRes val label: Int, val icon: ImageVector)

/** Bottom navigation order from the spec: Home, Parties, Billing (centre), Items, More. */
val bottomTabs = listOf(
    TabItem(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TabItem(Routes.PARTIES, R.string.nav_parties, Icons.Filled.Person),
    TabItem(Routes.BILLING, R.string.nav_billing, Icons.Filled.Add),
    TabItem(Routes.ITEMS, R.string.nav_items, Icons.Filled.ShoppingCart),
    TabItem(Routes.MORE, R.string.nav_more, Icons.Filled.Menu),
)
