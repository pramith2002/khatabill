package com.teczo.khatabill.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teczo.khatabill.data.dao.BusinessDao
import com.teczo.khatabill.data.dao.CategoryDao
import com.teczo.khatabill.data.dao.ExpenseDao
import com.teczo.khatabill.data.dao.InvoiceDao
import com.teczo.khatabill.data.dao.ItemDao
import com.teczo.khatabill.data.dao.PartyDao
import com.teczo.khatabill.data.dao.PaymentDao
import com.teczo.khatabill.data.dao.ReminderDao
import com.teczo.khatabill.data.dao.StockDao
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.CategoryEntity
import com.teczo.khatabill.data.entities.DocumentCounterEntity
import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.entities.PaymentAllocationEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.data.entities.ReminderEntity
import com.teczo.khatabill.data.entities.StockMovementEntity

/**
 * Version 1: business, category, party, item, invoice, invoice line, stock movement, document counter.
 * Version 2: payment, payment allocation, expense, reminder (see MIGRATION_1_2).
 * Schemas are exported to app/schemas so migrations can be written and tested.
 * Never use destructive migration: add a Migration to [ALL_MIGRATIONS] for every version bump.
 */
@Database(
    entities = [
        BusinessEntity::class,
        CategoryEntity::class,
        PartyEntity::class,
        ItemEntity::class,
        InvoiceEntity::class,
        InvoiceLineEntity::class,
        StockMovementEntity::class,
        DocumentCounterEntity::class,
        PaymentEntity::class,
        PaymentAllocationEntity::class,
        ExpenseEntity::class,
        ReminderEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class KhataBillDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun categoryDao(): CategoryDao
    abstract fun partyDao(): PartyDao
    abstract fun itemDao(): ItemDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun stockDao(): StockDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DB_NAME = "khatabill.db"
    }
}
