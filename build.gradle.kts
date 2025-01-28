import cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadTask
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.Properties
import kotlin.apply

plugins {
    // Kotlin
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.dokka") version "2.0.0"

    // Publishing
    `maven-publish`
    id("cl.franciscosolis.sonatype-central-upload") version "1.0.3"
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}

group = "net.ririfa"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.classgraph:classgraph:4.8.179")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    implementation("org.yaml:snakeyaml:2.3")
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("com.google.code.gson:gson:2.11.0")
}

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("")
}

tasks.dokkaHtml.configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
}

publishing {
    publications {
        //maven
        create<MavenPublication>("maven") {

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("LangMan")
                description.set("I18n library supporting multiple file types")
                url.set("https://github.com/ririf4/LangMan")
                licenses {
                    license {
                        name.set("S1-OP")
                        url.set("https://github.com/SwiftStorm-Studio/LICENSES/blob/main/S1-OP")
                    }
                }
                developers {
                    developer {
                        id.set("ririfa")
                        name.set("RiriFa")
                        email.set("main@ririfa.net")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ririf4/LangMan.git")
                    developerConnection.set("scm:git:ssh://github.com/ririf4/LangMan.git")
                    url.set("https://github.com/ririf4/LangMan")
                }
                dependencies
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

tasks.named<SonatypeCentralUploadTask>("sonatypeCentralUpload") {
    dependsOn("clean", "jar", "sourcesJar", "javadocJar", "generatePomFileForMavenPublication")

    username = localProperties.getProperty("cu")
    password = localProperties.getProperty("cp")

    archives = files(
        tasks.named("jar"),
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
    )

    pom = file(
        tasks.named("generatePomFileForMavenPublication").get().outputs.files.single()
    )

    signingKey = localProperties.getProperty("signing.key")
    signingKeyPassphrase = localProperties.getProperty("signing.passphrase")
}
