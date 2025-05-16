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
                val resourceVersion = resourceFlat?.get("version")?.toIntOrNull()

                if (resourceVersion == null) {
                    logger.warn("No valid version found in resource: $resourcePath")
                    continue
                }

                if (!Files.exists(outPath)) {
                    logger.info("Language file missing, copying: $outPath (version $resourceVersion)")
                    resourceStream.reset()
                    Files.copy(
                        LangManLoader::class.java.getResourceAsStream(resourcePath)!!,
                        outPath
                    )
                    continue
                }

                val fileStream = Files.newInputStream(outPath)
                val fileData = runCatching { loader.parse(fileStream) }.getOrNull()
                val fileFlat = runCatching { loader.flatten(fileData!!) }.getOrNull()
                val fileVersion = fileFlat?.get("version")?.toIntOrNull()

                if (fileVersion == null || resourceVersion > fileVersion) {
                    logger.info("Updating $outPath from v${fileVersion ?: "?"} to v$resourceVersion")
                    Files.copy(
                        LangManLoader::class.java.getResourceAsStream(resourcePath)!!,
                        outPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                } else {
                    logger.debug("No update needed for {} (version {} >= {})", outPath, fileVersion, resourceVersion)
                }
            }
        }
    }
}
