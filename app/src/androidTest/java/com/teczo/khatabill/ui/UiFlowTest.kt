package com.teczo.khatabill.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.localized
import com.teczo.khatabill.testBusiness
import com.teczo.khatabill.ui.billing.InvoiceFormState
import com.teczo.khatabill.ui.billing.LineState
import com.teczo.khatabill.ui.billing.NewInvoiceActions
import com.teczo.khatabill.ui.billing.NewInvoiceContent
import com.teczo.khatabill.ui.billing.buildCalc
import com.teczo.khatabill.ui.language.LanguageOptions
import com.teczo.khatabill.ui.onboarding.SetupForm
import com.teczo.khatabill.ui.onboarding.StepGstAndType
import com.teczo.khatabill.ui.theme.KhataBillTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Text as the screen under test shows it, in whatever language the device or app is currently set to. */
    private fun s(id: Int) = compose.activity.getString(id)

    // ---- language ----

    @Test fun languagePickerShowsAllThreeLanguagesAndReportsTheChoice() {
        var chosen = ""
        compose.setContent { KhataBillTheme { LanguageOptions(current = "", onSelect = { chosen = it }) } }
        compose.onNodeWithText("ಕನ್ನಡ").assertIsDisplayed()
        compose.onNodeWithText("English").assertIsDisplayed()
        compose.onNodeWithText("हिन्दी").assertIsDisplayed()
        compose.onNodeWithText("हिन्दी").performClick()
        assertEquals("hi", chosen)
    }

    @Test fun tabLabelsResolveInEveryLanguage() {
        val expected = mapOf(
            "en" to listOf("Home", "Parties", "Billing", "Items", "More"),
            "kn" to listOf("ಮುಖಪುಟ", "ಗ್ರಾಹಕರು", "ಬಿಲ್", "ಸರಕುಗಳು", "ಇನ್ನಷ್ಟು"),
            "hi" to listOf("होम", "ग्राहक", "बिल", "सामान", "और"),
        )
        expected.forEach { (tag, labels) ->
            val ctx = context.localized(tag)
            val actual = listOf(R.string.nav_home, R.string.nav_parties, R.string.nav_billing, R.string.nav_items, R.string.nav_more)
                .map { ctx.getString(it) }
            assertEquals("labels for $tag", labels, actual)
        }
    }

    @Test fun billingStringsAreLocalisedToo() {
        assertEquals("Save bill", context.localized("en").getString(R.string.btn_save_bill))
        assertEquals("ಬಿಲ್ ಉಳಿಸಿ", context.localized("kn").getString(R.string.btn_save_bill))
        assertEquals("बिल सहेजें", context.localized("hi").getString(R.string.btn_save_bill))
    }

    // ---- business setup ----

    @Test fun gstinFieldAppearsOnlyWhenTheBusinessHasGst() {
        compose.setContent {
            KhataBillTheme {
                var form by remember { mutableStateOf(SetupForm()) }
                StepGstAndType(form, emptyMap()) { change -> form = change(form) }
            }
        }
        assertEquals(0, compose.onAllNodesWithText("GSTIN").fetchSemanticsNodes().size)
        compose.onNodeWithText(s(R.string.gst_regular)).performClick()
        compose.onNodeWithText("GSTIN").assertIsDisplayed()
        compose.onNodeWithText(s(R.string.gst_none)).performClick()
        assertEquals(0, compose.onAllNodesWithText("GSTIN").fetchSemanticsNodes().size)
    }

    // ---- invoice creation ----

    private fun line(qty: String = "1", rate: String = "100") = LineState(
        key = "k1", itemId = "i1", name = "Agarbatti", hsn = null, unit = "kg", gstRateBp = 1800, taxInclusive = false,
        affectsStock = true, qty = qty, rate = rate,
    )

    private fun noOpActions(onLine: (String, (LineState) -> LineState) -> Unit = { _, _ -> }) = NewInvoiceActions(
        onBack = {}, onUpdate = {}, onSetParty = {}, onAddItem = {}, onUpdateLine = onLine, onRemoveLine = {}, onSave = {},
    )

    @Test fun newBillShowsCgstSgstTotalsForALocalSale() {
        val form = InvoiceFormState(type = DocumentType.SALE, placeOfSupply = "29", date = 0, lines = listOf(line()))
        val biz = testBusiness()
        compose.setContent {
            KhataBillTheme { NewInvoiceContent(form, biz, buildCalc(form, biz), emptyList(), emptyList(), null, noOpActions()) }
        }
        compose.onNodeWithText("CGST").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SGST").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("IGST").fetchSemanticsNodes().size)
        assertTrue(compose.onAllNodesWithText("₹118.00").fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun newBillShowsIgstForAnotherState() {
        val form = InvoiceFormState(type = DocumentType.SALE, placeOfSupply = "27", date = 0, lines = listOf(line()))
        val biz = testBusiness()
        compose.setContent {
            KhataBillTheme { NewInvoiceContent(form, biz, buildCalc(form, biz), emptyList(), emptyList(), null, noOpActions()) }
        }
        compose.onNodeWithText("IGST").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("CGST").fetchSemanticsNodes().size)
    }

    @Test fun typingAQuantityReachesTheViewModelCallback() {
        val form = InvoiceFormState(type = DocumentType.SALE, placeOfSupply = "29", date = 0, lines = listOf(line()))
        val biz = testBusiness()
        var typed: LineState? = null
        compose.setContent {
            KhataBillTheme {
                NewInvoiceContent(form, biz, buildCalc(form, biz), emptyList(), emptyList(), null, noOpActions { _, change -> typed = change(line()) })
            }
        }
        compose.onNodeWithText(s(R.string.line_qty) + " (" + s(R.string.unit_kg) + ")").performTextInput("5")
        assertTrue("callback got ${typed?.qty}", typed?.qty?.contains("5") == true)
    }

    @Test fun saveIsDisabledUntilThereIsAnItem() {
        val form = InvoiceFormState(type = DocumentType.SALE, placeOfSupply = "29", date = 0)
        val biz = testBusiness()
        compose.setContent {
            KhataBillTheme { NewInvoiceContent(form, biz, buildCalc(form, biz), emptyList(), emptyList(), null, noOpActions()) }
        }
        compose.onNodeWithText(s(R.string.btn_save_bill)).assertIsDisplayed()
        compose.onNodeWithText(s(R.string.btn_save_bill)).assertIsNotEnabled()
    }
}
