@file:Suppress("DuplicatedCode")

package net.ririfa.langman

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

object FileAutoUpdater {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)

    fun <T, E : IMessageProvider<C>, C : Any> updateIfNeeded(
        resourceRoot: String,
        outputDir: Path,
        languages: List<String>,
        fileExtensions: Set<String>,
        loader: FileLoader<T, E, C>
    ) {
        for (lang in languages) {
            for (ext in fileExtensions) {
                val fileName = "$lang.$ext"
                val resourcePath = "$resourceRoot/$fileName"
                val outPath = outputDir.resolve(fileName)

                val resourceStream = LangManLoader::class.java.getResourceAsStream(resourcePath)
                if (resourceStream == null) {
                    logger.debug("Resource not found: $resourcePath")
                    continue
                }

                // parse resource side
                val resourceData = runCatching { loader.parse(resourceStream) }.getOrNull()
                val resourceFlat = runCatching { loader.flatten(resourceData!!) }.getOrNull()
                val resourceVersionStr = resourceFlat?.get("version")?.toString()
                val resourceVersion = parseSemVer(resourceVersionStr)

                if (resourceVersion == null) {
                    logger.warn("Invalid or missing version in resource: $resourcePath")
                    continue
                }

                if (!Files.exists(outPath)) {
                    logger.info("Language file missing, copying: $outPath (version $resourceVersionStr)")
                    Files.copy(
                        LangManLoader::class.java.getResourceAsStream(resourcePath)!!,
                        outPath
                    )
                    continue
                }

                val fileStream = Files.newInputStream(outPath)
                val fileData = runCatching { loader.parse(fileStream) }.getOrNull()
                val fileFlat = runCatching { loader.flatten(fileData!!) }.getOrNull()
                val fileVersionStr = fileFlat?.get("version")?.toString()
                val fileVersion = parseSemVer(fileVersionStr)

                if (fileVersion == null || compareSemVer(resourceVersion, fileVersion) > 0) {
                    logger.info("Updating $outPath from v${fileVersionStr ?: "?"} to v$resourceVersionStr")
                    Files.copy(
                        LangManLoader::class.java.getResourceAsStream(resourcePath)!!,
                        outPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                } else {
                    logger.debug("No update needed for {} (version {} >= {})", outPath, fileVersionStr, resourceVersionStr)
                }
            }
        }
    }

    private fun parseSemVer(version: String?): List<Int>? {
        return version
            ?.split('.')
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
    }

    private fun compareSemVer(a: List<Int>, b: List<Int>): Int {
        val maxLength = maxOf(a.size, b.size)
        for (i in 0 until maxLength) {
            val av = a.getOrNull(i) ?: 0
            val bv = b.getOrNull(i) ?: 0
            if (av != bv) return av - bv
        }
        return 0
    }

}
