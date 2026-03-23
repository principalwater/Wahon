package com.wahon.shared.data.local

data class OfflinePageArtifact(
    val relativePath: String,
    val sizeBytes: Long,
)

expect class OfflineChapterFileStore {
    fun savePage(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
        pageIndex: Int,
        imageUrl: String,
        payload: ByteArray,
    ): OfflinePageArtifact

    fun deleteChapter(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
    )

    fun deleteManga(
        sourceId: String,
        mangaUrl: String,
    )

    fun exists(relativePath: String): Boolean

    fun resolveFileUri(relativePath: String): String?
}
