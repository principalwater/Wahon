package com.wahon.shared.data.repository.aix

data class AixWasmInspection(
    val isAixPackage: Boolean,
    val declaredSourceId: String? = null,
    val declaredName: String? = null,
    val declaredLanguage: String? = null,
    val declaredVersion: Long? = null,
    val declaredMinAppVersion: String? = null,
    val hasMainModule: Boolean = false,
    val mainModuleImportModules: List<String> = emptyList(),
    val mainModuleImports: List<String> = emptyList(),
    val mainModuleExports: List<String> = emptyList(),
    val aidokuHostAbiDetected: Boolean = false,
    val isExecutable: Boolean,
    val runtimeMessage: String,
)

/**
 * Runtime contract for Aidoku .aix WASM packages.
 * Current implementation provides package inspection and ABI diagnostics.
 */
interface AixWasmRuntime {
    fun inspect(payload: ByteArray): AixWasmInspection

    suspend fun executeMethod(
        extensionId: String,
        payload: ByteArray,
        methodName: String,
        argsJson: List<String>,
    ): String
}
