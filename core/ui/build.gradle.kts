plugins {
    alias(libs.plugins.ordo.android.library)
    alias(libs.plugins.ordo.android.compose)
}

android {
    namespace = "io.github.tombolinero.ordo.core.ui"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    api(project(":core:data"))
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
}
