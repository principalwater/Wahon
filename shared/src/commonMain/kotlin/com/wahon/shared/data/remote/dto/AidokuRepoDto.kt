package com.wahon.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for parsing community source list index JSON.
 * Format: { "name": "...", "sources": [...] }
 */
@Serializable
data class SourceListDto(
    val name: String = "",
    val sources: List<SourceEntryDto> = emptyList(),
)

@Serializable
data class SourceEntryDto(
    val id: String,
    val name: String,
    val version: Int = 1,
    @SerialName("iconURL") val iconUrl: String = "",
    @SerialName("icon") val legacyIcon: String = "",
    @SerialName("downloadURL") val downloadUrl: String = "",
    @SerialName("file") val legacyFile: String = "",
    val languages: List<String> = emptyList(),
    @SerialName("lang") val legacyLanguage: String = "",
    val contentRating: Int = 0,
    val nsfw: Int? = null,
    @SerialName("baseURL") val baseUrl: String = "",
    @SerialName("url") val legacyBaseUrl: String = "",
)
