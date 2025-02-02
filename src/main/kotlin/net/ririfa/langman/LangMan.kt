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

			val data = toLowercaseKeys(rawData) as Map<String, Any>
			startLoad(data, lang)
		}
	}

	private fun startLoad(data: Map<String, Any>, lang: String) {
		logger.logIfDebug("Starting load for language: $lang")

		val langData = messages.computeIfAbsent(lang) { mutableMapOf() }

		val messageKeys = getMessageKeys()

		messageKeys.forEach { key ->
			val keyName = convertToKeyName(key::class)
			val value = getNestedValue(data, keyName)

			logger.logIfDebug("Checking key: $keyName -> Value: $value")

			if (value != null) {
				langData[key] = value.toString()
				logger.logIfDebug("Mapped key: $keyName -> $value")
			} else {
				logger.logIfDebug("Key not found in YAML: $keyName")
			}
		}
	}

	private fun convertToKeyName(clazz: KClass<*>): String {
		val rawKey = clazz.qualifiedName!!
			.removePrefix("${expectedMKType.qualifiedName}.")
			.replace('$', '.')
			.lowercase()

		return rawKey.replace(Regex("item(\\d+)"), "$1")
	}

	private fun getNestedValue(map: Map<String, Any>, keyPath: String): Any? {
		val keys = keyPath.split(".")
		var current: Any? = map

		for (key in keys) {
			current = when (current) {
				is Map<*, *> -> {
					current[key.lowercase()]
				}
				is List<*> -> {
					if (key.matches(Regex("\\d+"))) {
						val index = key.toInt() - 1
						if (index in current.indices) {
							current[index]
						} else {
							return null
						}
					} else {
						current.mapIndexed { index, value -> (index + 1).toString() to value }
							.toMap()[key.lowercase()]
					}
				}
				else -> return null
			}
		}
		return current
	}

	private fun getMessageKeys(): List<MessageKey<P, C>> {
		val result = mutableListOf<MessageKey<P, C>>()

		fun collectKeys(kClass: KClass<*>) {
			kClass.objectInstance?.let {
				if (it is MessageKey<*, *>) {
					result.add(it as MessageKey<P, C>)
				}
			}

			kClass.nestedClasses.forEach { nested ->
				collectKeys(nested)
			}
		}

		collectKeys(expectedMKType)

		val keyMap = result.associateBy { it.rc().lowercase() }

		return keyMap.values.toList()
	}

	private fun toLowercaseKeys(input: Any?): Any? {
		return when (input) {
			is Map<*, *> -> input.entries.associate { (key, value) ->
				key.toString().lowercase() to toLowercaseKeys(value)
			}
			is List<*> -> input.map { toLowercaseKeys(it) }
			else -> input
		}
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
