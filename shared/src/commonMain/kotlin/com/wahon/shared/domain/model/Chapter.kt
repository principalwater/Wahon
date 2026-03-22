package com.wahon.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: String,
    val mangaId: String,
    val name: String,
    val chapterNumber: Float = -1f,
    val dateUpload: Long = 0L,
    val read: Boolean = false,
    val lastPageRead: Int = 0,
    val url: String = "",
    val scanlator: String = "",
)
