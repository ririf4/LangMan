package net.ririfa.langman

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
            return new<E, C>(C::class.java)
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
    private var isDebug: Boolean = false
    private var autoUpdate: Boolean = false
    private val langs: MutableList<String> = mutableListOf()

    fun withType(type: FileLoader<*, E, C>): LangManBuilder<E, C> {
        this.type = type
        return this
    }

    fun fromResource(resource: String): LangManBuilder<E, C> {
        this.resource = resource
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
        for (lang in langs) {
            for (ext in type.fileExtensions) {
                val resourcePath = "$resource/$lang.$ext"
                val outputPath = out.resolve("$lang.$ext")

                if (Files.exists(outputPath)) continue

                val stream = LangManLoader::class.java.getResourceAsStream(resourcePath)
                if (stream == null) {
                    // optional: debug log if needed
                    continue
                }

                Files.copy(stream, outputPath)
            }
        }
    }

}