package com.teczo.khatabill.util.export

import com.teczo.khatabill.domain.report.ReportTable
import java.io.File

/**
 * Writes report tables as CSV that opens cleanly in Excel: UTF-8 with a byte-order mark (so Kannada and Hindi
 * show correctly), quoted when needed, plain numbers without grouping.
 */
object CsvExport {
    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    fun render(tables: List<ReportTable>, resolve: (Int) -> String, headerLines: List<String> = emptyList()): String {
        val sb = StringBuilder()
        headerLines.forEach { sb.append(escape(it)).append("\r\n") }
        if (headerLines.isNotEmpty()) sb.append("\r\n")
        tables.forEachIndexed { index, t ->
            if (index > 0) sb.append("\r\n")
            t.title?.let { sb.append(escape(resolve(it))).append("\r\n") }
            sb.append(t.columns.joinToString(",") { escape(resolve(it.label)) }).append("\r\n")
            val cellText = { c: com.teczo.khatabill.domain.report.Cell -> c.res?.let(resolve) ?: c.raw }
            t.rows.forEach { row -> sb.append(row.joinToString(",") { escape(cellText(it)) }).append("\r\n") }
            t.total?.let { row -> sb.append(row.joinToString(",") { escape(cellText(it)) }).append("\r\n") }
        }
        return sb.toString()
    }

    fun write(file: File, tables: List<ReportTable>, resolve: (Int) -> String, headerLines: List<String> = emptyList()): File {
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            out.write(render(tables, resolve, headerLines).toByteArray(Charsets.UTF_8))
        }
        return file
    }
}
