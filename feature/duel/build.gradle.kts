plugins {
    alias(libs.plugins.ordo.android.feature)
}

android {
    namespace = "io.github.tombolinero.ordo.feature.duel"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
