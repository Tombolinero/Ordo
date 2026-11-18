plugins {
    alias(libs.plugins.ordo.android.feature)
}

android {
    namespace = "io.github.tombolinero.ordo.feature.ranking"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
