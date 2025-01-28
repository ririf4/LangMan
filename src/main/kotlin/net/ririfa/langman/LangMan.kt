package net.ririfa.langman

import net.ririfa.langman.dummy.DummyMessageKey
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST", "unused")
open class LangMan<P : IMessageProvider<C>, C> private constructor(
	val textComponentFactory: (String) -> C,
	val expectedMKType: KClass<out MessageKey<*, *>>
) {
	private var packName: String = ""

	var isDebug: Boolean = false
	var i: I? = null
	var t: InitType? = null

	companion object {
		val logger: Logger = LoggerFactory.getLogger("LangMan")

		@Suppress("CAST_NEVER_SUCCEEDS")
		private var instance: LangMan<*, *> =
			/**
			 * This is a dummy instance.
			 *
			 * To set the initial value to null, use ‘?’ must be added, but,
			 * although ‘?’ the IDE will point out the possibility of it being null, even if it has been successfully initialised.
			 *
			 * So I assign a dummy instance as the initial value.
			 */
			LangMan<IMessageProvider<Nothing>, Nothing>(
				{ it as Nothing },
				DummyMessageKey::class
			)

		/**
		 * Initialises the LangMan, create a new instance, and set it as the current instance.
		 * @param textComponentFactory The factory function to create the message content.
		 * @param expectedType The expected type of the message key (To keep the type safety).
		 * @return The newly created LangMan instance.
		 */
		@JvmStatic
		@JvmOverloads
		fun <P : IMessageProvider<C>, C> createNew(
			textComponentFactory: (String) -> C,
			expectedType: KClass<out MessageKey<P, C>>,
			packageName: String,
			isDebug: Boolean = false
		): LangMan<P, C> {
			val languageManager: LangMan<P, C> = LangMan(textComponentFactory, expectedType)

			instance = languageManager
			languageManager.packName = packageName
			languageManager.isDebug = isDebug

			return languageManager
		}

		/**
		 * Gets the current LangMan instance.
		 * @return The current LangMan instance.
		 */
		@Deprecated("Use getOrNull() instead", ReplaceWith("getOrNull()"))
		@JvmStatic
		fun <P : IMessageProvider<C>, C> get(): LangMan<P, C> {
			return instance as LangMan<P, C>
		}

		/**
		 * Gets the current LangMan instance if it is initialised.
		 * @return The current LangMan instance, if it is initialised, otherwise null.
		 */
		@JvmStatic
		fun <P : IMessageProvider<C>, C> getOrNull(): LangMan<P, C>? {
			return if (isInitialized()) instance as? LangMan<P, C> else null
		}

		@Deprecated("Use getOrNull() instead", ReplaceWith("getOrNull()"))
		@JvmStatic
		fun getUnsafe(): LangMan<*, *> {
			return instance
		}

		@JvmStatic
		fun isInitialized(): Boolean {
			return instance.expectedMKType != DummyMessageKey::class
		}
	}

	val messages: MutableMap<String, MutableMap<MessageKey<P, C>, String>> = mutableMapOf()

	fun findMissingKeys(lang: String) {
		logger.logIfDebug("Starting findMissingKeysForLanguage for language: $lang")

		val messageKeyMap: MutableMap<String, MessageKey<P, C>> = mutableMapOf()
		//scanForMessageKeys(messageKeyMap)

		val yamlData = messages[lang]
		if (yamlData == null) {
			logger.logIfDebug("No YAML data found for language: $lang")
			return
		}

		val yamlKeys: MutableSet<String> = mutableSetOf()
		//collectYamlKeysFromMessages(yamlData, yamlKeys)

		logger.logIfDebug("Keys in messageKeyMap: ${messageKeyMap.keys.joinToString(", ")}")
		logger.logIfDebug("Keys in YAML for language '$lang': ${yamlKeys.joinToString(", ")}")

		val missingKeys = messageKeyMap.keys.filter { it !in yamlKeys }

		if (missingKeys.isNotEmpty()) {
			logger.logIfDebug("Missing keys for language '$lang': ${missingKeys.joinToString(", ")}")
		} else {
			logger.logIfDebug("No missing keys found for language '$lang'. All class keys are present in YAML.")
		}
	}

	fun init(type: InitType, file: File, langs: List<String>) {
		logger.logIfDebug("Starting initialisation with type: $type")

		when (type) {
			InitType.YAML -> {
				i = YAML()
				val yaml = Yaml()
				val data = yaml.load(file.inputStream()) as Map<String, Any>
				langs.forEach { lang ->
					i?.startLoad(data, lang)
				}
			}
			InitType.JSON -> {
				i = JSON()

			}
			InitType.TOML -> {

			}
			InitType.PROPERTIES -> {

			}
		}
	}

	inner class YAML : I {
		override fun startLoad(
			data: Map<String, Any>,
			lang: String
		) {
			logger.logIfDebug("Starting to process YAML and map message keys for language: $lang")

			val messageKeyMap: MutableMap<String, MessageKey<P, C>> = mutableMapOf()
			val messageMap: MutableMap<MessageKey<P, C>, String> = mutableMapOf()

			scanForMessageKeys(messageKeyMap)
			logger.logIfDebug("MessageKey map generated with ${messageKeyMap.size} keys for language: $lang")

			processYamlData("", data, messageKeyMap, messageMap)
			logger.logIfDebug("YAML data processed for language: $lang with ${messageMap.size} entries")

			messages[lang] = messageMap
			logger.logIfDebug("Message map stored for language: $lang")
		}

		private fun scanForMessageKeys(
			messageKeyMap: MutableMap<String, MessageKey<P, C>>
		) {
			logger.logIfDebug("Starting scan for message keys of expected type: ${expectedMKType.simpleName}")

			val reflections = Reflections(
				ConfigurationBuilder()
					.setUrls(ClasspathHelper.forPackage(packName))
					.setScanners(Scanners.SubTypes)
			)

			val messageKeyClasses = reflections.getSubTypesOf(MessageKey::class.java)
			logger.logIfDebug("Found ${messageKeyClasses.size} potential MessageKey classes to examine")

			messageKeyClasses.forEach { clazz ->
				logger.logIfDebug("Examining class: ${clazz.kotlin.qualifiedName}")
				mapMessageKeys(clazz.kotlin, "", messageKeyMap)
			}

			logger.logIfDebug("Completed scanning for message keys.")
			logger.logIfDebug("Final MessageKey map contains: ${messageKeyMap.keys.joinToString(", ")}")
		}

		private fun mapMessageKeys(
			clazz: KClass<out MessageKey<*, *>>,
			currentPath: String = "",
			messageKeyMap: MutableMap<String, MessageKey<P, C>>
		) {
			val castedExpectedMKType = expectedMKType as KClass<out MessageKey<P, C>>

			val className = clazz.simpleName ?: return
			val fullPath = if (currentPath.isEmpty()) className else "$currentPath.$className"

			val normalizedKey = normalizeKey(fullPath)

			logger.logIfDebug("Mapping key for class: $className, normalized: $normalizedKey")

			val objectInstance = clazz.objectInstance ?: clazz.createInstanceOrNull()
			if (objectInstance != null && castedExpectedMKType.isInstance(objectInstance)) {
				if (!messageKeyMap.containsKey(normalizedKey)) {
					messageKeyMap[normalizedKey] = objectInstance as MessageKey<P, C>
					logger.logIfDebug("Registered key: $normalizedKey")
				}
			}

			clazz.nestedClasses.forEach { nestedClass ->
				if (nestedClass.isSubclassOf(castedExpectedMKType)) {
					mapMessageKeys(nestedClass as KClass<out MessageKey<P, C>>, fullPath, messageKeyMap)
				}
			}
		}

		private fun processYamlData(
			prefix: String,
			data: Map<String, Any>,
			messageKeyMap: Map<String, MessageKey<P, C>>,
			messageMap: MutableMap<MessageKey<P, C>, String>
		) {
			logger.logIfDebug("Starting YAML data processing with prefix: '$prefix'")
			logger.logIfDebug("Available keys in MessageKey map: ${messageKeyMap.keys.joinToString(", ")}")

			for ((key, value) in data) {
				val currentPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
				val normalizedPrefix = normalizeKey(currentPrefix)
				logger.logIfDebug("Processing key: $key, currentPrefix: $currentPrefix, normalized: $normalizedPrefix")

				if (key == "langVersion") {
					logger.logIfDebug("Skipping langVersion key")
					continue
				}

				when (value) {
					is String -> {
						val messageKey = messageKeyMap[normalizedPrefix]
						if (messageKey != null) {
							logger.logIfDebug("Mapping message: $normalizedPrefix -> $value")
							messageMap[messageKey] = value
						} else {
							logger.logIfDebug("No message key found for YAML path: $normalizedPrefix", LogLevel.WARN)
						}
					}

					is List<*> -> {
						logger.logIfDebug("Processing list at path: $normalizedPrefix with ${value.size} items")
						value.forEachIndexed { index, element ->
							val listPrefix = "$currentPrefix.item_$index"
							val normalizedListPrefix = normalizeKey(listPrefix)

							when (element) {
								is String -> {
									val messageKey = messageKeyMap[normalizedListPrefix]
									if (messageKey != null) {
										logger.logIfDebug("Mapping list item: $normalizedListPrefix -> $element")
										messageMap[messageKey] = element
									} else {
										logger.logIfDebug("No message key found for list item path: $normalizedListPrefix", LogLevel.WARN)
									}
								}
								is Map<*, *> -> {
									logger.logIfDebug("Encountered nested map in list at path: $normalizedListPrefix; diving deeper")
									processYamlData(listPrefix, element as Map<String, Any>, messageKeyMap, messageMap)
								}
								else -> {
									logger.logIfDebug("Unexpected value type in list at path $normalizedListPrefix: ${element?.let { it::class.simpleName } ?: "null"}")
								}
							}
						}
					}

					is Map<*, *> -> {
						logger.logIfDebug("Encountered nested structure at path: $normalizedPrefix; diving deeper")
						processYamlData(currentPrefix, value as Map<String, Any>, messageKeyMap, messageMap)
					}

					else -> {
						logger.logIfDebug("Unexpected value type at path $normalizedPrefix: ${value::class.simpleName}")
					}
				}
			}

			logger.logIfDebug("Completed YAML data processing for prefix: '$prefix'")
		}

		private fun collectYamlKeysFromMessages(
			messages: Map<MessageKey<P, C>, String>,
			yamlKeys: MutableSet<String>
		) {
			for (messageKey in messages.keys) {
				val normalizedKey = normalizeKey(messageKey.rc())
				yamlKeys.add(normalizedKey)
			}
		}
	}

	inner class JSON : I {
		override fun startLoad(data: Map<String, Any>, lang: String) {

		}
	}

	interface I {
		fun startLoad(data: Map<String, Any>, lang: String = "en")
	}

	private fun normalizeKey(key: String): String {
		return key.lowercase().replace("_", "")
	}
}