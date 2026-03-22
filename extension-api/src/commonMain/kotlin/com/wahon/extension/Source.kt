package com.wahon.extension

import kotlinx.serialization.Serializable

/**
 * Base interface for all content sources.
 * Extensions must implement this to provide manga/comics content.
 */
interface Source {
    val id: String
    val name: String
    val language: String
    val supportsNsfw: Boolean get() = false
}

/**
 * Metadata describing an extension package.
 */
@Serializable
data class ExtensionMeta(
    val name: String,
    val version: String,
    val language: String,
    val nsfw: Boolean = false,
)
