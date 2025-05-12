package net.ririfa.langman

import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized registry and resolution mechanism for LangMan instances.
 *
 * This object allows LangMan instances to be stored and retrieved in three different ways:
 *
 * 1. **By explicit key** — such as Class<*> or custom Strings.
 * 2. **By caller context (scope)** — automatically resolved from the current call stack.
 * 3. **By associated MessageKey class** — using reverse traversal of sealed class hierarchies.
 *
 * This design ensures flexible LangMan instance separation, useful in modular plugin/mod environments
 * (e.g., Fabric, Bukkit, Velocity) where multiple projects may coexist on the same thread or classloader.
 */
internal object LangManContext {
    /** Unified instance registry shared across all resolution strategies. */
    private val instances = ConcurrentHashMap<Any, LangMan>()

    /**
     * Registers a LangMan instance using an arbitrary key.
     *
     * @param key Any object to act as the identifier (e.g., a Class, String, UUID, etc.)
     * @param langMan The LangMan instance to register.
     */
    fun register(key: Any, langMan: LangMan) {
        instances[key] = langMan
    }

    /**
     * Retrieves a LangMan instance using an arbitrary key.
     *
     * @param key The identifier used to store the LangMan.
     * @return The LangMan instance if found, or null.
     */
    fun getByKey(key: Any): LangMan? = instances[key]

    /**
     * Registers a LangMan scoped to a specific class and classloader,
     * intended for per-module or per-plugin separation.
     *
     * @param ownerClass The class whose ClassLoader and name will be used as scope identifier.
     * @param langMan The LangMan instance to register.
     */
    fun registerScoped(ownerClass: Class<*>, langMan: LangMan) {
        val loaderId = ownerClass.classLoader.let { it::class.java.name + "@" + it.hashCode() }
        val key = "$loaderId:${ownerClass.name}"
        instances[key] = langMan
    }

    /**
     * Retrieves a LangMan instance by inspecting the current call stack and resolving the caller class + classloader.
     *
     * This is useful for context-aware utility functions like logging or default resolution.
     *
     * @return The scoped LangMan instance, or null if not found.
     */
    fun getScoped(): LangMan? {
        val key = resolveContextKey()
        return instances[key]
    }

    /**
     * Retrieves a LangMan instance based on a MessageKey class type.
     *
     * This supports sealed class hierarchies and will traverse upward to find a matching registered class.
     *
     * @param keyClass The MessageKey class.
     * @return The matching LangMan instance, or null if not found.
     */
    fun getByKeyClass(keyClass: Class<*>): LangMan? {
        var current: Class<*>? = keyClass
        while (current != null) {
            instances[current]?.let { return it }
            current = current.superclass
        }
        return null
    }

    /**
     * Resolves a context key string based on the first external class
     * found in the current thread’s call stack.
     *
     * Format: `ClassLoaderName@HashCode:ClassName`
     *
     * @return The resolved context key string.
     */
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
