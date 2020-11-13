import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.dokka") version "1.4.0"
}

group = "com.cubearrow"
version = "1.0-SNAPSHOT"
repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
}



tasks.test {
    useJUnitPlatform()
}


val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Main-Class"] = "com.cubearrow.cubelang.main.MainKt"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { zipTree(it) }
    })
}
