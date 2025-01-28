@file:Suppress("DuplicatedCode")

package net.ririfa.langman

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import net.ririfa.langman.dummy.DummyMessageKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

@Suppress("UNCHECKED_CAST", "unused")
open class LangMan<P : IMessageProvider<C>, C> private constructor(
	val textComponentFactory: (String) -> C,
	val expectedMKType: KClass<out MessageKey<*, *>>
) {
	private var packName: String = ""

	var isDebug: Boolean = false
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

	fun init(type: InitType, dir: File, langs: List<String>) {
		logger.logIfDebug("Starting initialisation with type: $type")

		val scanResult = ClassGraph()
			.enableClassInfo()
			.acceptPackages(packName)
			.scan()

		val subClasses = scanResult.getSubclasses(MessageKey::class.java.name)

		langs.forEach { lang ->
			val file = File(dir, "$lang.${type.fileExtension}")
			if (!file.exists()) {
				logger.logIfDebug("File not found for language: $lang")
				return@forEach
			}

			val data = when (type) {
				InitType.YAML -> {
					val yaml = Yaml()
					file.inputStream().use { yaml.load<Map<String, Any>>(it) ?: emptyMap() }
				}
				InitType.JSON -> {
					val gson = Gson()
					val typeToken = object : TypeToken<Map<String, Any>>() {}.type
					gson.fromJson<Map<String, Any>>(file.reader(), typeToken) ?: emptyMap()
				}
			}

			startLoad(data, subClasses, lang)
		}
	}

	fun startLoad(data: Map<String, Any>, messageKeyClass: ClassInfoList, lang: String) {
		logger.logIfDebug("Starting load for language: $lang")

		val langData = messages.computeIfAbsent(lang) { mutableMapOf() }

		messageKeyClass.forEach { classInfo ->
			val clazz = Class.forName(classInfo.name).kotlin
			if (!expectedMKType.isSuperclassOf(clazz)) return@forEach

			val instance = clazz.objectInstance as? MessageKey<P, C> ?: return@forEach

			val keyName = convertToKeyName(clazz)

			val value = getNestedValue(data, keyName)
			if (value != null) {
				langData[instance] = value.toString()
			}
		}
	}


	fun convertToKeyName(clazz: KClass<*>): String {
		return clazz.qualifiedName!!
			.removePrefix("${expectedMKType.qualifiedName}.")
			.replace('$', '.')
			.uppercase()
	}

	fun getNestedValue(map: Map<String, Any>, keyPath: String): Any? {
		val keys = keyPath.split(".")
		var current: Any? = map

		for (key in keys) {
			current = when (current) {
				is Map<*, *> -> {
					current[key]
				}

				is List<*> -> {
					val index = key.removePrefix("ITEM").toIntOrNull()
					if (index != null && index in current.indices) {
						current[index]
					} else {
						return null
					}
				}

				else -> return null
			}
		}
		return current
	}
}