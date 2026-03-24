package com.wahon.shared.domain.model

data class LocalCbzImportBatchResult(
    val discovered: Int,
    val imported: Int,
    val failed: Int,
    val failures: List<LocalCbzImportFailure> = emptyList(),
)

data class LocalCbzImportFailure(
    val archivePath: String,
    val reason: String,
)
