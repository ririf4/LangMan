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
		logger.info("Starting initialization with type: $type")

		langs.forEach { lang ->
			val file = File(dir, "$lang.${type.fileExtension}")
			if (!file.exists()) {
				logger.warn("File not found for language: $lang")
				return@forEach
			}

			logger.info("Loading language file: ${file.absolutePath}")
			val rawData: Map<String, Any> = when (type) {
				InitType.YAML -> Yaml().load(file.inputStream()) as? Map<String, Any> ?: emptyMap()
				InitType.JSON -> Gson().fromJson(file.reader(), object : TypeToken<Map<String, Any>>() {}.type)
					?: emptyMap()
			}
			logger.logIfDebug("Raw data for $lang: $rawData")

			val flatData = flattenYamlMap(rawData)
			logger.logIfDebug("Flattened YAML data for $lang: $flatData")

			val messageKeys = flattenMessageKeys(expectedMKType, expectedMKType.simpleName!!.lowercase())
			logger.logIfDebug("Extracted message keys: $messageKeys")

			val localizedMessages = mutableMapOf<MessageKey<P, C>, String>()
			flatData.forEach { (key, value) ->
				messageKeys[key]?.let {
					localizedMessages[it as MessageKey<P, C>] = value
					logger.logIfDebug("Matched key: $key -> $value")
				} ?: logger.logIfDebug("No matching message key found for: $key", LogLevel.WARN)
			}

			messages[lang] = localizedMessages
			logger.logIfDebug("Loaded messages for $lang: ${messages[lang]}")
		}
	}

	/**
	 * Logs missing translation keys for all loaded languages.
	 */
	fun logMissingKeys(lang: String) {
		val expectedKeys = flattenMessageKeys(expectedMKType, expectedMKType.simpleName!!.lowercase()).keys
		val loadedKeys = messages[lang]?.keys?.map { it.rc() }?.toSet() ?: emptySet()

		val missingKeys = expectedKeys - loadedKeys
		if (missingKeys.isNotEmpty()) {
			logger.warn("Missing translation keys for language [$lang]: $missingKeys")
		} else {
			logger.info("All expected translation keys are present for language [$lang]")
		}
	}


	private fun flattenYamlMap(map: Map<*, *>, parentKey: String = ""): Map<String, String> {
		val result = mutableMapOf<String, String>()
		logger.logIfDebug("Flattening YAML map: parentKey=$parentKey, map=$map")

		for ((key, value) in map) {
			val keyStr = key?.toString() ?: continue
			val newKey = if (parentKey.isEmpty()) keyStr.lowercase() else "$parentKey.$keyStr".lowercase()

			when (value) {
				is Map<*, *> -> {
					val mapValue = value.filterKeys { it is String } as? Map<String, Any> ?: emptyMap()
					logger.logIfDebug("Processing nested map at key=$newKey: $mapValue")
					result.putAll(flattenYamlMap(mapValue, newKey))
				}
				is List<*> -> {
					value.forEachIndexed { index, item ->
						if (item is String) {
							val listKey = "$newKey.item${index + 1}"
							result[listKey] = item
							logger.logIfDebug("Processing list item at key=$listKey: $item")
						}
					}
				}
				is String -> {
					result[newKey] = value
					logger.logIfDebug("Processing string at key=$newKey: $value")
				}
			}
		}
		logger.logIfDebug("Flattened YAML result: $result")
		return result
	}

	private fun flattenMessageKeys(rootClass: KClass<*>, removePrefix: String = ""): Map<String, MessageKey<*, *>> {
		val result = mutableMapOf<String, MessageKey<*, *>>()

		logger.logIfDebug("Flattening message keys for class: ${rootClass.simpleName}")

		fun processClass(clazz: KClass<*>, path: String) {
			logger.logIfDebug("Processing class: ${clazz.simpleName}, path: $path")

			clazz.sealedSubclasses.forEach { subclass ->
				val obj = runCatching { subclass.objectInstance }.getOrNull()
				logger.logIfDebug("Processing subclass: ${subclass.simpleName}, value: $obj")

				if (obj is MessageKey<*, *>) {
					val newPath = if (path.isEmpty()) subclass.simpleName!!.lowercase() else "$path.${subclass.simpleName!!.lowercase()}"
					result[newPath.removePrefix("$removePrefix.")] = obj
					logger.logIfDebug("Added MessageKey: $newPath -> $obj")
				} else {
					processClass(subclass, if (path.isEmpty()) subclass.simpleName!!.lowercase() else "$path.${subclass.simpleName!!.lowercase()}")
				}
			}
		}

		processClass(rootClass, "")
		logger.logIfDebug("Flattened message keys result: $result")
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
