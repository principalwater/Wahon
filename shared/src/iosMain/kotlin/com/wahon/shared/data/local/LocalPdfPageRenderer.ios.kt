package com.wahon.shared.data.local

actual class LocalPdfPageRenderer actual constructor() {

    actual fun getPageCount(pdfPath: String): Int {
        error("PDF page rendering is not implemented on iOS yet")
    }

    actual fun renderPageAsPng(
        pdfPath: String,
        pageIndex: Int,
    ): ByteArray {
        error("PDF page rendering is not implemented on iOS yet")
    }
}
