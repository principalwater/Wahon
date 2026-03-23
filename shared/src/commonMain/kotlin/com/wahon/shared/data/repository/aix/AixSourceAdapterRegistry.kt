package com.wahon.shared.data.repository.aix

class AixSourceAdapterRegistry(
    adapters: List<AixSourceAdapter>,
) {
    private val orderedAdapters = adapters.sortedByDescending(AixSourceAdapter::priority)

    fun find(source: AixSourceDescriptor): AixSourceAdapter? {
        return orderedAdapters.firstOrNull { adapter ->
            adapter.supports(source)
        }
    }
}
