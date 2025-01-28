package net.ririfa.langman

import net.ririfa.langman.LangMan.Companion.logger

interface MessageKey<E : IMessageProvider<C>, C> {
	fun c(): C {
		return LangMan.get<E, C>().textComponentFactory(this.javaClass.simpleName)
	}

	fun rc(): String {
		return this.javaClass.simpleName
	}

	fun log(level: LogLevel = LogLevel.INFO) {
		val message = this.rc()
		when (level) {
			LogLevel.INFO -> logger.info(message)
			LogLevel.WARN -> logger.warn(message)
			LogLevel.ERROR -> logger.error(message)
		}
	}

	fun t(player: E): C {
		return player.getMessage(this)
	}
}