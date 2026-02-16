plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.amos_tech_code"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.ktor.server.rate.limiting)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)

    // Postgresql
    implementation(libs.postgresql)

    // Exposed ORM
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)

    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation(kotlin("stdlib-jdk8"))

    // Hikari CP
    implementation("com.zaxxer:HikariCP:5.1.0")
    // BCrypt
    implementation("org.mindrot:jbcrypt:0.4")
    // Environment Variables
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    // Ktor client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    // SSE
    implementation("io.ktor:ktor-server-sse:3.3.0")
    // Jackson JSON serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    // QR Code generation
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.google.zxing:javase:3.5.1")
    // Cloudinary SDK
    implementation("com.cloudinary:cloudinary-http44:1.36.0")
    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.2.5")
    // CSV Generation
    implementation("com.opencsv:opencsv:5.8")

}

repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(17)
}
