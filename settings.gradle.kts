pluginManagement {
    repositories {
        google()
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
rootProject.name = "taptaptap"
include(":app")
include(":core")
include(":feature:game")
include(":feature:settings")
include(":feature:shop")
include(":feature:shop")