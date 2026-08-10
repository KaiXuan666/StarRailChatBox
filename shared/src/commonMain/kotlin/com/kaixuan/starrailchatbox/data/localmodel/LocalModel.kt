package com.kaixuan.starrailchatbox.data.localmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LocalModelSource { CATALOG, IMPORTED }

enum class LocalModelInstallState { NOT_INSTALLED, DOWNLOADING, VERIFYING, READY, FAILED }

enum class InferenceBackend { GPU, CPU }

enum class ChatModelMode { ONLINE, LOCAL }

data class LocalModel(
    val id: String,
    val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val sha256: String,
    val source: LocalModelSource,
    val sourceUrl: String?,
    val license: String,
    val contextWindow: Int,
    val maxOutputTokens: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

interface LocalModelRepository {
    fun observeModels(): Flow<List<LocalModel>>
    suspend fun getById(id: String): LocalModel?
    suspend fun upsert(model: LocalModel)
    suspend fun delete(id: String)
}

class InMemoryLocalModelRepository(
    initial: List<LocalModel> = emptyList(),
) : LocalModelRepository {
    private val models = MutableStateFlow(initial)

    override fun observeModels(): Flow<List<LocalModel>> = models.asStateFlow()

    override suspend fun getById(id: String): LocalModel? = models.value.firstOrNull { it.id == id }

    override suspend fun upsert(model: LocalModel) {
        models.value = models.value.filterNot { it.id == model.id } + model
    }

    override suspend fun delete(id: String) {
        models.value = models.value.filterNot { it.id == id }
    }
}

object LocalModelCatalog {
    val Qwen3_1_7B = CatalogLocalModel(
        id = "qwen3-1.7b",
        name = "Qwen3 1.7B",
        revision = "d9b8a9126e5ac18591306eacd4311ba43b92421e",
        fileName = "Qwen3_1.7B.litertlm",
        sizeBytes = 2_056_729_520L,
        sha256 = "66064a4e9269cb693e124c4e3040bcb8a446b10bca42663896329495add3861c",
        license = "Apache-2.0",
        contextWindow = 4_096,
        maxOutputTokens = 1_024,
    )
}

data class CatalogLocalModel(
    val id: String,
    val name: String,
    val revision: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val license: String,
    val contextWindow: Int,
    val maxOutputTokens: Int,
) {
    val downloadUrl: String =
        "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/$revision/$fileName"
}
