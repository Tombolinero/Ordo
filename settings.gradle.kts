pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Ordo"

include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:database")
include(":core:backup")
include(":core:data")
include(":core:ui")
include(":feature:lists")
include(":feature:ranking")
include(":feature:item")
include(":feature:duel")
include(":feature:search")
include(":feature:settings")
