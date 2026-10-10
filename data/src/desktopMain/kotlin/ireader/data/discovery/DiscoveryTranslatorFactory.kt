package ireader.data.discovery

import ireader.domain.data.repository.DiscoveryTranslator

/** ML Kit on-device models are Android-only. Never silently send metadata to an unofficial endpoint. */
actual fun createDiscoveryTranslator(): DiscoveryTranslator = DiscoveryTranslator { _, _ ->
    error("On-device catalog translation is available on Android")
}
