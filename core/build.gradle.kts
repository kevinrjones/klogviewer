plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)
    api(libs.sshj)
    implementation(libs.aws.s3)
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.strikt.core)
    testImplementation(libs.kluent)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sshd.core)
    testImplementation(libs.sshd.sftp)
    testRuntimeOnly(libs.logback.classic)
}

tasks.test {
    useJUnitPlatform()
}
