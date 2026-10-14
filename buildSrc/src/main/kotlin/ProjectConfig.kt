import org.gradle.api.JavaVersion

object ProjectConfig {
    const val minSdk = 26
    const val targetSdk = 37
    const val compileSdk = 37
    const val ndk = "21.3.6528147"
    const val versionName = "2.0.42"
    const val versionCode = 96
    const val applicationId = "ir.kazemcodes.infinityreader"

    val desktopJvmTarget = JavaVersion.VERSION_21
    val androidJvmTarget = JavaVersion.VERSION_21
    val toolChain = 14
}
