package net.ririfa.langman

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LangMan internal constructor(
    internal val isDebug: Boolean,
    internal val textFactory: TextFactory<*>,
) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(LangMan::class.java.simpleName)
    }
}