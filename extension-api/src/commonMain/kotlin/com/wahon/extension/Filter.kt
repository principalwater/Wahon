package com.wahon.extension

import kotlinx.serialization.Serializable

/**
 * Hierarchical filter system for source-specific search UI.
 * Each source can declare its own filters, and the app dynamically
 * builds the search UI based on them.
 */
@Serializable
sealed class Filter {
    abstract val name: String

    @Serializable
    data class Header(override val name: String) : Filter()

    @Serializable
    data class Separator(override val name: String = "") : Filter()

    @Serializable
    data class Text(override val name: String, val state: String = "") : Filter()

    @Serializable
    data class Select(
        override val name: String,
        val options: List<String>,
        val state: Int = 0,
    ) : Filter()

    @Serializable
    data class CheckBox(
        override val name: String,
        val state: Boolean = false,
    ) : Filter()

    @Serializable
    data class Sort(
        override val name: String,
        val options: List<String>,
        val state: SortState? = null,
    ) : Filter()

    @Serializable
    data class Group(
        override val name: String,
        val filters: List<Filter>,
    ) : Filter()
}

@Serializable
data class SortState(
    val index: Int,
    val ascending: Boolean,
)
