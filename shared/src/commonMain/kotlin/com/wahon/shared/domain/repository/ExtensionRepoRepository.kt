package com.wahon.shared.domain.repository

import com.wahon.shared.domain.model.ExtensionInfo
import com.wahon.shared.domain.model.ExtensionRepo
import kotlinx.coroutines.flow.Flow

interface ExtensionRepoRepository {
    fun getRepos(): Flow<List<ExtensionRepo>>
    suspend fun addRepo(url: String): Result<ExtensionRepo>
    suspend fun removeRepo(url: String)
    suspend fun fetchExtensionsFromRepo(repoUrl: String): Result<List<ExtensionInfo>>
    suspend fun fetchAllExtensions(): Result<List<ExtensionInfo>>
}
