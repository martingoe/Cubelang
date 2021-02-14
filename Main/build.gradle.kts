plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

group = "com.cubearrow"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(project(":Common"))
    implementation(project(":Frontend"))
    implementation(project(":X86_64Backend"))
}
