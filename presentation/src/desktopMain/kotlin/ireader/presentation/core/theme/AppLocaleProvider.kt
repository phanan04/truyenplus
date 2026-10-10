package ireader.presentation.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import java.util.Locale

/**
 * Desktop implementation of AppLocaleProvider.
 * 
 * Sets the JVM default locale which is used by Compose Resources
 * to determine which resource files to load.
 * 
 * The locale composition local ensures recomposition when language changes,
 * which triggers Compose Resources to reload strings in the new locale.
 */
@Composable
actual fun AppLocaleProvider(
    localeHelper: LocaleHelper,
    content: @Composable () -> Unit
) {
    val currentLanguage by localeHelper.currentLanguageFlow.collectAsState()
    
    // Apply locale change when language preference changes
    LaunchedEffect(currentLanguage) {
        if (currentLanguage.isNotEmpty()) {
            val locale = Locale.forLanguageTag(currentLanguage.replace('_', '-'))
            Locale.setDefault(locale)
        }
    }

    
    // Invalidate localized UI when language changes without recreating its state
    // This ensures Compose Resources reload strings in the new locale
    CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
        content()
    }
}
