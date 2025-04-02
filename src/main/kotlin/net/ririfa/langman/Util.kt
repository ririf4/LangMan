package net.ririfa.langman

import org.slf4j.Logger


/**
 * Logs a message at the specified log level if the debug mode is enabled.
 * If the debug mode is not enabled, the message will be logged using the debug level.
 *
 * @param message The message to be logged.
 * @param level The log level at which the message should be logged. Defaults to [LogLevel.INFO].
 */
@JvmOverloads
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

/**
 * Represents the severity level of a log message.
 */
enum class LogLevel {
	/**
	 * Represents an informational message log level.
	 * Typically used to log general information about the application's progress or state.
	 */
	INFO, /**
	 * Represents a log level used to indicate potentially harmful situations.
	 * Typically used to log warnings that are notable but not necessarily an error.
	 */
	WARN, /**
	 * Represents a log level used to indicate error messages.
	 * This log level is typically used for severe issues that
	 * require immediate attention or might lead to application failure.
	 */
	ERROR
}