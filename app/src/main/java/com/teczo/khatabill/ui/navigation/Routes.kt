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
    const val INVOICE_DETAIL = "invoice_detail/{invoiceId}"

    fun partyEdit(id: String) = "party_edit/$id"
    fun itemEdit(id: String) = "item_edit/$id"
    fun itemDetail(id: String) = "item_detail/$id"
    fun invoiceNew(type: String) = "invoice_new/$type"
    fun invoiceDetail(id: String) = "invoice_detail/$id"
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
