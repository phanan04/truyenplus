package ireader.presentation.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ireader.domain.usecases.backup.GoogleDriveOAuthHandler
import ireader.presentation.core.LocalNavigator
import ireader.presentation.core.safePopBackStack
import ireader.presentation.ui.settings.backups.CloudBackupScreen
import ireader.presentation.ui.settings.backups.GoogleDriveViewModel

/**
 * Screen spec for Cloud Backup
 * Now unified with Google Drive backup - uses the same modern UI
 */
class CloudBackupScreenSpec {

    @Composable
    fun Content() {
        val navController = requireNotNull(LocalNavigator.current) { "LocalNavigator not provided" }
        val viewModel: GoogleDriveViewModel = getIViewModel()
        
        // Check for pending OAuth callback data
        LaunchedEffect(Unit) {
            if (GoogleDriveOAuthHandler.hasPendingData()) {
                val authCode = GoogleDriveOAuthHandler.pendingAuthCode
                val error = GoogleDriveOAuthHandler.pendingError
                
                when {
                    error != null -> {
                        viewModel.onOAuthError("Authentication failed: $error")
                    }
                    authCode != null -> {
                        processOAuthCallback(viewModel, authCode)
                    }
                }
            }
        }
        
        CloudBackupScreen(
            onPopBackStack = { navController.safePopBackStack() },
            viewModel = viewModel,
            onStartOAuthFlow = {
                startOAuthFlowWithViewModel(viewModel)
            }
        )
    }
}
