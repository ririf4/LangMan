package net.ririfa.langman

/**
 * Provides language-related functionalities.
 *
 * @param C The type of the message content (e.g., `String`, `TextComponent`).
 */
interface IMessageProvider<C> {
	/**
	 * Retrieves the language code associated with this provider.
	 *
	 * @return The language code as a string (e.g., "en", "ja").
	 */
	fun getLanguage(): String

	/**
	 * Retrieves a localized message based on the provided key and optional arguments.
	 *
	 * @param key The message key.
	 * @param args Optional arguments to format the message.
	 * @return The formatted localized message content.
	 */
	fun getMessage(key: MessageKey<*, *>, vararg args: Any): C

	/**
	 * Retrieves the raw message string for the provided key without formatting.
	 *
	 * @param key The message key.
	 * @return The raw message string.
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