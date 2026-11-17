package com.teczo.khatabill.util.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfShare {
    /** Opens the share sheet for an already built PDF (statements, reports). */
    fun shareFile(context: Context, file: java.io.File, mime: String = "application/pdf") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    /** Builds the PDF off the main thread, then opens the system share sheet with it attached. */
    suspend fun share(context: Context, business: BusinessEntity, invoice: InvoiceEntity, lines: List<InvoiceLineEntity>) {
        val file = withContext(Dispatchers.IO) { InvoicePdfBuilder(context).build(business, invoice, lines) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    }
}
