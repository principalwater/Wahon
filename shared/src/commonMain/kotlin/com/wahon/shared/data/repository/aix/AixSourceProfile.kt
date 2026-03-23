package com.wahon.shared.data.repository.aix

enum class AixSourceFamily {
    MULTI_CHAN,
    MADARA,
    GROUPLE,
    JSON_API,
    CUSTOM,
}

data class AixSourceProfile(
    val profileId: String,
    val family: AixSourceFamily,
    val sourceIds: Set<String> = emptySet(),
    val hosts: Set<String> = emptySet(),
) {
    fun matches(source: AixSourceDescriptor): Boolean {
        return source.matchesId(sourceIds) || source.matchesHost(hosts)
    }
}

abstract class ProfiledAixSourceAdapter<P : Any> : AixSourceAdapter {
    protected abstract val profiles: List<P>
    protected abstract fun profileMatches(
        source: AixSourceDescriptor,
        profile: P,
    ): Boolean

    final override fun supports(source: AixSourceDescriptor): Boolean {
        return findProfile(source) != null
    }

    protected fun findProfile(source: AixSourceDescriptor): P? {
        return profiles.firstOrNull { profile ->
            profileMatches(source = source, profile = profile)
        }
    }

    protected fun requireProfile(source: AixSourceDescriptor): P {
        return findProfile(source)
            ?: error("Adapter $adapterId has no profile for source: ${source.extensionId}/${source.declaredSourceId.orEmpty()}")
    }
}
