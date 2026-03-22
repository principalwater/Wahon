package com.wahon.shared.data.repository

import com.dokar.quickjs.quickJs
import com.wahon.extension.ChapterInfo
import com.wahon.extension.Filter
import com.wahon.extension.MangaInfo
import com.wahon.extension.MangaPage
import com.wahon.extension.PageInfo
import com.wahon.shared.data.local.ExtensionFileStore
import com.wahon.shared.data.local.WahonDatabase
import com.wahon.shared.domain.model.LoadedSource
import com.wahon.shared.domain.model.SourceRuntimeKind
import com.wahon.shared.domain.repository.ExtensionRuntimeRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExtensionRuntimeRepositoryImpl(
    private val database: WahonDatabase,
    private val extensionFileStore: ExtensionFileStore,
    private val sourceManager: SourceManager,
) : ExtensionRuntimeRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override val loadedSources: StateFlow<List<LoadedSource>> = sourceManager.loadedSources

    override suspend fun reloadInstalledSources(): Result<List<LoadedSource>> {
        return runCatching {
            val rows = database.installed_extensionQueries.selectAllInstalledExtensions().executeAsList()
            val loaded = mutableListOf<LoadedSource>()

            for (row in rows) {
                val extensionId = row.extension_id
                if (!extensionFileStore.exists(extensionId)) {
                    database.installed_extensionQueries.deleteInstalledExtensionById(extensionId)
                    continue
                }

                val payload = extensionFileStore.readExtension(extensionId)
                if (payload == null) {
                    database.installed_extensionQueries.deleteInstalledExtensionById(extensionId)
                    continue
                }

                val initialRuntime = detectRuntime(payload)
                val runtime = if (initialRuntime.runtimeKind == SourceRuntimeKind.JAVASCRIPT) {
                    val script = initialRuntime.script.orEmpty()
                    val syntaxIsValid = validateScriptSyntax(script = script)
                    if (syntaxIsValid) {
                        initialRuntime
                    } else {
                        Napier.w("Extension script validation failed: $extensionId")
                        initialRuntime.copy(
                            isExecutable = false,
                            runtimeMessage = "JavaScript syntax validation failed",
                        )
                    }
                } else {
                    initialRuntime
                }

                loaded += LoadedSource(
                    extensionId = extensionId,
                    sourceId = extensionId,
                    name = row.name,
                    language = row.languages_csv.substringBefore(',').ifBlank { "en" },
                    supportsNsfw = row.nsfw != 0L,
                    baseUrl = row.base_url,
                    localFilePath = row.local_file_path,
                    runtimeKind = runtime.runtimeKind,
                    isRuntimeExecutable = runtime.isExecutable,
                    runtimeMessage = runtime.runtimeMessage,
                )
            }

            sourceManager.replaceAll(loaded)
            loaded
        }
    }

    override fun getLoadedSource(extensionId: String): LoadedSource? {
        return sourceManager.get(extensionId)
    }

    override suspend fun getPopularManga(extensionId: String, page: Int): Result<MangaPage> {
        return executeSourceMethod(
            extensionId = extensionId,
            methodName = "getPopularManga",
            argsJson = listOf(page.toString()),
        )
    }

    override suspend fun searchManga(
        extensionId: String,
        query: String,
        page: Int,
        filters: List<Filter>,
    ): Result<MangaPage> {
        return executeSourceMethod(
            extensionId = extensionId,
            methodName = "searchManga",
            argsJson = listOf(
                json.encodeToString(query),
                page.toString(),
                json.encodeToString(filters),
            ),
        )
    }

    override suspend fun getMangaDetails(
        extensionId: String,
        mangaUrl: String,
    ): Result<MangaInfo> {
        return executeSourceMethod(
            extensionId = extensionId,
            methodName = "getMangaDetails",
            argsJson = listOf(json.encodeToString(mangaUrl)),
        )
    }

    override suspend fun getChapterList(
        extensionId: String,
        mangaUrl: String,
    ): Result<List<ChapterInfo>> {
        return executeSourceMethod(
            extensionId = extensionId,
            methodName = "getChapterList",
            argsJson = listOf(json.encodeToString(mangaUrl)),
        )
    }

    override suspend fun getPageList(
        extensionId: String,
        chapterUrl: String,
    ): Result<List<PageInfo>> {
        return executeSourceMethod(
            extensionId = extensionId,
            methodName = "getPageList",
            argsJson = listOf(json.encodeToString(chapterUrl)),
        )
    }

    private suspend fun validateScriptSyntax(script: String): Boolean {
        return runCatching {
            quickJs {
                compile(script)
            }
        }.isSuccess
    }

    private suspend inline fun <reified T> executeSourceMethod(
        extensionId: String,
        methodName: String,
        argsJson: List<String>,
    ): Result<T> {
        return runCatching {
            val loadedSource = sourceManager.get(extensionId)
                ?: error("Source is not loaded: $extensionId")
            if (!loadedSource.isRuntimeExecutable || loadedSource.runtimeKind != SourceRuntimeKind.JAVASCRIPT) {
                val reason = loadedSource.runtimeMessage ?: "Runtime ${loadedSource.runtimeKind} is not executable"
                error("Source $extensionId cannot run method $methodName: $reason")
            }

            val payload = extensionFileStore.readExtension(extensionId)
                ?: error("Extension payload not found: $extensionId")
            val runtime = detectRuntime(payload)
            if (runtime.runtimeKind != SourceRuntimeKind.JAVASCRIPT || !runtime.isExecutable) {
                error("Source $extensionId runtime is ${runtime.runtimeKind} and cannot execute methods")
            }
            val sourceScript = runtime.script
                ?: error("JavaScript runtime script is missing for source: $extensionId")

            val invocationScript = buildMethodInvocationScript(
                methodName = methodName,
                argsJson = argsJson,
            )
            val executableScript = buildExecutableScript(
                sourceScript = sourceScript,
                invocationScript = invocationScript,
            )
            val resultJson: String = quickJs {
                evaluate<String>(executableScript)
            }
            if (resultJson == "null") {
                error("Source method returned null: $methodName")
            }
            json.decodeFromString<T>(resultJson)
        }
    }

    private fun detectRuntime(payload: ByteArray): RuntimeInspection {
        if (payload.isEmpty()) {
            return RuntimeInspection(
                runtimeKind = SourceRuntimeKind.UNKNOWN,
                isExecutable = false,
                runtimeMessage = "Extension payload is empty",
            )
        }

        if (isZipArchive(payload)) {
            return RuntimeInspection(
                runtimeKind = SourceRuntimeKind.AIDOKU_AIX,
                isExecutable = false,
                runtimeMessage = "Aidoku .aix package detected (WASM runtime is not implemented yet)",
            )
        }

        val decoded = payload.decodeToString()
        if (decoded.isBlank()) {
            return RuntimeInspection(
                runtimeKind = SourceRuntimeKind.UNKNOWN,
                isExecutable = false,
                runtimeMessage = "Extension payload is not readable text",
            )
        }

        val printableCount = decoded.count { char ->
            char == '\n' || char == '\r' || char == '\t' || (char.code in 32..126)
        }
        val printableRatio = printableCount.toDouble() / decoded.length.toDouble()
        if ('\u0000' in decoded || printableRatio < MIN_PRINTABLE_RATIO) {
            return RuntimeInspection(
                runtimeKind = SourceRuntimeKind.UNKNOWN,
                isExecutable = false,
                runtimeMessage = "Unknown extension payload format",
            )
        }

        return RuntimeInspection(
            runtimeKind = SourceRuntimeKind.JAVASCRIPT,
            script = decoded,
            isExecutable = true,
        )
    }

    private fun isZipArchive(payload: ByteArray): Boolean {
        if (payload.size < 4) return false
        return payload[0] == ZIP_SIGNATURE_1 &&
            payload[1] == ZIP_SIGNATURE_2 &&
            payload[2] == ZIP_SIGNATURE_3 &&
            payload[3] == ZIP_SIGNATURE_4
    }

    private fun buildMethodInvocationScript(
        methodName: String,
        argsJson: List<String>,
    ): String {
        val callArgs = if (argsJson.isEmpty()) "" else ", ${argsJson.joinToString(separator = ", ")}"
        return """
            (async () => {
                const sourceCandidate = globalThis.source ?? globalThis.Source ?? null;
                const target = sourceCandidate && typeof sourceCandidate["$methodName"] === "function"
                    ? sourceCandidate
                    : globalThis;
                const method = target["$methodName"];
                if (typeof method !== "function") {
                    throw new Error("Source method '$methodName' not found");
                }
                const result = await method.call(target$callArgs);
                return JSON.stringify(result ?? null);
            })();
        """.trimIndent()
    }

    private fun buildExecutableScript(
        sourceScript: String,
        invocationScript: String,
    ): String {
        return buildString {
            appendLine(sourceScript)
            appendLine()
            appendLine(invocationScript)
        }
    }

    private data class RuntimeInspection(
        val runtimeKind: SourceRuntimeKind,
        val script: String? = null,
        val isExecutable: Boolean,
        val runtimeMessage: String? = null,
    )

    private companion object {
        private const val MIN_PRINTABLE_RATIO = 0.70
        private const val ZIP_SIGNATURE_1: Byte = 0x50
        private const val ZIP_SIGNATURE_2: Byte = 0x4B
        private const val ZIP_SIGNATURE_3: Byte = 0x03
        private const val ZIP_SIGNATURE_4: Byte = 0x04
    }
}
