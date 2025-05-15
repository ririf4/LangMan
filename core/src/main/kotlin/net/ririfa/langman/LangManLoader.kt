package net.ririfa.langman

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

object LangManLoader {

    private val logger = LoggerFactory.getLogger("LangManLoader")

    fun <T, E : IMessageProvider<C>, C : Any> loadInto(
        langMan: LangMan<E, C>,
        loader: FileLoader<T>,
        resourcePath: String,
        outputDir: Path,
        languages: List<String>,
        expectedMKType: Class<out MessageKey<E, C>>,
        fileExtension: Set<String>
    ) {
        val keyMap = flattenMessageKeys<E, C>(expectedMKType.kotlin)

        for (lang in languages) {
            val file: Path? = fileExtension
                .map { outputDir.resolve("$lang.$it") }
                .firstOrNull { Files.exists(it) }

            val resourceExt: String? = fileExtension
                .firstOrNull { LangManLoader::class.java.getResourceAsStream("$resourcePath/$lang.$it") != null }

            val data = when {
                file != null -> {
                    logger.info("Loading from file: $file")
                    loader.load(file)
                }

                resourceExt != null -> {
                    val stream = LangManLoader::class.java.getResourceAsStream("$resourcePath/$lang.$resourceExt")!!
                    logger.info("Loading from resource: $resourcePath/$lang.$resourceExt")
                    loader.loadFromStream(stream)
                }

                else -> {
                    logger.warn("No language file found for $lang (neither file nor resource)")
                    continue
                }
            }

            val flat = flattenMap(data as Map<String, Any>)
            val resolved = mutableMapOf<MessageKey<E, C>, String>()

            for ((k, v) in flat) {
                val key = keyMap[k]
                if (key is MessageKey<E, C>) {
                    resolved[key] = v
                } else {
                    logger.warn("Unmatched key: $k")
                }
            }

            langMan.messages[lang] = resolved
        }
    }

    fun flattenMap(map: Map<String, Any>, parent: String = ""): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for ((key, value) in map) {
            val fullKey = if (parent.isEmpty()) key else "$parent.$key"
            when (value) {
                is String -> result[fullKey.lowercase()] = value
                is Map<*, *> -> {
                    val subMap = value.filterKeys { it is String } as Map<String, Any>
                    result.putAll(flattenMap(subMap, fullKey))
                }

                is List<*> -> value.forEachIndexed { index, item ->
                    if (item is String) result["$fullKey.item${index + 1}".lowercase()] = item
                }
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : IMessageProvider<C>, C : Any> flattenMessageKeys(
        root: KClass<*>,
        prefix: String = ""
    ): Map<String, MessageKey<E, C>> {
        val result = mutableMapOf<String, MessageKey<E, C>>()
        fun scan(clazz: KClass<*>, path: String) {
            clazz.sealedSubclasses.forEach { sub ->
                val obj = runCatching { sub.objectInstance }.getOrNull()
                val fullPath = if (path.isEmpty()) sub.simpleName else "$path.${sub.simpleName}"
                if (obj is MessageKey<*, *>) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        result[fullPath!!.lowercase()] = obj as MessageKey<E, C>
                    } catch (e: ClassCastException) {
                        logger.warn("Failed to cast $fullPath to MessageKey: ${e.message}")
                    }
                } else {
                    scan(sub, fullPath!!.lowercase())
                }
            }
        }
        scan(root, prefix)
        return result
    }
}
