val logbackVersion = "1.5.19"
val seleniumVersion = "4.38.0"

plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.1"
}

group = "ru.kutoven"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    implementation("org.apache.commons:commons-csv:1.11.0")
}

repositories {
    mavenCentral()
}