package com.teczo.khatabill.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 -> 2: adds payment, payment_allocation, expense and reminder.
 *
 * The CREATE statements are copied from the exported schema (app/schemas/.../2.json) so Room's
 * validation passes. Invoices that already had a paid amount get a CASH payment and an allocation
 * (ids are derived from the invoice id so the two inserts line up), so the new ledger agrees with
 * the cached paidAmount.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payment` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `syncState` TEXT NOT NULL, `type` TEXT NOT NULL, `partyId` TEXT, " +
                "`invoiceId` TEXT, `date` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `mode` TEXT NOT NULL, `reference` TEXT, `notes` TEXT, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `party`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_businessId_date` ON `payment` (`businessId`, `date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_partyId` ON `payment` (`partyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_invoiceId` ON `payment` (`invoiceId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payment_allocation` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `syncState` TEXT NOT NULL, `paymentId` TEXT NOT NULL, " +
                "`invoiceId` TEXT NOT NULL, `amount` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`paymentId`) REFERENCES `payment`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`invoiceId`) REFERENCES `invoice`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_allocation_paymentId` ON `payment_allocation` (`paymentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_allocation_invoiceId` ON `payment_allocation` (`invoiceId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expense` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `syncState` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
                "`category` TEXT NOT NULL, `amount` INTEGER NOT NULL, `mode` TEXT NOT NULL, `partyId` TEXT, `notes` TEXT, `receiptUri` TEXT, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `party`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_businessId_date` ON `expense` (`businessId`, `date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_partyId` ON `expense` (`partyId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminder` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `syncState` TEXT NOT NULL, `partyId` TEXT NOT NULL, `invoiceId` TEXT, " +
                "`scheduledAt` INTEGER NOT NULL, `channel` TEXT NOT NULL, `status` TEXT NOT NULL, `message` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `party`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`invoiceId`) REFERENCES `invoice`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_businessId_scheduledAt` ON `reminder` (`businessId`, `scheduledAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_partyId` ON `reminder` (`partyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_invoiceId` ON `reminder` (`invoiceId`)")

        // Backfill: every invoice that already has money against it gets a payment and an allocation.
        db.execSQL(
            "INSERT INTO payment (id, businessId, createdAt, updatedAt, deletedAt, syncState, type, partyId, invoiceId, date, amount, mode, reference, notes) " +
                "SELECT 'mig-' || id, businessId, createdAt, updatedAt, NULL, 'PENDING', " +
                "CASE WHEN type IN ('PURCHASE', 'SALE_RETURN') THEN 'PAID' ELSE 'RECEIVED' END, " +
                "partyId, id, date, paidAmount, 'CASH', NULL, NULL FROM invoice WHERE paidAmount > 0 AND deletedAt IS NULL",
        )
        db.execSQL(
            "INSERT INTO payment_allocation (id, businessId, createdAt, updatedAt, deletedAt, syncState, paymentId, invoiceId, amount) " +
                "SELECT 'mig-a-' || id, businessId, createdAt, updatedAt, NULL, 'PENDING', 'mig-' || id, id, paidAmount " +
                "FROM invoice WHERE paidAmount > 0 AND deletedAt IS NULL",
        )
    }
}

/**
 * Registry of all Room migrations.
 * Rule: no fallbackToDestructiveMigration in release builds, ever.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
