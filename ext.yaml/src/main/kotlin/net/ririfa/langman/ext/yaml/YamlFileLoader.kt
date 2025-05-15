package net.ririfa.langman.ext.yaml

import net.ririfa.langman.FileLoader
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class YamlFileLoader(
    private val yaml: Yaml = Yaml()
) : FileLoader<Map<String, Any>> {

    override val fileExtension: Set<String>
        get() = setOf("yaml", "yml")

    @Suppress("UNCHECKED_CAST")
    override fun load(path: Path): Map<String, Any> {
        Files.newInputStream(path).use { return loadFromStream(it) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun loadFromStream(input: InputStream): Map<String, Any> {
        return yaml.load(input) ?: throw IOException("Invalid YAML structure")
    }
}
