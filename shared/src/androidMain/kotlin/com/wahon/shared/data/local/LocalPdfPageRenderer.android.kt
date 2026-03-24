package com.wahon.shared.data.local

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File

actual class LocalPdfPageRenderer actual constructor() {

    actual fun getPageCount(pdfPath: String): Int {
        val normalizedPdfPath = normalizePdfPath(pdfPath)
        val descriptor = ParcelFileDescriptor.open(
            File(normalizedPdfPath),
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
        descriptor.use { parcelDescriptor ->
            PdfRenderer(parcelDescriptor).use { renderer ->
                return renderer.pageCount
            }
        }
    }

    actual fun renderPageAsPng(
        pdfPath: String,
        pageIndex: Int,
    ): ByteArray {
        val normalizedPdfPath = normalizePdfPath(pdfPath)
        require(pageIndex >= 0) { "PDF page index is negative: $pageIndex" }

        val descriptor = ParcelFileDescriptor.open(
            File(normalizedPdfPath),
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
        descriptor.use { parcelDescriptor ->
            PdfRenderer(parcelDescriptor).use { renderer ->
                require(pageIndex < renderer.pageCount) {
                    "PDF page index out of range: index=$pageIndex count=${renderer.pageCount}"
                }

                renderer.openPage(pageIndex).use { page ->
                    val width = page.width.coerceAtLeast(1)
                    val height = page.height.coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                    )

                    return ByteArrayOutputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                        bitmap.recycle()
                        output.toByteArray()
                    }
                }
            }
        }
    }

    private fun normalizePdfPath(path: String): String {
        val normalized = path.trim()
        require(normalized.isNotBlank()) { "PDF path is blank" }
        return normalized
    }
}
