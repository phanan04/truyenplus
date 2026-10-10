package ireader.presentation.ui.truyenplus

import ireader.domain.data.repository.DiscoveryRepository
import ireader.presentation.ui.core.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

class DiscoveryViewModel(private val repository: DiscoveryRepository) : BaseViewModel() {
    val state = repository.state
    fun load() { scope.launch { repository.refresh() } }
    fun refresh() { scope.launch { repository.refresh(force = true) } }
}
