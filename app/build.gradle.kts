plugins {
    alias(libs.plugins.ordo.android.application)
    alias(libs.plugins.ordo.android.compose)
    alias(libs.plugins.ordo.screenshot)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Version comes from `-Pordo.version=1.2.0` (set by the release workflow).
 * versionCode = major * 10000 + minor * 100 + patch, so it always grows with the version.
 */
val ordoVersion: String = providers.gradleProperty("ordo.version").getOrElse("0.1.0")
val ordoVersionCode: Int = ordoVersion.split(".").map { it.toInt() }.let { (major, minor, patch) ->
    require(minor < 100 && patch < 100) { "minor and patch must be < 100" }
    major * 10000 + minor * 100 + patch
}

android {
    namespace = "io.github.tombolinero.ordo"
    defaultConfig {
        applicationId = "io.github.tombolinero.ordo"
        versionCode = ordoVersionCode
        versionName = ordoVersion
    }

    // Release signing: the keystore is never in the repository. CI decodes it from GitHub Secrets
    // and passes the location and passwords through environment variables (see docs/RELEASING.md).
    val keystorePath = System.getenv("ORDO_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ORDO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ORDO_KEY_ALIAS")
                keyPassword = System.getenv("ORDO_KEY_PASSWORD")
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Ordo Debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "Ordo")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        resValues = true
        buildConfig = true
    }

    androidResources {
        // Only ship the languages the app is translated to (keeps libraries' extra locales out).
        localeFilters += listOf("en", "de")
        generateLocaleConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
    }

    dependenciesInfo {
        // Obtainium / F-Droid friendly: no encrypted Google dependency blob in the APK.
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:lists"))
    implementation(project(":feature:ranking"))
    implementation(project(":feature:item"))
    implementation(project(":feature:duel"))
    implementation(project(":feature:search"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.glance.appwidget)
}
