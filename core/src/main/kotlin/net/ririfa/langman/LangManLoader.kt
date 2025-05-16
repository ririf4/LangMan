package net.ririfa.langman

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

object LangManLoader {

    private val logger = LoggerFactory.getLogger("LangManLoader")

    fun <T, E : IMessageProvider<C>, C : Any> loadInto(
        langMan: LangMan<E, C>,
        loader: FileLoader<T, E, C>,
        resourcePath: String,
        outputDir: Path,
        languages: List<String>,
        expectedMKType: Class<out MessageKey<E, C>>,
        fileExtensions: Set<String> = loader.fileExtensions
    ) {
        val keyMap = flattenMessageKeys<E, C>(expectedMKType.kotlin)

        for (lang in languages) {
            val file: Path? = fileExtensions
                .map { outputDir.resolve("$lang.$it") }
                .firstOrNull { Files.exists(it) }

            val resourceExt: String? = fileExtensions
                .firstOrNull {
                    LangManLoader::class.java.getResourceAsStream("$resourcePath/$lang.$it") != null
                }

            val parsed: T = when {
                file != null -> {
                    logger.info("Loading from file: $file")
                    loader.parse(file)
                }

                resourceExt != null -> {
                    val stream = LangManLoader::class.java
                        .getResourceAsStream("$resourcePath/$lang.$resourceExt")!!
                    logger.info("Loading from resource: $resourcePath/$lang.$resourceExt")
                    loader.parse(stream)
                }

                else -> {
                    logger.warn("No language file found for $lang (neither file nor resource)")
                    continue
                }
            }

            val resolved = loader.resolve(parsed, keyMap)

            langMan.messages[lang] = resolved
        }
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
