package net.ririfa.langman.ext.yaml

import net.ririfa.langman.FileLoader
import net.ririfa.langman.IMessageProvider
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class YamlFileLoader<E : IMessageProvider<C>, C : Any>(
    private val yamlParser: (InputStream) -> Any
) : FileLoader<Map<String, Any>, E, C> {

    override val fileExtensions = setOf("yml", "yaml")

    override fun parse(path: Path): Map<String, Any> =
        Files.newInputStream(path).use(::parse)

    override fun parse(stream: InputStream): Map<String, Any> {
        val result = yamlParser(stream)
        return result as? Map<String, Any>
            ?: error("Invalid YAML format: Top-level must be a Map<String, Any>")
    }

    override fun flatten(data: Map<String, Any>, parent: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for ((key, value) in data) {
            val fullKey = if (parent.isEmpty()) key else "$parent.$key"
            when (value) {
                is String -> result[fullKey.lowercase()] = value
                is Map<*, *> -> {
                    val subMap = value.entries
                        .filter { it.key is String && it.value != null }
                        .associate { it.key as String to it.value as Any }
                    result.putAll(flatten(subMap, fullKey))
                }

                is List<*> -> value.forEachIndexed { index, item ->
                    if (item is String) {
                        result["$fullKey.item${index + 1}".lowercase()] = item
                    }
                }
            }
        }
        return result
    }
}
