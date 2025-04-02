import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Kotlin
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.dokka") version "2.0.0"
    `maven-publish`
}

group = "net.ririfa"
version = "1.4.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    implementation("org.yaml:snakeyaml:2.3")
    implementation("io.hotmoka:toml4j:0.7.3")
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

    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
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
        maven {
            val releasesRepoUrl = uri("https://repo.ririfa.net/maven2-rel/")
            val snapshotsRepoUrl = uri("https://repo.ririfa.net/maven2-snap/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = findProperty("nxUN").toString()
                password = findProperty("nxPW").toString()
            }
        }
    }
}