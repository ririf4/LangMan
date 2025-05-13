package net.ririfa.langman

//TODO
object LangManLoader {

    fun <E : IMessageProvider<C>, C : Any> loadInto(
        langMan: LangMan<E, C>,
        type: InitType,
        resourcePath: String,
        outputFile: String,
        languages: List<String>
    ) {
    }

    fun flattenYamlOrTomlMap() {}

    fun flattenMessageKeys() {}
}
