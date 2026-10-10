package ireader.presentation.ui.truyenplus

import ireader.domain.data.repository.*
import ireader.domain.models.discovery.*
import ireader.presentation.ui.core.viewmodel.BaseViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DiscoveryTranslationState(
    val language: String = "vi", val values: Map<String, DiscoveryTranslation> = emptyMap(),
    val translating: Boolean = false, val failed: Boolean = false,
)

/** Restart the translation queue when the language, selected source or source metadata changes. */
class DiscoveryViewModel(private val repository: DiscoveryRepository, private val translations: DiscoveryTranslations) : BaseViewModel() {
    val state = repository.state
    private data class Selection(val source: DiscoverySource, val language: String, val retry: Int = 0)
    private val selection = MutableStateFlow<Selection?>(null)
    private val mutableTranslations = MutableStateFlow(DiscoveryTranslationState())
    val translated = mutableTranslations.asStateFlow()
    init {
        scope.launch {
            combine(state.map { it.feeds }.distinctUntilChanged(), selection.filterNotNull()) { feeds, selected ->
                selected to feeds[selected.source]?.books.orEmpty()
            }.collectLatest { (selected, books) ->
                mutableTranslations.value = DiscoveryTranslationState(selected.language)
                if (selected.source.language == selected.language) return@collectLatest
                // Restore every cached entry before waiting for language packs or new translations.
                val cached = books.mapNotNull { book -> translations.cached(book, selected.language)?.let { book.key to it } }.toMap()
                mutableTranslations.value = DiscoveryTranslationState(selected.language, cached, books.any { it.key !in cached })
                try {
                    for (book in books) {
                        if (book.key in mutableTranslations.value.values) continue
                        val value = translations.translate(book, selected.language)
                        mutableTranslations.update { it.copy(values = it.values + (book.key to value)) }
                    }
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { mutableTranslations.update { it.copy(failed = true) } }
                finally { mutableTranslations.update { it.copy(translating = false) } }
            }
        }
    }
    fun configure(source: DiscoverySource, language: String) {
        val code = ReaderUiLanguage.fromCode(language).code
        selection.update { if (it?.source == source && it.language == code) it else Selection(source, code) }
    }
    fun retryTranslation() { selection.update { it?.copy(retry = it.retry + 1) } }
    fun load() { scope.launch { repository.refresh() } }
    fun refresh() { scope.launch { repository.refresh(force = true) }; retryTranslation() }
}
