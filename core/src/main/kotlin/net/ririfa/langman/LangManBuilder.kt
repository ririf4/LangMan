package net.ririfa.langman

class LangManBuilder<C : Any> private constructor(
    private val actualC: Class<C>,
) {
    companion object {
        @JvmStatic
        fun <C : Any> new(actualC: Class<C>): LangManBuilder<C> {
            val builder = LangManBuilder<C>(actualC)
            return builder
        }
    }

    private lateinit var type: InitType
    private lateinit var resource: String
    private lateinit var file: String
    private lateinit var key: MessageKey<*, C>
    private lateinit var textFactory: TextFactory<*>
    private var isDebug: Boolean = false

    fun withType(type: InitType): LangManBuilder<C> {
        this.type = type
        return this
    }

    fun fromResource(resource: String): LangManBuilder<C> {
        this.resource = resource
        return this
    }

    fun toFile(file: String): LangManBuilder<C> {
        this.file = file
        return this
    }

    fun withMessageKey(key: MessageKey<*, C>): LangManBuilder<C> {
        this.key = key
        return this
    }

    fun debug(enabled: Boolean): LangManBuilder<C> {
        this.isDebug = enabled
        return this
    }

    fun <T : Any> registerTextFactory(factory: TextFactory<T>): LangManBuilder<C> {
        require(factory.clazz == actualC) {
            "TextFactory<T> must match LangManBuilder<C>'s actual type: expected $actualC but got ${factory.clazz}"
        }
        this.textFactory = factory
        return this
    }

    fun build(): LangMan {
        val langMan = LangMan(
            isDebug = isDebug,
            textFactory = textFactory,
        )
        if (actualC == String::class.java && !this::textFactory.isInitialized) {
            this.textFactory = object : TextFactory<String> {
                override val clazz = String::class.java
                override fun invoke(text: String): String {
                    return text
                }
            }
        }
        LangManContext.register(key::class.java, langMan)
        LangManContext.registerScoped(key::class.java, langMan)
        return langMan
    }
}