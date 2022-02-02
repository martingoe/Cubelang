plugins {
    kotlin("jvm")
    application
    id("org.jetbrains.dokka")
}

group = "com.cubearrow"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Common"))
    implementation(project(":Frontend"))
    implementation(project(":InstructionSelection"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.5.21")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.0")
}


application {
    mainClass.set("com.cubearrow.cubelang.main.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.cubearrow.cubelang.main.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}
