# LangMan – I18n

LangMan is a language manager for your project. It helps you to manage your language files and translations.

Latest version: ![Dynamic XML Badge](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo.ririfa.net%2Frepository%2Fmaven-public%2Fnet%2Fririfa%2Flangman%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Flatest&prefix=v&style=plastic&logo=sonatype&logoColor=56ba5e&label=Nexus&color=56ba5e)

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