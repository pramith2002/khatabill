package com.teczo.khatabill.domain.model

/** Unit codes (stored in the database) and the default list per business type. */
object UnitCatalog {
    val all: List<String> =
        listOf("kg", "g", "pcs", "box", "dozen", "metre", "litre", "bundle", "carton", "pack")

    fun unitsFor(type: BusinessType): List<String> = when (type) {
        BusinessType.AGARBATTI -> listOf("kg", "g", "box", "pack", "bundle", "carton", "pcs", "dozen")
        BusinessType.SILK_HANDICRAFTS -> listOf("metre", "pcs", "box", "dozen")
        BusinessType.FOOD_SWEETS -> listOf("kg", "g", "box", "pcs", "dozen", "litre")
        BusinessType.GARMENTS -> listOf("pcs", "dozen", "metre", "box", "carton", "bundle")
        BusinessType.GENERAL_TRADER -> all
        BusinessType.MANUFACTURER -> listOf("kg", "pcs", "box", "carton", "bundle", "litre", "metre", "g")
    }
}

/** GST rate choices offered in forms, in basis points. */
object GstRates {
    val options: List<Int> = listOf(0, 25, 300, 500, 1200, 1800, 2800)
}
