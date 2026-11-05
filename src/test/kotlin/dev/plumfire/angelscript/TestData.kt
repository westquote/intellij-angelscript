package dev.plumfire.angelscript

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText

object TestData {
    val root: Path = Path.of(System.getProperty("angelscript.testData") ?: "src/test/testData")

    fun text(relative: String): String = root.resolve(relative).readText()

    /** Every AngelScript file under `lexer/` and `sample/`. */
    fun lexerFixtures(): List<String> = listOf("lexer", "sample").flatMap { directory ->
        Files.walk(root.resolve(directory)).use { paths ->
            paths
                .filter { it.extension == "as" || it.name.endsWith("as.predefined") }
                .map { it.readText() }
                .toList()
        }
    }
}
