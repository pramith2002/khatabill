package com.teczo.khatabill.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.testBusiness
import com.teczo.khatabill.ui.billing.InvoiceFormState
import com.teczo.khatabill.ui.billing.LineState
import com.teczo.khatabill.ui.billing.NewInvoiceActions
import com.teczo.khatabill.ui.billing.NewInvoiceContent
import com.teczo.khatabill.ui.billing.buildCalc
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.AppBottomBar
import com.teczo.khatabill.ui.components.PeachCard
import com.teczo.khatabill.ui.components.ProgressRing
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.ui.components.WhiteCard
import com.teczo.khatabill.ui.navigation.Routes
import com.teczo.khatabill.ui.theme.KhataBillTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Renders key screens to PNG files (pulled with adb) so the visual design can be reviewed without clicking through the app. */
@RunWith(AndroidJUnit4::class)
class DesignPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun save(name: String) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(ctx.getExternalFilesDir(null), "design-preview").apply { mkdirs() }
        val bmp: Bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(bmp.width > 100)
    }

    @Test fun homeStyleScreen() {
        compose.setContent {
            KhataBillTheme {
                Scaffold(bottomBar = { AppBottomBar(Routes.HOME) {} }) { pad ->
                    Column(Modifier.fillMaxSize().padding(pad).statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Namaskara, Ramesh", style = MaterialTheme.typography.headlineMedium)
                        TealCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("To collect", style = MaterialTheme.typography.titleMedium)
                                    Text("₹1,24,500.00", style = MaterialTheme.typography.headlineMedium)
                                    AccentButton(onClick = {}) { Text("Collections") }
                                }
                                ProgressRing(0.8f)
                            }
                        }
                        Text("Today", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WhiteCard(Modifier.weight(1f)) { Text("Sales"); Text("₹18,450.00", style = MaterialTheme.typography.titleLarge) }
                            PeachCard(Modifier.weight(1f)) { Text("Received"); Text("₹9,200.00", style = MaterialTheme.typography.titleLarge) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PeachCard(Modifier.weight(1f)) { Text("To pay"); Text("₹32,000.00", style = MaterialTheme.typography.titleLarge) }
                            TealCard(Modifier.weight(1f)) { Text("Overdue"); Text("₹14,300.00", style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
            }
        }
        save("home-style")
    }

    @Test fun newBillScreen() {
        val line = LineState(
            key = "k", itemId = "i", name = "Sandal Agarbatti", hsn = null, unit = "kg", gstRateBp = 1800, taxInclusive = false,
            affectsStock = true, qty = "2.5", rate = "200",
        )
        val form = InvoiceFormState(type = DocumentType.SALE, placeOfSupply = "29", date = 20_400, lines = listOf(line, line.copy(key = "k2", name = "Rose Agarbatti", qty = "12", rate = "45", gstRateBp = 500)))
        val biz = testBusiness()
        compose.setContent {
            KhataBillTheme {
                NewInvoiceContent(
                    form, biz, buildCalc(form, biz), emptyList(), emptyList(), null,
                    NewInvoiceActions({}, {}, {}, {}, { _, _ -> }, {}, {}),
                )
            }
        }
        save("new-bill")
    }
}
