import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URI

plugins {
    // Kotlin
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.gradleup.shadow") version "9.2.2"
    `maven-publish`
}

val coreVer = "2.0.3"
val yamlVer = "2.0.1"

allprojects {
    group = "net.ririfa"
    version = when (name) {
        "langman-core" -> coreVer
        "langman-ext.yaml" -> yamlVer
        else -> "1.0.0"
    }

    repositories {
        mavenCentral()
    }

    afterEvaluate {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
            api("org.slf4j:slf4j-api:2.1.0-alpha1")
        }
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    when (name) {
        "langman-ext.yaml" -> {
            afterEvaluate {
                dependencies {
                    compileOnly(project(":langman-core"))
                }
            }
        }
    }

    java { withSourcesJar() }

    kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

    tasks.withType<KotlinCompile> { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    tasks.withType<JavaCompile> { options.release.set(17) }

    tasks.shadowJar {
        archiveClassifier.set("all")

        dependencies {
            exclude(dependency("org.jetbrains.kotlin:.*"))
            exclude(dependency("org.jetbrains.kotlinx:.*"))
            exclude(dependency("org.jetbrains:annotations"))
        }

        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/services/*kotlin*")
        exclude("META-INF/*kotlin*")
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        onlyIf {
            val artifactId = project.name
            val ver = project.version.toString()
            val repoUrl = if (ver.endsWith("SNAPSHOT")) {
                "https://repo.ririfa.net/maven2-snap/"
            } else {
                "https://repo.ririfa.net/maven2-rel/"
            }

            val artifactUrl = "${repoUrl}net/ririfa/$artifactId/$ver/$artifactId-$ver.jar"
            logger.lifecycle("Checking existence of artifact at: $artifactUrl")

            val connection = URI(artifactUrl).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val exists = connection.responseCode == HttpURLConnection.HTTP_OK
            connection.disconnect()

            if (exists) {
                logger.lifecycle("Artifact already exists at $artifactUrl, skipping publish.")
                false
            } else {
                logger.lifecycle("Artifact not found at $artifactUrl, proceeding with publish.")
                true
            }
        }
    }

    tasks.register<Jar>("javadocJar") {
        dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
        from(tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml").flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])
                artifact(tasks.named("javadocJar"))

                pom {
                    name.set(project.name)
                    description.set("")
                    url.set("https://github.com/ririf4/Yacla")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/license/mit")
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
                        connection.set("scm:git:git://github.com/ririf4/Yacla.git")
                        developerConnection.set("scm:git:ssh://github.com/ririf4/Yacla.git")
                        url.set("https://github.com/ririf4/Yacla")
                    }
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
}