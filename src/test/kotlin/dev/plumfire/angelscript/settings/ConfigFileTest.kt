package dev.plumfire.angelscript.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigFileTest {
    private fun ConfigFile.value(path: String) = overrides.singleOrNull { it.setting.path == path }?.value

    @Test
    fun typicalFileIsRead() {
        val file =
            ConfigFile.parse(
                """
                {
                  "${'$'}schema": "https://example.com/schema.json",
                  "builtinStringType": "FString",
                  "explicitPropertyAccessor": true,
                  "definedSymbols": ["EDITOR"],
                  "includePath": ["Script/Shared"],
                  "formatter": { "indentSpaces": 4 }
                }
                """.trimIndent(),
                "/work/game",
            )
        assertNull(file.error)
        assertEquals(emptyList<String>(), file.ignoredKeys)
        assertEquals("FString", file.value("builtinStringType"))
        assertEquals(true, file.value("explicitPropertyAccessor"))
        assertEquals(listOf("EDITOR"), file.value("definedSymbols"))
        assertEquals(listOf("/work/game/Script/Shared"), file.value("includePath"))
        assertEquals(4, file.value("formatter.indentSpaces"))
        assertEquals(5, file.overrides.size)
    }

    @Test
    fun relativePathsResolveAgainstTheFileDirectoryOnWindows() {
        val file = ConfigFile.parse("""{ "forceIncludePredefined": ["..\\api\\as.predefined"] }""", "C:\\work\\game")
        assertEquals(listOf("C:/work/api/as.predefined"), file.value("forceIncludePredefined"))
    }

    @Test
    fun notJsonIsIgnoredWithThePosition() {
        val file = ConfigFile.parse("{ \"builtinStringType\": ", "/p")
        val error = file.error
        assertTrue(error.toString(), error != null && "line" in error)
        assertTrue(file.overrides.isEmpty())
    }

    @Test
    fun notAnObjectIsIgnored() {
        val file = ConfigFile.parse("[1, 2]", "/p")
        assertNotNull(file.error)
        assertTrue(file.overrides.isEmpty())
    }

    @Test
    fun invalidValuesAndUnknownKeysAreListedWhileOthersApply() {
        val file =
            ConfigFile.parse(
                """
                {
                  "builtinStringType": "not a type",
                  "supportsForEach": "yes",
                  "formatter": { "indentSpaces": 40, "useTabIndent": true, "width": 3 },
                  "definedSymbols": ["OK", "NOT OK"],
                  "serverExecutable": "/bin/sh",
                  "trace": { "server": "verbose" },
                  "characterLiterals": true
                }
                """.trimIndent(),
                "/p",
            )
        assertNull(file.error)
        assertEquals(
            setOf(
                "builtinStringType",
                "supportsForEach",
                "formatter.indentSpaces",
                "formatter.width",
                "definedSymbols",
                "serverExecutable",
                "trace",
            ),
            file.ignoredKeys.toSet(),
        )
        assertEquals(
            mapOf("formatter.useTabIndent" to true, "characterLiterals" to true),
            file.overrides.associate { it.setting.path to it.value },
        )
    }

    @Test
    fun extraFilePatternsAreAccepted() {
        val file =
            ConfigFile.parse(
                """{ "files": { "angelScript": ["*.angelscript"], "exclude": ["gen/**"] } }""",
                "/p",
            )
        assertEquals(listOf("*.angelscript"), file.extraFilePatterns)
        assertEquals(listOf("gen/**"), file.value("files.exclude"))
    }

    @Test
    fun fileValuesTakePrecedenceOverPageValues() {
        val page = LanguageSettings(builtinStringType = "PageString", supportsForEach = false)
        val file = ConfigFile.parse("""{ "builtinStringType": "FileString" }""", "/p")
        val effective = page.deepCopy()
        file.overrides.forEach { it.applyTo(effective) }
        assertEquals("FileString", effective.builtinStringType)
        assertEquals(false, effective.supportsForEach)
        assertEquals("array", effective.builtinArrayType)
    }
}
