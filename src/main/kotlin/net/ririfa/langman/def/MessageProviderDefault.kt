@file:Suppress("DuplicatedCode")

package net.ririfa.langman.def

import net.ririfa.langman.IMessageProvider
import net.ririfa.langman.LangMan
import net.ririfa.langman.MessageKey
import kotlin.reflect.full.isSubclassOf

// Based https://github.com/SwiftStorm-Studio/SwiftBase/blob/main/integrations/fabric/src/main/kotlin/net/rk4z/s1/swiftbase/fabric/FabricPlayer.kt
// Made by me!
abstract class MessageProviderDefault<P: MessageProviderDefault<P, C>, C> : IMessageProvider<C> {
	/**
	 * Retrieves the `LangMan` instance.
	 *
	 * @throws IllegalStateException if `LangMan` is not initialized.
	 * @return The active `LangMan` instance.
	 */
	val langMan: LangMan<P, C>
		get() {
			if (!LangMan.isInitialized()) {
				throw IllegalStateException("LangMan is not initialized but you are trying to use it.")
			}
			val languageManager = LangMan.Companion.getOrNull<P, C>()
				?: throw IllegalStateException("LangMan is not initialized but you are trying to use it.")

			return languageManager
		}

	/**
	 * Retrieves a formatted localized message using sequential arguments.
	 *
	 * @param key The message key.
	 * @param args Optional arguments used to format the message.
	 * @return The formatted localized message content.
	 */
	override fun getMessage(key: MessageKey<*, *>, vararg args: Any): C {
		val messages = langMan.messages
		val expectedMKType = langMan.expectedMKType
		val textComponentFactory = langMan.textComponentFactory

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		val message = messages[lang]?.get(key)

		val text = message?.let { String.format(it, *args) } ?: key.rc()
		return textComponentFactory(text)
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
		val textComponentFactory = langMan.textComponentFactory

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		var message = messages[lang]?.get(key) ?: key.rc()

		// Replace placeholders using the provided arguments
		for ((placeholder, value) in argsComplete) {
			message = message.replace("%$placeholder%", value.toString())
		}

		return textComponentFactory(message)
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
	 * Retrieves a localized message based on the specified [key], applies placeholder replacement using [argsComplete],
	 * and converts the processed message into the output type [C].
	 *
	 * This method uses the registered [LangMan.replaceLogic] to perform placeholder replacements on the raw message. After all
	 * placeholders have been replaced, it applies the corresponding converter from [LangMan.convertToFinalType] to transform
	 * the message into the final type [C].
	 *
	 * @param K The placeholder key type.
	 * @param V The placeholder value type.
	 * @param C The reified output type.
	 * @param key The [MessageKey] identifying which localized message to retrieve.
	 * @param argsComplete A map of placeholders (keys) and their respective replacement values.
	 * @return The localized message of type [C].
	 * @throws IllegalStateException if the retrieved message key's type does not match the expected message key type.
	 * @throws IllegalArgumentException if no replacer is found for the retrieved message's class or no converter is found for [C].
	 */
	@Suppress("UNCHECKED_CAST")
	inline fun <K, V, reified C> getMessage(key: MessageKey<*, *>, argsComplete: Map<K, V>): C {
		val messages = langMan.messages
		val expectedMKType = langMan.expectedMKType

		require(key::class.isSubclassOf(expectedMKType)) {
			"Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType"
		}
		val lang = this.getLanguage()
		var message: Any = messages[lang]?.get(key) ?: key.rc()

		val replacer = langMan.replaceLogic[message::class.java]
			?: error("No replacer found for ${message::class.java}")

		for ((placeholder, value) in argsComplete) {
			message = replacer(message, "%$placeholder%", value as Any)
		}

		val converter = langMan.convertToFinalType[C::class.java] as? (Any) -> C
			?: error("No converter found for ${message::class.java} -> ${C::class.java}")

		return converter(message)
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

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
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

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		return messages[lang]?.containsKey(key) == true
	}
}