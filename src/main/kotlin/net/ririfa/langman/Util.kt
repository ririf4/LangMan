package net.ririfa.langman

import net.ririfa.langman.LangMan.Companion.logger
import org.slf4j.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


fun Logger.logIfDebug(message: String, level: LogLevel = LogLevel.INFO) {
	if (LangMan.getUnsafe().isDebug) {
		when (level) {
			LogLevel.INFO -> info(message)
			LogLevel.WARN -> warn(message)
			LogLevel.ERROR -> error(message)
		}
	} else {
		debug(message)
	}
}

enum class LogLevel {
	INFO, WARN, ERROR
}

fun <T : Any> KClass<T>.createInstanceOrNull(): T? {
	return try {
		this.createInstance()
	} catch (e: Exception) {
		logger.logIfDebug("Failed to create instance for class: ${this.simpleName}, reason: ${e.message}")
		null
	}
}