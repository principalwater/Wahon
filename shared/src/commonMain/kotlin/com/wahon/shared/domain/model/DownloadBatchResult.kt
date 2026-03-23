package com.wahon.shared.domain.model

data class DownloadBatchResult(
    val requested: Int,
    val downloaded: Int,
    val skipped: Int,
    val failed: Int,
)
