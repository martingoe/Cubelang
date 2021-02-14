plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

group = "com.cubearrow"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
