package net.ririfa.langman

import java.io.InputStream
import java.nio.file.Path

interface FileLoader<T, E : IMessageProvider<C>, C : Any> {

    val fileExtensions: Set<String>

    fun parse(path: Path): T
    fun parse(stream: InputStream): T

    fun flatten(data: T, parent: String = ""): Map<String, String>

    fun resolve(
        data: T,
        keyMap: Map<String, MessageKey<E, C>>
    ): MutableMap<MessageKey<E, C>, String> {
        return flatten(data).mapNotNull { (key, value) ->
            val mk = keyMap[key.lowercase()]
            if (mk != null) mk to value else null
        }.toMap().toMutableMap()
    }
}
