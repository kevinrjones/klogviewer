plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.desktop)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.strikt.core)
}

tasks.test {
    useJUnitPlatform()
}
