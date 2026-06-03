import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.desktop) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.versionUpdate)
    alias(libs.plugins.catalogUpdate)
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

val detektSourceDirectories = listOf(
    "app/src",
    "core/src",
    "domain/src",
    "ui/src"
)

subprojects {
    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            ignoreFailures = false
            config.setFrom(rootProject.file("detekt.yml"))
            baseline = rootProject.file("detekt-baseline.xml")
            basePath = rootProject.projectDir.absolutePath
        }

        tasks.withType<Detekt>().configureEach {
            jvmTarget = "17"
            reports {
                html.required.set(true)
                xml.required.set(true)
                md.required.set(true)
                sarif.required.set(true)
            }
        }

        tasks.withType<DetektCreateBaselineTask>().configureEach {
            enabled = false
        }

        tasks.named("check").configure {
            dependsOn("detekt")
        }
    }
}

tasks.named<DetektCreateBaselineTask>("detektBaseline") {
    description = "Generates a single Detekt baseline for all Kotlin modules."
    group = "verification"

    buildUponDefaultConfig.set(true)
    ignoreFailures.set(false)
    config.setFrom(files(rootProject.file("detekt.yml")))
    baseline.set(rootProject.file("detekt-baseline.xml"))
    basePath = rootProject.projectDir.absolutePath
    setSource(files(detektSourceDirectories.map(::file)))
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**")
    exclude("**/resources/**")
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