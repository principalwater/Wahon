package com.wahon.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Manga(
    val id: String,
    val title: String,
    val artist: String = "",
    val author: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val status: MangaStatus = MangaStatus.UNKNOWN,
    val genres: List<String> = emptyList(),
    val inLibrary: Boolean = false,
    val sourceId: String = "",
    val url: String = "",
)

enum class MangaStatus {
    ONGOING, COMPLETED, HIATUS, CANCELLED, UNKNOWN
}
