package com.wahon.shared.data.local

expect class LocalPdfPageRenderer() {
    fun getPageCount(pdfPath: String): Int

    fun renderPageAsPng(
        pdfPath: String,
        pageIndex: Int,
    ): ByteArray
}
