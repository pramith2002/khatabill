package com.teczo.khatabill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every user-visible string must exist in English, Kannada and Hindi, with the same format placeholders.
 * Reads the resource files directly, so it also catches a missing translation before a device test does.
 */
class StringsCompletenessTest {
    private val resDir = File("src/main/res")
    private val nameRegex = Regex("""<string\s+name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val placeholder = Regex("""%\d+\$[sd]""")

    private fun load(folder: String): Map<String, String> {
        val dir = File(resDir, folder)
        assertTrue("missing folder ${dir.absolutePath}", dir.isDirectory)
        return dir.listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }!!
            .flatMap { f -> nameRegex.findAll(f.readText()).map { it.groupValues[1] to it.groupValues[2] }.toList() }
            .toMap()
    }

    private val en = load("values")
    private val kn = load("values-kn")
    private val hi = load("values-hi")

    @Test fun thereAreStrings() = assertTrue(en.size > 200)

    @Test fun kannadaHasEveryEnglishString() = assertEquals(emptySet<String>(), en.keys - kn.keys)

    @Test fun hindiHasEveryEnglishString() = assertEquals(emptySet<String>(), en.keys - hi.keys)

    @Test fun noExtraStringsInTranslations() {
        assertEquals(emptySet<String>(), kn.keys - en.keys)
        assertEquals(emptySet<String>(), hi.keys - en.keys)
    }

    @Test fun placeholdersMatchInEveryLanguage() {
        for ((name, text) in en) {
            val expected = placeholder.findAll(text).map { it.value }.sorted().toList()
            assertEquals("kn placeholders for $name", expected, placeholder.findAll(kn.getValue(name)).map { it.value }.sorted().toList())
            assertEquals("hi placeholders for $name", expected, placeholder.findAll(hi.getValue(name)).map { it.value }.sorted().toList())
        }
    }

    @Test fun translationsAreNotLeftAsEnglishCopies() {
        // Allowed to be identical: brand/tax terms and symbols that are the same in every language.
        val sameEverywhere = setOf(
            "app_name", "lang_en", "lang_kn", "lang_hi", "total_cgst", "total_sgst", "total_igst", "field_gstin",
            "pdf_gstin", "pdf_col_hsn", "pdf_col_gst", "pdf_ifsc", "pdf_upi", "amount_rupees", "qty_unit", "percent_value",
            "discount_percent_short", "discount_amount_short", "sample_row", "mode_upi", "col_gstin", "col_cgst", "col_sgst", "col_igst", "col_hsn", "col_gst_rate",
        )
        for ((name, text) in en) {
            if (name in sameEverywhere || name.startsWith("state_")) continue
            if (text.none { it.isLetter() }) continue
            assertTrue("kn string $name is still English", kn.getValue(name) != text)
            assertTrue("hi string $name is still English", hi.getValue(name) != text)
        }
    }
}
