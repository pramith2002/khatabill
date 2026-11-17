package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.domain.model.SyncState

/**
 * A customer or supplier. There is deliberately no stored balance: it is always derived
 * (opening balance + invoices - payments).
 */
@Entity(tableName = "party", indices = [Index(value = ["businessId", "name"])])
data class PartyEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val name: String,
    val nameKn: String? = null,
    val type: PartyType = PartyType.CUSTOMER,
    val phone: String? = null,
    val whatsapp: String? = null,
    val gstin: String? = null,
    val billingAddress: String? = null,
    val shippingAddress: String? = null,
    val stateCode: String? = null,
    /** Paise. null = no limit. */
    val creditLimit: Long? = null,
    /** Paise, always >= 0; [openingBalanceType] says who owes whom. */
    val openingBalance: Long = 0,
    val openingBalanceType: BalanceType = BalanceType.TO_COLLECT,
    val notes: String? = null,
)
