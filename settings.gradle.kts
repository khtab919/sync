pluginManagement {
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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    maven("https://maven-central.storage-download.googleapis.com/maven2") {
      content { includeModule("org.robolectric", "nativeruntime-dist-compat") }
    }
    mavenCentral()
  }
}

rootProject.name = "Layali Lounge"

include(":app")
