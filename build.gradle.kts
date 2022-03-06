plugins {
    kotlin("jvm") version "1.6.20-RC"
    id("org.jetbrains.dokka") version "1.6.10"

    id("application")
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
group = "com.cubearrow"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.cubearrow.cubelang.main.MainKt")
}