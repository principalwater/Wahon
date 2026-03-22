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
    @SerialName("downloadURL") val downloadUrl: String = "",
    val languages: List<String> = emptyList(),
    val contentRating: Int = 0,
    @SerialName("baseURL") val baseUrl: String = "",
)
