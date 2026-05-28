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
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(libs.koalaplot.core)
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.strikt.core)
    testImplementation(libs.jetbrains.compose.ui.test.junit4)
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("desktopTest") {
    description = "Runs the desktop UI tests."
    group = "verification"
    
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    
    useJUnitPlatform()
    
    // UI tests often need more memory
    jvmArgs("-Xmx2g")
}
