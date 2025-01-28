package net.ririfa.langman.exa

import net.ririfa.langman.IMessageProvider
import net.ririfa.langman.MessageKey

// キーは必ずobjectで定義し、名前をUpperCaseにする
open class A : MessageKey<B, String> {
	object KEY1 : A()

	open class Nested : A() {
		object KEY2 : Nested()

		// Arrayの要素名は、ITEM1, ITEM2のようにする。
		open class Array : Nested() {
			object ITEM1 : Array()
			object ITEM2 : Array()
		}
	}
}

class B() : IMessageProvider<String> {
	override fun getLanguage(): String {
		return "en"
	}

	override fun getMessage(key: MessageKey<*, *>, vararg args: Any): String {
		return "placeholder"
	}

	override fun getRawMessage(key: MessageKey<*, *>): String {
		return "placeholder"
	}

	override fun hasMessage(key: MessageKey<*, *>): Boolean {
		return true
	}
}