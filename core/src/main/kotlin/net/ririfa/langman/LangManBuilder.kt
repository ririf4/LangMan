package net.ririfa.langman

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class LangManBuilder<E : IMessageProvider<C>, C : Any> private constructor(
    private val actualC: Class<C>,
) {
    companion object {
        @JvmStatic
        fun <E : IMessageProvider<C>, C : Any> new(actualC: Class<C>): LangManBuilder<E, C> {
            val builder = LangManBuilder<E, C>(actualC)
            return builder
        }

        inline fun <E : IMessageProvider<C>, reified C : Any> new(): LangManBuilder<E, C> {
            return new(C::class.java)
        }

        inline fun <E : IMessageProvider<C>, reified C : Any> new(
            block: LangManBuilder<E, C>.() -> Unit
        ): LangManBuilder<E, C> {
            return new<E, C>().apply(block)
        }
    }

    private lateinit var type: FileLoader<*, E, C>
    private lateinit var resource: String
    private lateinit var out: Path
    private lateinit var key: Class<out MessageKey<E, C>>
    private lateinit var textFactory: TextFactory<C>
    private var parentClass: Class<*>? = null
    private var isDebug: Boolean = false
    private var autoUpdate: Boolean = false
    private val langs: MutableList<String> = mutableListOf()

    fun withType(type: FileLoader<*, E, C>): LangManBuilder<E, C> {
        this.type = type
        return this
    }

    fun fromResource(resource: String): LangManBuilder<E, C> {
        this.resource = if (resource.startsWith("/")) resource else "/$resource"
        return this
    }

    fun toPath(out: Path): LangManBuilder<E, C> {
        this.out = out
        return this
    }

    fun withMessageKey(key: Class<out MessageKey<E, C>>): LangManBuilder<E, C> {
        this.key = key
        return this
    }

    fun debug(enabled: Boolean): LangManBuilder<E, C> {
        this.isDebug = enabled
        return this
    }

    fun withLanguage(langs: List<String>): LangManBuilder<E, C> {
        this.langs.clear()
        this.langs.addAll(langs)
        return this
    }

    fun fromClass(clazz: Class<*>): LangManBuilder<E, C> {
        this.parentClass = clazz
        return this
    }

    fun registerTextFactory(factory: TextFactory<C>): LangManBuilder<E, C> {
        this.textFactory = factory
        return this
    }

    fun autoUpdateIfNeeded(enabled: Boolean): LangManBuilder<E, C> {
        this.autoUpdate = enabled
        return this
    }

    fun build(): LangMan<E, C> {
        if (actualC == String::class.java && !this::textFactory.isInitialized) {
            @Suppress("UNCHECKED_CAST")
            this.textFactory = defaultStringFactory as TextFactory<C>
        }

        if (parentClass == null) {
            throw IllegalStateException("`parentClass` must be set.")
        }

        extractMissingLanguageFiles()

        val langMan = LangMan(
            isDebug = isDebug,
            textFactory = textFactory,
            expectedMKType = key
        )

        LangManLoader.loadInto(
            langMan,
            type,
            resource,
            out,
            langs,
            key,
            type.fileExtensions
        )

        LangManContext.register(langMan)

        if (autoUpdate) {
            FileAutoUpdater.updateIfNeeded(
                resource,
                out,
                langs,
                type.fileExtensions,
                type
            )
        }

        return langMan
    }

    private val defaultStringFactory = object : TextFactory<String> {
        override val clazz = String::class.java
        override fun invoke(text: String): String = text
    }

    private fun extractMissingLanguageFiles() {
        Files.createDirectories(out)

        for (lang in langs) {
            val found: MutableList<Pair<String, InputStream>> = mutableListOf()

            for (ext in type.fileExtensions) {
                val path = "${resource.trimEnd('/')}/$lang.$ext"
                val stream = parentClass?.getResourceAsStream(path)
                if (stream != null) {
                    found.add(path to stream)
                }
            }

            when (found.size) {
                0 -> {
                    if (isDebug) {
                        println("[LangMan] Missing resource: ${resource.trimEnd('/')}/$lang.[${type.fileExtensions.joinToString()}]")
                    }
                }

                1 -> {
                    val (path, stream) = found.first()
                    val outputPath = out.resolve(path.substringAfterLast('/'))
                    Files.createDirectories(outputPath.parent)
                    if (Files.notExists(outputPath)) {
                        stream.use {
                            Files.copy(it, outputPath)
                        }
                    }
                }

                else -> {
                    found.forEach { it.second.close() }
                    val paths = found.joinToString { it.first }
                    error("Multiple language files found for '$lang': $paths. Only one extension is allowed per language.")
                }
            }
        }
    }

}