package com.teczo.khatabill.domain.model

/** Enums shared by the domain and data layers. Room stores them as their names (text). */

enum class SyncState { PENDING, SYNCED, CONFLICT }

enum class GstScheme { REGULAR, COMPOSITION, UNREGISTERED }

enum class BusinessType { AGARBATTI, SILK_HANDICRAFTS, FOOD_SWEETS, GARMENTS, GENERAL_TRADER, MANUFACTURER }

enum class PartyType { CUSTOMER, SUPPLIER, BOTH }

enum class BalanceType { TO_COLLECT, TO_PAY }

enum class ItemType { PRODUCT, SERVICE }

enum class DocumentType {
    SALE, PURCHASE, QUOTATION, SALE_RETURN, PURCHASE_RETURN, DELIVERY_CHALLAN, PROFORMA
}

enum class InvoiceStatus { DRAFT, UNPAID, PARTIAL, PAID, CANCELLED }

enum class StockReason { SALE, PURCHASE, RETURN, ADJUSTMENT, OPENING }
