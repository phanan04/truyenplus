package ireader.data.editing

import ireader.domain.models.editing.*
import ireader.domain.services.platform.*
import ireader.domain.services.common.ServiceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Lazy secure-store lookup keeps Android Keystore creation off the UI thread. */
class SecureEditingSettings(private val vault: ()->SecureStorageService): EditingSettings {
    constructor(vault: SecureStorageService): this({vault})
    private val mutex=Mutex()
    private fun key(provider: EditingProvider)="chapter_edit_key_${provider.name.lowercase()}"
    private fun <T> ServiceResult<T>.checked(): T = when(this) {
        is ServiceResult.Success -> data
        else -> error("Không thể truy cập kho lưu API key an toàn.")
    }
    override suspend fun load(): EditingConfig = withContext(Dispatchers.IO) { mutex.withLock {
        vault().getString("chapter_edit_config").checked()?.let { runCatching{Json.decodeFromString<EditingConfig>(it)}.getOrNull() } ?: EditingConfig()
    } }
    override suspend fun save(config: EditingConfig,newKey: String?) = withContext(Dispatchers.IO) { mutex.withLock {
        validateEditingConfig(config)
        val secure=vault()
        if(!newKey.isNullOrBlank()) {
            require(secure.isSecureStorageAvailable() && secure.getEncryptionLevel() in listOf(EncryptionLevel.HARDWARE,EncryptionLevel.HARDWARE_BIOMETRIC)) { "Thiết bị chưa hỗ trợ lưu API key an toàn." }
            val clean=newKey.trim()
            require(clean.length<=512 && clean.none {it.isWhitespace() || it.isISOControl()}) { "API key không hợp lệ." }
            secure.putString(key(config.provider),clean).checked()
        }
        secure.putString("chapter_edit_config",Json.encodeToString(config)).checked()
        Unit
    } }
    override suspend fun credentials(config: EditingConfig): EditingCredentials = withContext(Dispatchers.IO) { mutex.withLock {
        validateEditingConfig(config)
        val value=vault().getString(key(config.provider)).checked().orEmpty()
        require(value.isNotBlank()) { "Chưa có API key ${config.provider}. Mở Cài đặt biên tập để nhập key." }
        EditingCredentials(config,value)
    } }
    override suspend fun clearKey(provider: EditingProvider) = withContext(Dispatchers.IO) { mutex.withLock { vault().remove(key(provider)).checked() } }
}
