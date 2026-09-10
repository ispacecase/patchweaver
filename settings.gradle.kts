rootProject.name = "patchweaver"

pluginManagement {
    // Resolve the Morphe patches Gradle plugin from a local checkout instead of GitHub
    // Packages, which requires authentication even for public packages. Clone
    // https://github.com/MorpheApp/morphe-patches-gradle-plugin as a sibling of this repo.
    includeBuild("../morphe-patches-gradle-plugin")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("app.morphe.patches") version "1.3.4"
}

settings {
    patchesProjectPath = "patches"

    extensions {
        projectsPath = "extensions"
        defaultNamespace = "app.fdroidbackends.extension"
    }
}

include(":stub")
include(":apply-tool")
