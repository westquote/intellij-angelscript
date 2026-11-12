package dev.plumfire.angelscript.settings

import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
    fun invalidValuesAndReservedKeysAreListedWhileOthersApply() {
        val file =
            ConfigFile.parse(
                """
                {
                  "builtinStringType": "not a type",
                  "supportsForEach": "yes",
                  "formatter": { "indentSpaces": 40, "useTabIndent": true },
                  "completion": true,
                  "definedSymbols": ["OK", "NOT OK"],
                  "serverExecutable": "/bin/sh",
                  "trace": { "server": "verbose" },
                  "${'$'}schema": 1,
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
                "completion",
                "definedSymbols",
                "serverExecutable",
                "trace",
                "${'$'}schema",
            ),
            file.ignoredKeys.toSet(),
        )
        assertEquals(
            mapOf("formatter.useTabIndent" to true, "characterLiterals" to true),
            file.overrides.associate { it.setting.path to it.value },
        )
        assertEquals(JsonObject(), file.passThrough)
    }

    @Test
    fun unknownKeysPassThroughAsWritten() {
        val file =
            ConfigFile.parse(
                """
                {
                  "newSetting": { "depth": [1, "two"] },
                  "formatter": { "indentSpaces": 2, "spaceBeforeParen": true },
                  "files": { "angelScript": ["*.asc"], "watch": false },
                  "builtinStringType": "FString"
                }
                """.trimIndent(),
                "/p",
            )
        assertEquals(emptyList<String>(), file.ignoredKeys)
        assertEquals(
            JsonParser.parseString(
                """
                {
                  "newSetting": { "depth": [1, "two"] },
                  "formatter": { "spaceBeforeParen": true },
                  "files": { "watch": false }
                }
                """,
            ),
            file.passThrough,
        )
        assertEquals(2, file.value("formatter.indentSpaces"))
    }

    @Test
    fun namesWithADotAreListedAndNotSent() {
        val file =
            ConfigFile.parse(
                """{ "formatter.indentSpaces": 2, "foo.bar": 1, "foo": 3 }""",
                "/p",
            )
        assertEquals(setOf("formatter.indentSpaces", "foo.bar"), file.ignoredKeys.toSet())
        assertEquals(JsonParser.parseString("""{ "foo": 3 }"""), file.passThrough)
        assertNull(file.value("formatter.indentSpaces"))
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
