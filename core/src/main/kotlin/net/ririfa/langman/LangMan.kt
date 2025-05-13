@file:Suppress("DuplicatedCode")

package net.ririfa.langman

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.full.isSubclassOf

class LangMan<E : IMessageProvider<C>, C : Any> internal constructor(
    internal val isDebug: Boolean,
    internal val textFactory: TextFactory<C>,
    val expectedMKType: Class<out MessageKey<E, C>>,
) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(LangMan::class.java.simpleName)
    }

    /**
     * A mutable map that holds localized messages organized by language codes and message keys.
     *
     * The outer map uses strings as keys, representing language codes (e.g., "en", "fr").
     * The inner map pairs `MessageKey` instances with their corresponding localized message strings.
     *
     * This structure is a core component of the language manager, allowing for efficient
     * storage and retrieval of translations based on language and specific message keys.
     */
    val messages: MutableMap<String, MutableMap<MessageKey<E, C>, String>> = mutableMapOf()

    /**
     * A mutable map that manages replacement logic for specific types.
     *
     * Each entry in the map associates a class type with a corresponding function
     * that defines the logic for replacing or transforming values for that type.
     *
     * - Key: The class type for which replacement logic is defined.
     * - Value: A function that takes three parameters:
     *   1. The original value of type `Any`.
     *   2. A string representing an identifier or key.
     *   3. A new value of type `Any` to replace or transform.
     *
     * The function returns the transformed or replaced value.
     *
     * This map is primarily utilized by the `registerReplacementLogic` method
     * to dynamically add replacement or transformation logic for various types.
     */
    val replaceLogic = mutableMapOf<Class<*>, (Any, String, Any) -> Any>()

    /**
     * A mutable map that associates a `Class` type with a conversion function.
     * The conversion function transforms an input of type `Any` into an `Any`
     * object of the specified target class type.
     *
     * This map can be utilized for dynamic type conversion by providing a mapping
     * between a target class type and its corresponding transformation logic.
     * It supports extensible and custom transformations for various type requirements.
     */
    val convertToFinalType = mutableMapOf<Class<*>, (Any) -> Any>()

    @JvmOverloads
    fun <K, V> getMessage(key: MessageKey<*, *>, argsComplete: Map<K, V>, lang: String = "en"): C {
        require(key::class.isSubclassOf(expectedMKType.kotlin)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        var message = messages[lang]?.get(key) ?: key.rc()

        // Replace placeholders using the provided arguments
        for ((placeholder, value) in argsComplete) {
            message = message.replace("%$placeholder%", value.toString())
        }

        return textFactory.invoke(message)
    }

    fun getAvailableLanguages(): Set<String> = messages.keys

    inline fun <reified I : Any> registerReplacementLogic(
        noinline logic: (I, String, I) -> I
    ) {
        replaceLogic[I::class.java] = logic as (Any, String, Any) -> Any
    }

    fun <I : Any> registerReplacementLogic(
        clazz: Class<I>,
        logic: (I, String, I) -> I
    ) {
        replaceLogic[clazz] = logic as (Any, String, Any) -> Any
    }

    /**
     * Registers a conversion logic from type `I` to type `C`.
     *
     * @param I The source type.
     * @param C The target type.
     * @param converter A function that converts an instance of type `I` to type `C`.
     *
     * @throws ClassCastException If the specified conversion logic cannot be cast to `(Any) -> Any`.
     *
     * Example usage:
     * ```
     * registerConversionLogic<String, Text> { Text.of(it) }
     * ```
     */
    inline fun <reified I : Any, reified C : Any> registerConversionLogic(
        noinline converter: (I) -> C
    ) {
        convertToFinalType[C::class.java] = converter as (Any) -> Any
    }

    fun <I : Any, C : Any> registerConversionLogic(
        clazz: Class<C>,
        converter: (I) -> C
    ) {
        convertToFinalType[clazz] = converter as (Any) -> Any
    }
}