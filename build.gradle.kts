import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.desktop) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.versionUpdate)
    alias(libs.plugins.catalogUpdate)
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}