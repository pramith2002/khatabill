package com.teczo.khatabill.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Builds a genuine version-1 database from the exported schema (app/schemas/.../1.json), fills it with data,
 * then opens it through Room with MIGRATION_1_2. Room itself validates the migrated tables against the
 * version-2 entities when it opens the file, so a wrong migration fails here.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val target: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        dbFile = target.getDatabasePath(NAME)
        target.deleteDatabase(NAME)
        dbFile.parentFile?.mkdirs()
    }

    @After
    fun tearDown() {
        target.deleteDatabase(NAME)
    }

    private val insertInvoice = """
        INSERT INTO invoice (id, businessId, createdAt, updatedAt, syncState, type, series, financialYear, sequenceNo, number, date,
            placeOfSupplyStateCode, subtotal, discountTotal, otherCharges, taxableTotal, cgst, sgst, igst, roundOff, grandTotal, paidAmount, status)
        VALUES (?, 'biz', 1, 1, 'PENDING', ?, 'INV', '2026-27', ?, ?, 20000, '29', 1000, 0, 0, 1000, 0, 0, 0, 0, 1000, ?, ?)
    """.trimIndent()

    /** Creates the version-1 database exactly as Room created it (tables, indices, identity hash, user_version = 1). */
    private fun createVersion1(fill: (SQLiteDatabase) -> Unit) {
        val json = JSONObject(
            testAssets.open("com.teczo.khatabill.data.db.KhataBillDatabase/1.json").bufferedReader().use { it.readText() },
        ).getJSONObject("database")
        val raw = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        raw.use { db ->
            val entities = json.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = e.optJSONArray("indices")
                if (indices != null) {
                    for (j in 0 until indices.length()) {
                        db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
            }
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            fill(db)
            db.version = 1
        }
    }

    private fun openMigrated(): KhataBillDatabase =
        Room.databaseBuilder(target, KhataBillDatabase::class.java, NAME).addMigrations(MIGRATION_1_2).build()

    @Test
    fun migratingFromV1KeepsDataAndCreatesThePaymentLedger() {
        createVersion1 { db ->
            db.execSQL(insertInvoice, arrayOf<Any?>("inv-paid", "SALE", 1, "INV/26-27/0001", 400L, "PARTIAL"))
            db.execSQL(insertInvoice, arrayOf<Any?>("inv-unpaid", "SALE", 2, "INV/26-27/0002", 0L, "UNPAID"))
            db.execSQL(insertInvoice, arrayOf<Any?>("inv-purchase", "PURCHASE", 3, "PUR/26-27/0003", 1000L, "PAID"))
        }

        val room = openMigrated()
        val db = room.openHelper.writableDatabase // opening runs the migration and Room's schema validation

        db.query("SELECT COUNT(*) FROM invoice").use { c -> c.moveToFirst(); assertEquals(3, c.getInt(0)) }

        db.query("SELECT id, type, amount, mode, invoiceId FROM payment ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals("mig-inv-paid", c.getString(0))
            assertEquals("RECEIVED", c.getString(1))
            assertEquals(400L, c.getLong(2))
            assertEquals("CASH", c.getString(3))
            assertEquals("inv-paid", c.getString(4))
            c.moveToNext()
            assertEquals("mig-inv-purchase", c.getString(0))
            assertEquals("PAID", c.getString(1))
        }

        db.query("SELECT paymentId, invoiceId, amount FROM payment_allocation ORDER BY paymentId").use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals("mig-inv-paid", c.getString(0))
            assertEquals("inv-paid", c.getString(1))
            assertEquals(400L, c.getLong(2))
        }

        listOf("expense", "reminder").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
        assertEquals(2, db.version)
        room.close()
    }

    @Test
    fun migratingAnEmptyV1DatabaseWorks() {
        createVersion1 { }
        val room = openMigrated()
        assertEquals(2, room.openHelper.writableDatabase.version)
        room.close()
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
