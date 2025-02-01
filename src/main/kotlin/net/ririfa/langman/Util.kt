package net.ririfa.langman

import org.slf4j.Logger


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

class UnexpectedClassTypeException(message: String) : RuntimeException(message)