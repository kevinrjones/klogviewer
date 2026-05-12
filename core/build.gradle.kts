plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.strikt.core)
    testImplementation(libs.kluent)
}

tasks.test {
    useJUnitPlatform()
}
