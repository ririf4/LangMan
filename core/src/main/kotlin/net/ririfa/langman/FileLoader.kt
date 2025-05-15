package net.ririfa.langman

import java.io.InputStream
import java.nio.file.Path

interface FileLoader<T> {
    val fileExtension: Set<String>
    fun load(path: Path): T
    fun loadFromStream(input: InputStream): T
}
