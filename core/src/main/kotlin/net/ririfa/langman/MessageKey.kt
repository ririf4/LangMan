package net.ririfa.langman

import net.ririfa.langman.LangMan.Companion.logger

/**
 * Represents a key for a localized message.
 *
 * @param E The type of the message provider.
 * @param C The type of the text component.
 */
interface MessageKey<E : IMessageProvider<C>, C : Any> {
    /**
     * Retrieves the text component of this message key.
     *
     * @return The text component of this message key.
     */
    fun c(): C {
        val langMan = LangManContext.getByKeyClass(this::class.java)
            ?: error("No LangMan registered for ${this::class.java.name}")

        val factory = langMan.textFactory
        val clazz = factory.clazz

        require(clazz == this::class.java || clazz.isAssignableFrom(this::class.java)) {
            "TextFactory type mismatch: factory for $clazz but MessageKey is ${this::class.java}"
        }

        @Suppress("UNCHECKED_CAST")
        return (factory as TextFactory<C>).invoke(this::class.java.simpleName)
    }

    /**
     * Retrieves the raw content of this message key.
     *
     * @return The raw content of this message key.
     */
    fun rc(): String {
        return this.javaClass.simpleName
    }

    /**
     * Retrieves the full-path content of this message key.
     *
     * @return The full-path content of this message key.
     */
    fun fp(): String {
        val fullName = this.javaClass.name
        val packageName = this.javaClass.`package`?.name
        val relativeName = packageName?.let { fullName.removePrefix("$it.") } ?: fullName

        return relativeName.replace('$', '.').lowercase()
    }

    /**
     * Logs the message corresponding to this key.
     *
     * @param level The log level to use.
     */
    fun log(level: LogLevel = LogLevel.INFO) {
        val message = this.rc()
        when (level) {
            LogLevel.INFO -> logger.info(message)
            LogLevel.WARN -> logger.warn(message)
            LogLevel.ERROR -> logger.error(message)
        }
    }

    /**
     * Retrieves the message corresponding to this key using the given provider.
     *
     * @param provider The message provider to use.
     * @return The localized message content.
     */
    fun t(provider: E): C {
        return provider.getMessage(this)
    }
}