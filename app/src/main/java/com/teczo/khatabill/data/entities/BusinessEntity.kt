package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.BusinessType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.domain.model.SyncState

/** A business. Its own [id] is the businessId carried by every other row; [businessId] repeats it for sync. */
@Entity(tableName = "business")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val name: String,
    val ownerName: String,
    val phone: String,
    val email: String? = null,
    val address: String,
    val city: String,
    val state: String,
    val stateCode: String,
    val pincode: String,
    val gstin: String? = null,
    val pan: String? = null,
    val gstScheme: GstScheme = GstScheme.UNREGISTERED,
    val businessType: BusinessType = BusinessType.GENERAL_TRADER,
    val logoUri: String? = null,
    val signatureUri: String? = null,
    val upiId: String? = null,
    val bankName: String? = null,
    val accountNo: String? = null,
    val ifsc: String? = null,
    /** At most 2 characters so invoice numbers stay within the GST limit of 16. */
    val invoicePrefix: String = "",
    /** 4 = April (Indian financial year). */
    val financialYearStartMonth: Int = 4,
)
