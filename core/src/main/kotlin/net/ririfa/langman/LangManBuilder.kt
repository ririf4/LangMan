package net.ririfa.langman

class LangManBuilder<E : IMessageProvider<C>, C : Any> private constructor(
    private val actualC: Class<C>,
) {
    companion object {
        @JvmStatic
        fun <E : IMessageProvider<C>, C : Any> new(actualC: Class<C>): LangManBuilder<E, C> {
            val builder = LangManBuilder<E, C>(actualC)
            return builder
        }
    }

    private lateinit var type: InitType
    private lateinit var resource: String
    private lateinit var file: String
    private lateinit var key: MessageKey<E, C>
    private lateinit var textFactory: TextFactory<C>
    private var isDebug: Boolean = false
    private var scope: LangManScope = LangManScope.KEY_CLASS
    private var customKey: Any? = null

    fun withType(type: InitType): LangManBuilder<E, C> {
        this.type = type
        return this
    }

    fun fromResource(resource: String): LangManBuilder<E, C> {
        this.resource = resource
        return this
    }

    fun toFile(file: String): LangManBuilder<E, C> {
        this.file = file
        return this
    }

    fun withMessageKey(key: MessageKey<E, C>): LangManBuilder<E, C> {
        this.key = key
        return this
    }

    fun debug(enabled: Boolean): LangManBuilder<E, C> {
        this.isDebug = enabled
        return this
    }

    fun registerTextFactory(factory: TextFactory<C>): LangManBuilder<E, C> {
        this.textFactory = factory
        return this
    }

    fun register(scope: LangManScope, key: Any? = null): LangManBuilder<E, C> {
        this.scope = scope
        this.customKey = key
        return this
    }

    fun build(): LangMan<E, C> {
        if (actualC == String::class.java && !this::textFactory.isInitialized) {
            @Suppress("UNCHECKED_CAST")
            this.textFactory = defaultStringFactory as TextFactory<C>
        }

        val langMan = LangMan(
            isDebug = isDebug,
            textFactory = textFactory,
            expectedMKType = key::class.java
        )

        LangManContext.register(
            langMan = langMan,
            scope = scope,
            key = when (scope) {
                LangManScope.KEY_CLASS -> key::class.java
                LangManScope.CUSTOM -> customKey
                LangManScope.CALLER_CONTEXT -> null
            }
        )

        return langMan
    }

    private val defaultStringFactory = object : TextFactory<String> {
        override val clazz = String::class.java
        override fun invoke(text: String): String = text
    }
}