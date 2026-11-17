package com.teczo.khatabill.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.teczo.khatabill.MainActivity
import com.teczo.khatabill.R
import com.teczo.khatabill.localized
import com.teczo.khatabill.data.db.ALL_MIGRATIONS
import com.teczo.khatabill.data.db.KhataBillDatabase
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.repositories.InvoiceDraft
import com.teczo.khatabill.data.repositories.InvoiceLineDraft
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.ItemRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.BusinessType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.util.dates.DateFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * End-to-end: seeds a realistic shop into the real database file, launches the real MainActivity (real Hilt graph,
 * real screens), picks English, and walks through Home, parties, items, bills, collections, books and reports.
 * Saves a PNG of each screen to the app's external files folder for review.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = DateFormat.today()
    private val biz = "biz-e2e"
    private lateinit var outDir: File

    /** The test picks English on the language screen, so expected labels are read in English. */
    private fun s(id: Int) = target.localized("en").getString(id)

    @Before
    fun seed() = runBlocking {
        outDir = File(target.getExternalFilesDir(null), "flow-preview").apply { deleteRecursively(); mkdirs() }
        target.deleteDatabase(KhataBillDatabase.DB_NAME)
        File(target.filesDir, "datastore").deleteRecursively()

        val db = Room.databaseBuilder(target, KhataBillDatabase::class.java, KhataBillDatabase.DB_NAME).addMigrations(*ALL_MIGRATIONS).build()
        val now = System.currentTimeMillis()
        db.businessDao().upsert(
            BusinessEntity(
                id = biz, businessId = biz, createdAt = now, updatedAt = now, name = "Sri Ganesh Agarbatti", ownerName = "Ramesh",
                phone = "9845012345", address = "Devaraja Mohalla", city = "Mysuru", state = "Karnataka", stateCode = "29", pincode = "570001",
                gstin = "29AAGCB7383J1Z4", gstScheme = GstScheme.REGULAR, businessType = BusinessType.AGARBATTI, upiId = "ramesh@okhdfcbank",
                bankName = "State Bank of India", accountNo = "123456789012", ifsc = "SBIN0001234", invoicePrefix = "SG",
            ),
        )
        fun party(id: String, name: String, kn: String?, type: PartyType = PartyType.CUSTOMER, opening: Long = 0, gstin: String? = null) =
            PartyEntity(id = id, businessId = biz, createdAt = now, updatedAt = now, name = name, nameKn = kn, type = type, phone = "98450" + (10000 + id.hashCode() % 89999).toString().takeLast(5), stateCode = "29", gstin = gstin, openingBalance = opening, openingBalanceType = BalanceType.TO_COLLECT, creditLimit = if (id == "p1") 50_000_00L else null)
        db.partyDao().upsert(party("p1", "Lakshmi Stores", "ಲಕ್ಷ್ಮಿ ಸ್ಟೋರ್ಸ್", gstin = "29AAPFU0939F1Z".let { it + com.teczo.khatabill.util.validators.GstinValidator.checkCharacter(it) }))
        db.partyDao().upsert(party("p2", "Manjunath Traders", "ಮಂಜುನಾಥ ಟ್ರೇಡರ್ಸ್", opening = 4_000_00L))
        db.partyDao().upsert(party("p3", "Mysore Fragrance Supplies", null, PartyType.SUPPLIER))

        val items = ItemRepository(db, db.itemDao(), db.stockDao())
        items.create(ItemEntity(id = "i1", businessId = biz, createdAt = now, updatedAt = now, name = "Sandal Agarbatti", nameKn = "ಚಂದನ ಅಗರಬತ್ತಿ", unit = "kg", salePrice = 200_00, purchasePrice = 120_00, gstRateBp = 1800, hsnSac = "33074100", minStock = 10_000), 8_000, today - 60)
        items.create(ItemEntity(id = "i2", businessId = biz, createdAt = now, updatedAt = now, name = "Rose Agarbatti", nameKn = "ಗುಲಾಬಿ ಅಗರಬತ್ತಿ", unit = "kg", salePrice = 45_00, purchasePrice = 28_00, gstRateBp = 500, hsnSac = "33074100"), 120_000, today - 60)
        items.create(ItemEntity(id = "i3", businessId = biz, createdAt = now, updatedAt = now, name = "Dhoop Sticks", unit = "box", salePrice = 150_50, purchasePrice = 90_00, gstRateBp = 1200, hsnSac = "33074900"), 40_000, today - 60)

        val invoices = InvoiceRepository(db, db.invoiceDao(), db.stockDao(), db.paymentDao())
        val payments = PaymentRepository(db, db.paymentDao(), db.invoiceDao())

        suspend fun bill(type: DocumentType, partyId: String, name: String, gstin: String?, daysAgo: Int, itemId: String, itemName: String, qty: Long, rate: Long, bp: Int, due: Int?, paid: Long = 0, intra: Boolean = true): String {
            val gross = qty * rate / 1000
            val tax = gross * bp / 10_000
            val half = tax / 2
            val total = gross + (if (intra) half * 2 else tax)
            return invoices.createInvoice(
                InvoiceDraft(
                    businessId = biz, type = type, series = if (type == DocumentType.SALE) "INV" else "PUR", prefix = "SG", financialYearStartMonth = 4,
                    date = today - daysAgo, dueDate = due?.let { today + it }, partyId = partyId, partyName = name, partyGstin = gstin, partyAddress = null,
                    partyStateCode = "29", placeOfSupplyStateCode = if (intra) "29" else "27", subtotal = gross, discountTotal = 0, otherCharges = 0, taxableTotal = gross,
                    cgst = if (intra) half else 0, sgst = if (intra) half else 0, igst = if (intra) 0 else tax, roundOff = 0, grandTotal = total, paidAmount = paid,
                    paymentMode = PaymentMode.UPI, notes = null, terms = null, transportDetails = null, vehicleNo = null,
                    lines = listOf(
                        InvoiceLineDraft(
                            itemId = itemId, nameSnapshot = itemName, hsnSac = "33074100", qty = qty, unit = "kg", rate = rate, taxInclusive = false,
                            discountPercentBp = 0, discountAmount = 0, taxableValue = gross, gstRateBp = bp, cgst = if (intra) half else 0,
                            sgst = if (intra) half else 0, igst = if (intra) 0 else tax, lineTotal = total, affectsStock = true,
                        ),
                    ),
                ),
            ).id
        }
        bill(DocumentType.SALE, "p1", "Lakshmi Stores", "29AAPFU0939F1Z".let { it + com.teczo.khatabill.util.validators.GstinValidator.checkCharacter(it) }, 52, "i1", "Sandal Agarbatti", 25_000, 200_00, 1800, due = -22)
        bill(DocumentType.SALE, "p1", "Lakshmi Stores", null, 12, "i2", "Rose Agarbatti", 30_000, 45_00, 500, due = -2, paid = 500_00)
        bill(DocumentType.SALE, "p2", "Manjunath Traders", null, 3, "i3", "Dhoop Sticks", 12_000, 150_50, 1200, due = 5, intra = false)
        bill(DocumentType.SALE, "p2", "Manjunath Traders", null, 0, "i1", "Sandal Agarbatti", 7_500, 200_00, 1800, due = 15, paid = 1_000_00)
        bill(DocumentType.PURCHASE, "p3", "Mysore Fragrance Supplies", null, 20, "i2", "Rose Agarbatti", 100_000, 28_00, 500, due = null)
        payments.record(biz, PaymentType.RECEIVED, "p1", today - 5, 800_00, PaymentMode.BANK, "UTR889123", null)
        payments.record(biz, PaymentType.PAID, "p3", today - 2, 1_000_00, PaymentMode.CASH, null, null)
        db.expenseDao().upsert(ExpenseEntity(id = "e1", businessId = biz, createdAt = now, updatedAt = now, date = today - 1, category = "rent", amount = 6_500_00, mode = PaymentMode.CASH))
        db.expenseDao().upsert(ExpenseEntity(id = "e2", businessId = biz, createdAt = now + 1, updatedAt = now, date = today, category = "transport", amount = 450_00, mode = PaymentMode.UPI, notes = "Tempo to Bengaluru"))
        db.close()
    }

    private fun shot(name: String) {
        val bmp: Bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(outDir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Clicks the first clickable element showing [text] (tabs, rows, buttons); falls back to any element with that text. */
    private fun click(text: String, index: Int = 0) {
        val clickable = hasText(text) and hasClickAction()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodes(clickable).fetchSemanticsNodes().size > index) compose.onAllNodes(clickable)[index].performClick()
        else compose.onAllNodes(hasText(text))[index].performClick()
        compose.waitForIdle()
    }

    private fun waitFor(text: String) {
        try {
            compose.waitUntil(10_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            // keep evidence of what was on screen when the expected text never appeared
            runCatching { shot("FAIL-waiting-for-" + text.take(20).filter { it.isLetterOrDigit() }) }
            runCatching { File(outDir, "FAIL-tree.txt").writeText("waiting for: $text\n" + compose.onRoot().printToString(maxDepth = 12)) }
            throw e
        }
    }

    private fun ActivityScenario<MainActivity>.back() {
        onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun scrollTo(text: String) {
        compose.onAllNodes(hasText(text))[0].performScrollTo()
        compose.waitForIdle()
    }

    @Test
    fun walkThroughTheWholeApp() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            click("English")

            // Home dashboard
            waitFor(s(R.string.home_view_collections))
            shot("01-home")
            scrollTo(s(R.string.dash_recent))
            shot("02-home-lower")

            // Parties: balances, filters, detail
            click(s(R.string.nav_parties))
            waitFor("Lakshmi Stores")
            shot("03-parties")
            click("Lakshmi Stores")
            waitFor(s(R.string.party_add_payment))
            shot("04-party-detail")
            click(s(R.string.party_add_payment))
            waitFor(s(R.string.btn_save_payment))
            shot("05-record-payment")
            it.back()
            it.back()

            // Items with stock, low stock flag
            click(s(R.string.nav_items))
            waitFor("Sandal Agarbatti")
            shot("06-items")

            // Bills: list, detail, UPI QR
            click(s(R.string.nav_billing))
            waitFor("SGINV/26-27/0001")
            shot("07-bills")
            click("SGINV/26-27/0002")
            waitFor(s(R.string.invoice_share))
            shot("08-bill-detail")
            click(s(R.string.invoice_show_qr))
            waitFor(s(R.string.upi_pay_with_app))
            shot("09-upi-qr")
            it.back()
            it.back()

            // More: money screens
            click(s(R.string.nav_more))
            waitFor(s(R.string.more_payments))
            shot("10-more")

            click(s(R.string.more_payments))
            waitFor(s(R.string.payments_by_mode))
            shot("11-payments")
            it.back()

            click(s(R.string.more_collections))
            waitFor(s(R.string.aging_hint))
            shot("12-collections")
            click(s(R.string.party_remind))
            waitFor(s(R.string.reminder_whatsapp))
            shot("13-reminder")
            it.back()
            it.back()

            click(s(R.string.more_expenses))
            waitFor(s(R.string.exp_rent))
            shot("14-expenses")
            it.back()

            click(s(R.string.more_day_book))
            waitFor(s(R.string.field_date))
            shot("15-day-book")
            it.back()

            click(s(R.string.more_cash_book))
            waitFor(s(R.string.statement_closing))
            shot("16-cash-book")
            it.back()

            click(s(R.string.more_reports))
            waitFor(s(R.string.report_sales))
            shot("17-report-hub")
            click(s(R.string.report_gstr1))
            waitFor(s(R.string.tbl_b2b))
            shot("18-gstr1")
            it.back()
            click(s(R.string.report_gstr3b))
            waitFor(s(R.string.tbl_3b).let { _ -> s(R.string.gst_net) })
            shot("19-gstr3b")
            it.back()
            click(s(R.string.report_pl))
            waitFor(s(R.string.pl_profit))
            shot("20-profit-loss")
        }
        assertTrue(outDir.listFiles()!!.size >= 20)
    }
}