package com.wahon.shared.domain.repository

import com.wahon.shared.domain.model.LocalCbzImportBatchResult
import com.wahon.shared.domain.model.LocalCbzImportResult

interface LocalArchiveRepository {
    suspend fun importCbzArchive(archivePath: String): Result<LocalCbzImportResult>

    suspend fun listCbzArchives(
        directoryPath: String,
        recursive: Boolean = true,
    ): Result<List<String>>

    suspend fun importCbzDirectory(
        directoryPath: String,
        recursive: Boolean = true,
    ): Result<LocalCbzImportBatchResult>
}
