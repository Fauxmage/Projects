rootProject.name = "pebtip"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    defaultLibrariesExtensionName = "projectLibs"
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io")  // Required for libpebble3
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
        create("libpebble") {
            from(files("libs/libpebble3/gradle/libs.versions.toml"))
        }
    }
}

includeBuild("libs/libpebble3") {
   dependencySubstitution {
      substitute(module("com.coredevices:libpebble3"))
         .using(project(":libpebble3"))
   }
}

include(":composeApp")