pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    buildscript {
        repositories { google(); mavenCentral() }
        // Firebase Auth 25.0.0 uses Kotlin 2.4 metadata; use a compatible shrinker.
        dependencies { classpath("com.android.tools:r8:9.4.14") }
    }
}
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "SentinelAuthenticator"
include(":app")
