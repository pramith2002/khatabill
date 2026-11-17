package com.teczo.khatabill.domain.model

import androidx.annotation.StringRes
import com.teczo.khatabill.R

/** Expense categories. The key is what is stored; the label is translated. Unknown keys show as typed. */
object ExpenseCategories {
    val keys: List<String> = listOf("rent", "salary", "electricity", "transport", "raw_material", "repair", "food", "packing", "other")

    @StringRes
    fun labelRes(key: String): Int? = when (key) {
        "rent" -> R.string.exp_rent
        "salary" -> R.string.exp_salary
        "electricity" -> R.string.exp_electricity
        "transport" -> R.string.exp_transport
        "raw_material" -> R.string.exp_raw_material
        "repair" -> R.string.exp_repair
        "food" -> R.string.exp_food
        "packing" -> R.string.exp_packing
        "other" -> R.string.exp_other
        else -> null
    }
}
