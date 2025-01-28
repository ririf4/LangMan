# LangMan – I18n

LangMan is a language manager for your project. It helps you to manage your language files and translations.

Latest version: ![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https://repo1.maven.org/maven/net/rk4z/langman/maven-metadata.xml&style=plastic&logo=sonatype&label=Central&color=00FF87%20)

## Installation

Gradle(Groovy):

```groovy
repositories {
    mavenCentral()
}

implementation 'net.ririfa:langman:${version}'
```

Gradle(Kotlin):

```kotlin
repositories {
    mavenCentral()
}

implementation("net.ririfa:langman:${version}")
```

Maven:

```xml

<repositories>
    <repository>
        <id>central</id>
        <url>https://repo.maven.apache.org/maven2</url>
    </repository>
</repositories>

<dependency>
<groupId>net.ririfanet.ririfa</groupId>
<artifactId>langman</artifactId>
<version>${version}</version>
</dependency>
```

Hint: You can find the all version of the library on the [MavenCentral](https://central.sonatype.com/artifact/net.rk4z/langman/versions).

## Usage