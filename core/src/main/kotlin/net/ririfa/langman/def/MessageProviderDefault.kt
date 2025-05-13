@file:Suppress("DuplicatedCode")

package net.ririfa.langman.def

import net.ririfa.langman.*
import kotlin.reflect.full.isSubclassOf

/**
 * An abstract base class implementing the `IMessageProvider` interface for creating localized message systems.
 *
 * This class is intended to handle the localization of messages grouped by language settings
 * and provides multiple strategies for retrieving context-aware localized messages, including:
 * - Messages with sequential arguments.
 * - Messages using placeholder-based maps.
 * - Messages generated based on intermediate types and custom transformations.
 *
 * The output type of localized messages is determined by the generic type parameter `C`.
 *
 * @param E Self-referenced generic type used for type-safe association within implementations.
 * @param C The type of the final localized message output (e.g., string, text component).
 * @property clazz The class type of the final output used during message conversion.
 */
// Based https://github.com/SwiftStorm-Studio/SwiftBase/blob/main/integrations/fabric/src/main/kotlin/net/rk4z/s1/swiftbase/fabric/FabricPlayer.kt
// Made by me!
abstract class MessageProviderDefault<E : MessageProviderDefault<E, C>, C : Any>(
    val clazz: Class<C>,
    private val scope: LangManScope = LangManScope.CALLER_CONTEXT,
    private val customKey: Any? = null
) : IMessageProvider<C> {
    /**
     * Retrieves the `LangMan` instance.
     *
     * @throws IllegalStateException if `LangMan` is not initialized.
     * @return The active `LangMan` instance.
     */
    val langMan: LangMan<E, C>
        get() = LangManContext.getBy<E, C>(scope, customKey)
            ?: throw IllegalStateException("LangMan instance not found for scope=$scope key=$customKey")

    /**
     * Retrieves a formatted localized message using sequential arguments.
     *
     * @param key The message key.
     * @param args Optional arguments used to format the message.
     * @return The formatted localized message content.
     */
    @Deprecated(
        "Use getMessage(key, Map) or getMessage(key, T, transform) instead",
        replaceWith = ReplaceWith("getMessage(key, mapOf(...))"),
        level = DeprecationLevel.WARNING
    )
    override fun getMessage(key: MessageKey<*, *>, vararg args: Any): C {
        val messages = langMan.messages
        val expectedMKType = langMan.expectedMKType
        val textFactory = langMan.textFactory

        require(key::class.isSubclassOf(expectedMKType.kotlin)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        val lang = this.getLanguage()
        val message = messages[lang]?.get(key)

        val text = message?.let { String.format(it, *args) } ?: key.rc()
        return textFactory.invoke(text)
    }

    /**
     * Retrieves a formatted localized message using a map of named arguments.
     *
     * @param key The message key.
     * @param argsComplete A map of placeholders and their corresponding values.
     * @return The formatted localized message content.
     */
    override fun <K, V> getMessage(key: MessageKey<*, *>, argsComplete: Map<K, V>): C {
        val messages = langMan.messages
        val expectedMKType = langMan.expectedMKType
        val textFactory = langMan.textFactory

        require(key::class.isSubclassOf(expectedMKType.kotlin)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        val lang = this.getLanguage()
        var message = messages[lang]?.get(key) ?: key.rc()

        // Replace placeholders using the provided arguments
        for ((placeholder, value) in argsComplete) {
            message = message.replace("%$placeholder%", value.toString())
        }

        return textFactory.invoke(message)
    }

    /**
     * Retrieves a formatted localized message using an intermediate data type.
     *
     * The given intermediate object is transformed into a placeholder map,
     * which is then used for message formatting.
     *
     * @param key The message key.
     * @param intermediate An object containing data relevant to the message.
     * @param transform A function to convert the intermediate object into a map of placeholders.
     * @return The formatted localized message content.
     */
    override fun <T, K, V> getMessage(key: MessageKey<*, *>, intermediate: T, transform: (T) -> Map<K, V>): C {
        val argsComplete = transform(intermediate)
        return getMessage(key, argsComplete)
    }

    /**
     * Retrieves a localized message of type [C] based on the specified [key], applies placeholder replacement using [argsComplete],
     * and converts the processed message into the requested type.
     *
     * This method first retrieves the raw message from `LangMan`, then applies placeholder replacements using the registered
     * `replaceLogic`. After the placeholders are replaced, the message is converted into the final type [C] using the registered
     * `convertToFinalType`.
     *
     * @param C The reified output type.
     * @param key The [MessageKey] identifying which localized message to retrieve.
     * @param argsComplete A map of placeholders (keys) and their respective replacement values.
     * @return The localized message of type [C].
     * @throws IllegalStateException if the retrieved message key's type does not match the expected message key type.
     * @throws IllegalStateException if no converter is found for type [C].
     * @throws IllegalStateException if no replacement logic is found for type [C].
     * @throws IllegalArgumentException if message conversion fails due to a type mismatch.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    inline fun <reified C : Any> getMsgWithOther(
        key: MessageKey<*, *>,
        argsComplete: Map<String, C> = emptyMap()
    ): C {
        val messages = langMan.messages as Map<String, Map<MessageKey<E, C>, String>>
        val expectedMKType = langMan.expectedMKType

        require(key::class.isSubclassOf(expectedMKType.kotlin)) {
            "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType"
        }

        val lang = this.getLanguage()
        val message = messages[lang]?.get(key) ?: key.rc()

        val converter = langMan.convertToFinalType[C::class.java]
            ?: throw IllegalStateException("No converter found for type ${C::class}")

        var convertedMessage: C = try {
            converter.invoke(message) as? C
                ?: throw IllegalArgumentException("Failed to convert message: Expected ${C::class}, but got ${message::class}")
        } catch (e: ClassCastException) {
            throw IllegalArgumentException("Failed to convert message: Expected ${C::class}, but got ${message::class}", e)
        }

        if (argsComplete.isNotEmpty()) {
            val replaceFunction = langMan.replaceLogic[C::class.java]
                ?: throw IllegalStateException("No replacement logic found for type ${C::class}")

            for ((key, value) in argsComplete) {
                convertedMessage = (replaceFunction as (C, String, C) -> C)(convertedMessage, key, value)
            }
        }

        return convertedMessage
    }

    /**
     * Retrieves a localized message of type [G], applies placeholder replacement, and then converts it into the default class type [C].
     *
     * This method first retrieves a localized message of type [G] using [getMsgWithOther], applies the necessary placeholder replacements,
     * and then converts it into the class-level default type [C] using the registered converter.
     *
     * @param G The intermediate type used for processing before conversion to [C].
     * @param key The [MessageKey] identifying which localized message to retrieve.
     * @param argsComplete A map of placeholders (keys) and their respective replacement values.
     * @return The localized message converted into the class-defined type [C].
     * @throws IllegalStateException if no converter is found for type [C].
     * @throws IllegalArgumentException if message conversion fails due to a type mismatch.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    inline fun <reified G : Any> getMsg(
        key: MessageKey<E, C>,
        argsComplete: Map<String, G> = emptyMap()
    ): C {
        val converter = langMan.convertToFinalType[clazz]
            ?: throw IllegalStateException("No converter found for type $clazz")

        val messageG: G = getMsgWithOther(key, argsComplete)

        return try {
            converter.invoke(messageG) as? C
                ?: throw IllegalArgumentException("Failed to convert message: Expected $clazz, but got ${messageG::class}")
        } catch (e: ClassCastException) {
            throw IllegalArgumentException("Failed to convert message: Expected $clazz, but got ${messageG::class}", e)
        }
    }

    /**
     * Retrieves the raw message string for the provided key without formatting.
     *
     * @param key The message key.
     * @return The unformatted message string.
     */
    override fun getRawMessage(key: MessageKey<*, *>): String {
        val messages = langMan.messages
        val expectedMKType = langMan.expectedMKType

        require(key::class.isSubclassOf(expectedMKType.kotlin)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        val lang = this.getLanguage()
        return messages[lang]?.get(key) ?: key.rc()
    }

    /**
     * Checks if a localized message exists for the given key.
     *
     * @param key The message key.
     * @return `true` if a message exists, `false` otherwise.
     */
    override fun hasMessage(key: MessageKey<*, *>): Boolean {
        val messages = langMan.messages
        val expectedMKType = langMan.expectedMKType

        require(key::class.isSubclassOf(expectedMKType.kotlin)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        val lang = this.getLanguage()
        return messages[lang]?.containsKey(key) == true
    }
}