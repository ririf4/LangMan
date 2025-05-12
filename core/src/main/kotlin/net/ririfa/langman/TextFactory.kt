package net.ririfa.langman

interface TextFactory<T : Any> {
    val clazz: Class<T>
    fun invoke(text: String): T
}
