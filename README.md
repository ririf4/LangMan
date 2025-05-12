# LangMan – I18n

LangMan is a language manager for your project. It helps you to manage your language files and translations.

Latest
version: ![Dynamic XML Badge](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo.ririfa.net%2Frepository%2Fmaven-public%2Fnet%2Fririfa%2Flangman%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Flatest&style=plastic&logo=sonatype&label=Nexus)

## Installation

Gradle(Groovy):

```groovy
repositories {
    maven { url "https://repo.ririfa.net/maven2" }
}

implementation 'net.ririfa:langman:${version}'
```

Gradle(Kotlin):

```kotlin
repositories {
    maven("https://repo.ririfa.net/maven2")
}

implementation("net.ririfa:langman:${version}")
```

Maven:

```xml
<repositories>
    <repository>
        <id>ririfa-repo</id>
        <url>https://repo.ririfa.net/maven2</url>
    </repository>
</repositories>

<dependency>
<groupId>net.ririfa</groupId>
<artifactId>langman</artifactId>
<version>${version}</version>
</dependency>
```

Hint: You can find the all versions of the library on the [RiriFa Repo](https://repo.ririfa.net/service/rest/repository/browse/maven-public/net/ririfa/langman/)