package org.ireader.app.initiators

import android.app.Application
import android.content.pm.PackageManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.ireader.app.BuildConfig
import ireader.domain.preferences.prefs.AppPreferences
import ireader.domain.services.update_service.UpdateService
import ireader.core.log.Log



class UpdateServiceInitializer(app: Application, appPreferences: AppPreferences) {

    init {
        val appUpdaterEnabled = appPreferences.appUpdater().get()
        val isDebug = BuildConfig.DEBUG
        val isPreview = BuildConfig.PREVIEW
        // Truyen+ already owns updates through its signed update.json flow. Changing the
        // runtime debug flag must not wake the inherited IReader release checker.
        val usesManifestUpdater = app.packageManager.getApplicationInfo(app.packageName,PackageManager.GET_META_DATA)
            .metaData?.getString("truyenplus.UPDATE_MANIFEST_URL").isNullOrBlank().not()
        
        Log.info { "UpdateServiceInitializer: appUpdater=$appUpdaterEnabled, DEBUG=$isDebug, PREVIEW=$isPreview" }
        
        if (appUpdaterEnabled && !isDebug && !isPreview && !usesManifestUpdater) {
            Log.info { "UpdateServiceInitializer: Scheduling update check" }
            val updateRequest = OneTimeWorkRequestBuilder<UpdateService>().build()
            val manager = WorkManager.getInstance(app)
            manager.enqueue(updateRequest)
        } else {
            Log.info { "UpdateServiceInitializer: Update check skipped - " +
                when {
                    usesManifestUpdater -> "using the Truyen+ manifest updater"
                    !appUpdaterEnabled -> "app updater is disabled in settings"
                    isDebug -> "running in DEBUG mode"
                    isPreview -> "running in PREVIEW mode"
                    else -> "unknown reason"
                }
            }
        }
    }
}
