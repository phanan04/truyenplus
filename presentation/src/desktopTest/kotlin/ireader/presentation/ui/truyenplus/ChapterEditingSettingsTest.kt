package ireader.presentation.ui.truyenplus
import ireader.data.editing.SecureEditingSettings
import ireader.domain.models.editing.*
import ireader.domain.services.platform.*
import ireader.domain.services.common.ServiceResult
import kotlinx.coroutines.runBlocking
import kotlin.test.*
class ChapterEditingSettingsTest {
    private class Vault : SecureStorageService {
        val values=mutableMapOf<String,String>();var encrypted=true
        override suspend fun putString(key:String,value:String):ServiceResult<Unit> {values[key]=value;return ServiceResult.Success(Unit)}
        override suspend fun getString(key:String)=ServiceResult.Success(values[key])
        override suspend fun remove(key:String):ServiceResult<Unit> {values.remove(key);return ServiceResult.Success(Unit)}
        override suspend fun contains(key:String)=values.containsKey(key)
        override suspend fun clear():ServiceResult<Unit>{values.clear();return ServiceResult.Success(Unit)}
        override suspend fun getAllKeys()=values.keys.toList()
        override fun isSecureStorageAvailable()=true
        override fun getEncryptionLevel()=if(encrypted)EncryptionLevel.HARDWARE else EncryptionLevel.NONE
        override suspend fun initialize(){};override suspend fun start(){};override suspend fun stop(){};override fun isRunning()=false;override suspend fun cleanup(){}
        override suspend fun putBytes(key:String,value:ByteArray)=ServiceResult.Success(Unit)
        override suspend fun getBytes(key:String)=ServiceResult.Success<ByteArray?>(null)
    }
    @Test fun providerKeysAreSeparateAndBlankInputPreservesSavedKey()=runBlocking {
        val vault=Vault();val settings=SecureEditingSettings(vault)
        settings.save(EditingConfig(),"gkey")
        settings.save(EditingConfig(EditingProvider.DeepSeek),"dkey")
        settings.save(EditingConfig(),null)
        assertEquals("gkey",settings.credentials(EditingConfig()).apiKey)
        assertEquals("dkey",settings.credentials(EditingConfig(EditingProvider.DeepSeek)).apiKey)
        assertFalse(vault.values.getValue("chapter_edit_config").contains("gkey"))
        assertEquals(EditingConfig(),SecureEditingSettings(vault).load())
        settings.clearKey(EditingProvider.Gemini)
        assertFails{settings.credentials(EditingConfig())}; Unit
    }
    @Test fun insecureStorageNeverReceivesKeysAndInvalidModelsAreRejected()=runBlocking {
        val vault=Vault().apply{encrypted=false};val settings=SecureEditingSettings(vault)
        assertFails{settings.save(EditingConfig(),"key")}
        assertTrue(vault.values.isEmpty())
        assertFails{settings.save(EditingConfig(model="../bad?key=stolen"),"key")}; Unit
    }
}
