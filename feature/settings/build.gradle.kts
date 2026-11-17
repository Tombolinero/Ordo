plugins {
    alias(libs.plugins.ordo.android.feature)
}

android {
    namespace = "io.github.tombolinero.ordo.feature.settings"
}

dependencies {
    implementation(project(":core:backup"))
    implementation(libs.androidx.activity.compose)
}
