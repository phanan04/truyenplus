package ireader.presentation.ui.home.tts

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ireader.domain.preferences.models.prefs.IReaderVoice
import ireader.domain.usecases.preferences.TextReaderPrefUseCase
import ireader.domain.usecases.tts.GetNativeTTSVoicesUseCase
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale

/**
 * Native TTS Voice Selection Screen for Android
 * 
 * Displays available voices and languages from the device's native TTS engine.
 * Only shown when Native TTS engine is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeTTSVoiceSelectionScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localizeHelper = LocalLocalizeHelper.currentOrThrow
    val scope = rememberCoroutineScope()
    
    val getNativeTTSVoicesUseCase = remember { GetNativeTTSVoicesUseCase(context) }
    val textReaderPrefUseCase: TextReaderPrefUseCase = koinInject()
    
    var availableLanguages by remember { mutableStateOf<List<Locale>>(emptyList()) }
    var availableVoices by remember { mutableStateOf<List<IReaderVoice>>(emptyList()) }
    var selectedLanguage by remember { mutableStateOf<Locale?>(null) }
    var selectedVoice by remember { mutableStateOf<IReaderVoice?>(null) }
    var saving by remember {mutableStateOf(false)}
    var languageRequest by remember {mutableIntStateOf(0)}
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load saved preferences
    LaunchedEffect(Unit) {
        selectedVoice = textReaderPrefUseCase.readVoice()
        val savedLanguage = textReaderPrefUseCase.readLanguage()
        
        // Load available languages
        getNativeTTSVoicesUseCase.getAvailableLanguages()
            .onSuccess { languages ->
                availableLanguages = languages.sortedBy { it.displayName }
                
                // Set selected language from saved preference or default
                selectedLanguage = if (savedLanguage.isNotEmpty()) {
                    languages.find { it.language == savedLanguage } ?: Locale.getDefault()
                } else {
                    Locale.getDefault()
                }
                
                // Load voices for selected language
                selectedLanguage?.let { locale ->
                    loadVoicesForLanguage(locale, getNativeTTSVoicesUseCase) { voices ->
                        availableVoices = voices
                        isLoading = false
                    }
                }
            }
            .onFailure { error ->
                errorMessage = error.message
                isLoading = false
            }
    }
    
    ireader.presentation.ui.truyenplus.ReaderFloatingTaskPanel(localizeHelper.localize(Res.string.tp_system_voice),{if(!saving)onDismiss()}) {
            Column(modifier.fillMaxWidth()) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: localizeHelper.localize(Res.string.tp_operation_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Language Selector
                    LanguageSelector(
                        languages = availableLanguages,
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { locale ->
                            selectedLanguage = locale
                            isLoading = true
                            val request=++languageRequest
                            scope.launch {
                                loadVoicesForLanguage(locale, getNativeTTSVoicesUseCase) { voices ->
                                    if(request==languageRequest){availableVoices=voices;isLoading=false}
                                }
                            }
                        },
                        localizeHelper = localizeHelper
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Voice List
                    VoiceList(
                        voices = availableVoices,
                        selectedVoice = selectedVoice,
                        onVoiceSelected = { voice ->
                            selectedVoice = voice
                        },
                        localizeHelper = localizeHelper,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick={
                        val voice=selectedVoice?:return@Button
                        val language=selectedLanguage?:return@Button
                        saving=true;scope.launch {
                            try {
                                textReaderPrefUseCase.saveLanguage(language.language)
                                textReaderPrefUseCase.saveVoice(voice)
                                onDismiss()
                            } catch(e:kotlinx.coroutines.CancellationException){throw e}
                            catch(_:Exception){errorMessage=localizeHelper.localize(Res.string.tp_operation_failed)}
                            finally {saving=false}
                        }
                    },enabled=!saving && !isLoading && selectedVoice?.let {it in availableVoices}==true,
                        modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                        Text(localizeHelper.localize(if(saving)Res.string.tp_edit_load else Res.string.tp_voice_use))
                    }
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    languages: List<Locale>,
    selectedLanguage: Locale?,
    onLanguageSelected: (Locale) -> Unit,
    localizeHelper: ireader.i18n.LocalizeHelper,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Text(
            text = localizeHelper.localize(Res.string.language),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedLanguage?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { locale ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = locale.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onLanguageSelected(locale)
                            expanded = false
                        },
                        leadingIcon = if (locale == selectedLanguage) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceList(
    voices: List<IReaderVoice>,
    selectedVoice: IReaderVoice?,
    onVoiceSelected: (IReaderVoice) -> Unit,
    localizeHelper: ireader.i18n.LocalizeHelper,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = localizeHelper.localize(Res.string.available_voices),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (voices.isEmpty()) {
            Text(
                text = localizeHelper.localize(Res.string.no_voices_available_for_this_language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(voices, key = { it.name }) { voice ->
                    VoiceItem(
                        voice = voice,
                        isSelected = voice.name == selectedVoice?.name,
                        onSelected = { onVoiceSelected(voice) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceItem(
    voice: IReaderVoice,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.small,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voice.localDisplayName.ifEmpty { voice.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (voice.country.isNotEmpty()) {
                    Text(
                        text = "${voice.language}-${voice.country}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private suspend fun loadVoicesForLanguage(
    locale: Locale,
    getNativeTTSVoicesUseCase: GetNativeTTSVoicesUseCase,
    onResult: (List<IReaderVoice>) -> Unit
) {
    getNativeTTSVoicesUseCase.getVoicesForLanguage(locale)
        .onSuccess { voices ->
            onResult(voices.sortedBy { it.localDisplayName.ifEmpty { it.name } })
        }
        .onFailure {
            onResult(emptyList())
        }
}
