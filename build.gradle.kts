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
version = "1.0.0"

application {
    mainClass.set("com.cubearrow.cubelang.main.MainKt")
}

tasks.jar {
    manifest{
        attributes["Main-Class"] = "com.cubearrow.cubelang.main.MainKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

}