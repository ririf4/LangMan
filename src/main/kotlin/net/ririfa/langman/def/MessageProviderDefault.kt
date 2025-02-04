package net.ririfa.langman.def

import net.ririfa.langman.IMessageProvider
import net.ririfa.langman.LangMan
import net.ririfa.langman.MessageKey
import kotlin.reflect.full.isSubclassOf

// Based https://github.com/SwiftStorm-Studio/SwiftBase/blob/main/integrations/fabric/src/main/kotlin/net/rk4z/s1/swiftbase/fabric/FabricPlayer.kt
// Made by me!
abstract class MessageProviderDefault<P: MessageProviderDefault<P, C>, C> : IMessageProvider<C> {
	val langMan: LangMan<P, C>
		get() {
			if (!LangMan.isInitialized()) {
				throw IllegalStateException("LangMan is not initialized but you are trying to use it.")
			}
			val languageManager = LangMan.Companion.getOrNull<P, C>()
				?: throw IllegalStateException("LangMan is not initialized but you are trying to use it.")

			return languageManager
		}

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

	override fun getRawMessage(key: MessageKey<*, *>): String {
		val messages = langMan.messages
		val expectedMKType = langMan.expectedMKType

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		return messages[lang]?.get(key) ?: key.rc()
	}

	override fun hasMessage(key: MessageKey<*, *>): Boolean {
		val messages = langMan.messages
		val expectedMKType = langMan.expectedMKType

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		return messages[lang]?.containsKey(key) ?: false
	}
}