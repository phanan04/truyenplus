package ireader.presentation.ui.truyenplus

import java.io.File
import kotlin.test.*

/** Guard user-visible onboarding against legacy extension and hardcoded-language copy. */
class ReaderOnboardingCopyTest {
    @Test fun onboardingUsesLocalizedActionsAndDoesNotPromiseUnsupportedStorage() {
        val source=File("src/commonMain/kotlin/ireader/presentation/ui/onboarding/OnboardingScreen.kt").readText()
        assertFalse(source.contains("\"Change Folder\""))
        assertFalse(source.contains("\"Select Folder\""))
        assertFalse(source.contains("Text(\"Bắt đầu\")"))
        assertFalse(source.contains("Res.string.this_folder_will_be_used"))
        assertFalse(source.contains("Res.string.select_a_folder_where_ireader"))
    }
}
