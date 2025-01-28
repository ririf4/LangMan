package net.ririfa.langman

/**
 * Provides language-related functionalities.
 *
 * @param C The type of the message content (e.g., `String`, `TextComponent`).
 */
interface IMessageProvider<C> {
	fun getLanguage(): String

	fun getMessage(key: MessageKey<*, *>, vararg args: Any): C

	fun getRawMessage(key: MessageKey<*, *>): String

	fun hasMessage(key: MessageKey<*, *>): Boolean
}