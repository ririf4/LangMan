package net.ririfa.langman

import java.util.concurrent.ConcurrentHashMap

internal object LangManContext {
    private val instances = ConcurrentHashMap<Class<*>, LangMan<*, *>>()

    fun <E : IMessageProvider<C>, C : Any> register(langMan: LangMan<E, C>) {
        val key = langMan.expectedMKType
        val old = instances.put(key, langMan)
        if (old != null) {
            LangMan.logger.warn("LangMan instance for '$key' was overwritten.")
        }
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

    fun dumpKeys(): Map<Class<*>, LangMan<*, *>> = instances.toMap()
}
