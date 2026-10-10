package ireader.domain.data.repository

import ireader.domain.models.discovery.DiscoveryState
import kotlinx.coroutines.flow.StateFlow

interface DiscoveryRepository {
    val state: StateFlow<DiscoveryState>
    suspend fun refresh(force: Boolean = false)
}
