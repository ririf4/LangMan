package net.ririfa.langman

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ririfa.langman.dummy.DummyMessageKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.Locale
import kotlin.collections.get
import kotlin.reflect.KClass

/**
 * Manages language translations and message localization.
 * @param P The type of the message provider.
 * @param C The type of the text component.
 * @property textComponentFactory Factory function to create a text component.
 * @property expectedMKType Expected type of the message key for type safety.
 */
@Suppress("UNCHECKED_CAST", "unused")
open class LangMan<P : IMessageProvider<C>, C> private constructor(
	val textComponentFactory: (String) -> C,
	val expectedMKType: KClass<out MessageKey<*, *>>
) {
	/** Flag to enable debug mode. */
	var isDebug: Boolean = false

	/** The initialization type used. */
	var t: InitType? = null

	companion object {
		/** Logger instance for debugging and logging purposes. */
		val logger: Logger = LoggerFactory.getLogger("LangMan")

		@Suppress("CAST_NEVER_SUCCEEDS")
		private var instance: LangMan<*, *> =
			/**
			 * A dummy instance used to avoid null initialization issues.
			 */
			LangMan<IMessageProvider<Nothing>, Nothing>(
				{ it as Nothing },
				DummyMessageKey::class
			)

		/**
		 * Initializes a new instance of LangMan and sets it as the current instance.
		 * @param textComponentFactory Factory function to create message content.
		 * @param expectedType The expected message key type to ensure type safety.
		 * @param isDebug Whether debug mode should be enabled.
		 * @return The newly created LangMan instance.
		 */
		@JvmStatic
		@JvmOverloads
		fun <P : IMessageProvider<C>, C> createNew(
			textComponentFactory: (String) -> C,
			expectedType: KClass<out MessageKey<P, C>>,
			isDebug: Boolean = false
		): LangMan<P, C> {
			val languageManager: LangMan<P, C> = LangMan(textComponentFactory, expectedType)

			instance = languageManager
			languageManager.isDebug = isDebug

			return languageManager
		}

		/**
		 * Retrieves the current instance of LangMan.
		 * @return The current LangMan instance.
		 * @deprecated Use [getOrNull] instead.
		 */
		@Deprecated("Use getOrNull() instead", ReplaceWith("getOrNull()"))
		@JvmStatic
		fun <P : IMessageProvider<C>, C> get(): LangMan<P, C> {
			return instance as LangMan<P, C>
		}

		/**
		 * Retrieves the current instance of LangMan if initialized.
		 * @return The current LangMan instance, or null if not initialized.
		 */
		@JvmStatic
		fun <P : IMessageProvider<C>, C> getOrNull(): LangMan<P, C>? {
			return if (isInitialized()) instance as? LangMan<P, C> else null
		}

		/**
		 * Retrieves the LangMan instance without type safety.
		 * @return The current LangMan instance.
		 * @deprecated Use [getOrNull] instead.
		 */
		@Deprecated("Use getOrNull() instead", ReplaceWith("getOrNull()"))
		@JvmStatic
		fun getUnsafe(): LangMan<*, *> {
			return instance
		}

		/**
		 * Checks if LangMan has been initialized.
		 * @return True if initialized, false otherwise.
		 */
		@JvmStatic
		fun isInitialized(): Boolean {
			return instance.expectedMKType != DummyMessageKey::class
		}
	}

	/** Stores the localized messages for different languages. */
	val messages: MutableMap<String, MutableMap<MessageKey<P, C>, String>> = mutableMapOf()

	/**
	 * Initializes the language manager by loading translations from files.
	 * @param type The type of initialization (YAML or JSON).
	 * @param dir The directory containing language files.
	 * @param langs The list of language codes to load.
	 */
	fun init(type: InitType, dir: File, langs: List<String>) {
		logger.logIfDebug("Starting initialization with type: $type")

		langs.forEach { lang ->
			val file = File(dir, "$lang.${type.fileExtension}")
			if (!file.exists()) {
				logger.logIfDebug("File not found for language: $lang")
				return@forEach
			}

			val rawData: Map<String, Any> = when (type) {
				InitType.YAML -> Yaml().load(file.inputStream()) as? Map<String, Any> ?: emptyMap()
				InitType.JSON -> Gson().fromJson(file.reader(), object : TypeToken<Map<String, Any>>() {}.type)
					?: emptyMap()
			}

			val messagesForLang = registerMessages(expectedMKType, rawData)

			val mappedMessages = messagesForLang.mapNotNull { (key, value) ->
				val messageKey = findMessageKey(expectedMKType, key)
				messageKey?.let { it to value }
			}.toMap().toMutableMap()

			messages[lang] = mappedMessages
		}
	}

	private fun registerMessages(expectedMKType: KClass<*>, rawData: Map<String, Any>): MutableMap<String, String> {
		val messages = mutableMapOf<String, String>()

		val expectedKeys = collectMessageKeys(expectedMKType)
		val availableKeys = flattenMap(rawData)

		expectedKeys.forEach { key ->
			availableKeys[key]?.let { value ->
				messages[key] = value.toString()
			}
		}

		return messages
	}

	private fun collectMessageKeys(expectedMKType: KClass<*>, prefix: String = ""): List<String> {
		val keys = mutableListOf<String>()

		if (expectedMKType.simpleName == null) return keys

		val baseKey = if (prefix.isEmpty()) expectedMKType.simpleName!!.lowercase()
		else "$prefix.${expectedMKType.simpleName!!.lowercase()}"

		if (expectedMKType.objectInstance != null) {
			keys.add(baseKey)
		}

		expectedMKType.nestedClasses.forEach { subclass ->
			keys.addAll(collectMessageKeys(subclass, baseKey))
		}

		return keys
	}

	private fun findMessageKey(expectedMKType: KClass<*>, key: String): MessageKey<P, C>? {
		expectedMKType.nestedClasses.forEach { subclass ->
			val subclassKey = subclass.simpleName?.lowercase() ?: return@forEach

			if (subclassKey == key || key.matches(Regex("$subclassKey\\.item\\d+"))) {
				return subclass.objectInstance as? MessageKey<P, C>
			}

			val nestedKey = findMessageKey(subclass, key)
			if (nestedKey != null) {
				return nestedKey
			}
		}

		return null
	}

	private fun flattenMap(map: Map<String, Any>, prefix: String = ""): Map<String, Any> {
		val result = mutableMapOf<String, Any>()

		for ((key, value) in map) {
			val newKey = if (prefix.isEmpty()) key.lowercase() else "$prefix.${key.lowercase()}"

			when (value) {
				is Map<*, *> -> {
					result.putAll(flattenMap(value as Map<String, Any>, newKey))
				}
				is List<*> -> {
					value.forEachIndexed { index, item ->
						val itemKey = "$newKey.item${index + 1}".lowercase()
						result[itemKey] = item ?: "null"
					}
				}
				else -> result[newKey] = value
			}
		}

		return result
	}

	/**
	 * Retrieves a system message using the default locale.
	 * @param key The message key.
	 * @param args Arguments to format the message.
	 * @return The formatted system message.
	 */
	fun getSysMessage(key: MessageKey<*, *>, vararg args: Any): String {
		val lang = Locale.getDefault().language
		val message = messages[lang]?.get(key)
		return message?.let { String.format(it, *args) } ?: key.rc()
	}

	/**
	 * Retrieves a system message using a specific language code.
	 * @param key The message key.
	 * @param lang The language code.
	 * @param args Arguments to format the message.
	 * @return The formatted system message.
	 */
	fun getSysMessageByLangCode(key: MessageKey<*, *>, lang: String, vararg args: Any): String {
		val message = messages[lang]?.get(key)
		val text = message?.let { String.format(it, *args) } ?: return key.rc()
		return text
	}
}
