plugins {
    alias(libs.plugins.ordo.android.library)
    alias(libs.plugins.ordo.android.room)
}

android {
    namespace = "io.github.tombolinero.ordo.core.database"
    // Exported schemas are test assets so MigrationTestHelper can open every old version.
    sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    api(libs.room.runtime)
    api(libs.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.turbine)
}
