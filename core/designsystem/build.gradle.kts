plugins {
    alias(libs.plugins.ordo.android.library)
    alias(libs.plugins.ordo.android.compose)
    alias(libs.plugins.ordo.screenshot)
}

android {
    namespace = "io.github.tombolinero.ordo.core.designsystem"
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.animation)
    api(libs.compose.material3)
    implementation(libs.androidx.core.ktx)
}
