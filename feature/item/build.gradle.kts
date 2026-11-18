plugins {
    alias(libs.plugins.ordo.android.feature)
}

android {
    namespace = "io.github.tombolinero.ordo.feature.item"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
