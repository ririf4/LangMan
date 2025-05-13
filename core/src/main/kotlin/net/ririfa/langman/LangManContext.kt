package net.ririfa.langman

import java.util.concurrent.ConcurrentHashMap

enum class LangManScope {
    KEY_CLASS,
    CALLER_CONTEXT,
    CUSTOM
}

internal object LangManContext {
    private val instances = ConcurrentHashMap<Any, LangMan<*, *>>()

    fun <E : IMessageProvider<C>, C : Any> register(
        langMan: LangMan<E, C>,
        scope: LangManScope = LangManScope.KEY_CLASS,
        key: Any? = null
    ) {
        val resolvedKey = when (scope) {
            LangManScope.KEY_CLASS -> key ?: langMan.expectedMKType
            LangManScope.CALLER_CONTEXT -> resolveContextKey()
            LangManScope.CUSTOM -> key ?: error("CUSTOM scope requires explicit key")
        }

        val old = instances.put(resolvedKey, langMan)
        if (old != null) {
            LangMan.logger.warn("LangMan instance for '$resolvedKey' was overwritten.")
        }
    }

    fun <E : IMessageProvider<C>, C : Any> getBy(scope: LangManScope, key: Any? = null): LangMan<E, C>? {
        val resolvedKey = when (scope) {
            LangManScope.KEY_CLASS -> key ?: error("KEY_CLASS scope requires a class key")
            LangManScope.CALLER_CONTEXT -> resolveContextKey()
            LangManScope.CUSTOM -> key ?: error("CUSTOM scope requires explicit key")
        }
        @Suppress("UNCHECKED_CAST")
        return instances[resolvedKey] as? LangMan<E, C>
    }

    fun <E : IMessageProvider<C>, C : Any> getByKeyClass(keyClass: Class<*>): LangMan<E, C>? {
        var current: Class<*>? = keyClass
        while (current != null) {
            instances[current]?.let {
                @Suppress("UNCHECKED_CAST")
                return it as? LangMan<E, C>
            }
            current = current.superclass
        }
        return null
    }

    fun dumpKeys(): Map<Any, LangMan<*, *>> = instances.toMap()

    private fun resolveContextKey(): String {
        val stack = Thread.currentThread().stackTrace
        for (i in 3 until stack.size) {
            val className = stack[i].className
            if (!className.startsWith("net.ririfa.langman")) {
                val clazz = runCatching { Class.forName(className) }.getOrNull()
                val loaderId = clazz?.classLoader?.let { it::class.java.name + "@" + it.hashCode() } ?: "default"
                return "$loaderId:$className"
            }
        }
        return "default-${Thread.currentThread().id}"
    }
}
