package net.ririfa.langman

/**
 * Interface for a message provider that handles localized messages.
 *
 * @param C The type of the message content (e.g., `String`, `Component`).
 */
interface IMessageProvider<C> {
	/**
	 * Retrieves the language code associated with this provider.
	 *
	 * @return The language code as a string (e.g., `"en"`, `"ja"`).
	 */
	fun getLanguage(): String

	/**
	 * Retrieves a formatted localized message based on the provided key and optional arguments.
	 *
	 * The formatting follows a sequential argument replacement mechanism (e.g., `%s, %s`).
	 *
	 * @param key The message key.
	 * @param args Optional arguments used to format the message.
	 * @return The formatted localized message content.
	 */
	fun getMessage(key: MessageKey<*, *>, vararg args: Any): C

	/**
	 * Retrieves a formatted localized message using a map of named arguments.
	 *
	 * This method allows for explicit placeholder replacement (e.g., `%player%`, `%id%`).
	 *
	 * @param key The message key.
	 * @param argsComplete A map of placeholders and their corresponding values.
	 * @return The formatted localized message content.
	 */
	fun <K, V> getMessage(key: MessageKey<*, *>, argsComplete: Map<K, V>): C

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
	fun <T, K, V> getMessage(key: MessageKey<*, *>, intermediate: T, transform: (T) -> Map<K, V>): C

	/**
	 * Retrieves the raw message string for the provided key without formatting.
	 *
	 * @param key The message key.
	 * @return The unformatted message string.
	 */
	fun getRawMessage(key: MessageKey<*, *>): String

	/**
	 * Checks if a localized message exists for the given key.
	 *
	 * @param key The message key.
	 * @return `true` if a message exists, `false` otherwise.
	 */
	fun hasMessage(key: MessageKey<*, *>): Boolean
}
