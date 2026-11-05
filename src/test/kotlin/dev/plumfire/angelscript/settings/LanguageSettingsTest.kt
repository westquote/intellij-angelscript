package dev.plumfire.angelscript.settings

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import dev.plumfire.angelscript.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageSettingsTest {
    private inline fun <reified S : Setting<*>> setting(path: String): S = SETTINGS.first { it.path == path } as S

    @Test
    fun defaultsMatchTheGoldenFile() {
        val expected = JsonParser.parseString(TestData.text("settings/defaults.json"))
        assertEquals(expected, LanguageSettings().toServerJson("/project", listOf("*.as")))
    }

    @Test
    fun everyKeyMapsFromPageStateToJson() {
        val changed =
            mapOf<String, Any>(
                "builtinStringType" to "FString",
                "builtinArrayType" to "TArray",
                "explicitPropertyAccessor" to false,
                "supportsForEach" to false,
                "supportsTypedEnumerations" to true,
                "supportsDigitSeparators" to true,
                "characterLiterals" to true,
                "allowUnicodeIdentifiers" to true,
                "definedSymbols" to listOf("EDITOR"),
                "includePath" to listOf("/abs/include"),
                "forceIncludePredefined" to listOf("/abs/as.predefined"),
                "implicitMutualInclusion" to true,
                "hoistEnumParentScope" to true,
                "suppressAnalyzerErrors" to false,
                "completion.builtinKeywords" to false,
                "completion.snippets" to false,
                "files.exclude" to listOf("**/gen/**"),
                "formatter.maxBlankLines" to 3,
                "formatter.indentSpaces" to 2,
                "formatter.useTabIndent" to true,
            )
        assertEquals(SETTINGS.map { it.path }.toSet(), changed.keys)
        for (setting in SETTINGS) {
            val value = changed.getValue(setting.path)
            val settings = LanguageSettings()

            @Suppress("UNCHECKED_CAST")
            (setting as Setting<Any>).set(settings, if (value is List<*>) value.toMutableList() else value)
            val json = settings.toServerJson("/project", listOf("*.as"))
            assertEquals(setting.path, Gson().toJsonTree(value), json.getPath(setting.path))
        }
    }

    @Test
    fun pagePathsResolveAgainstTheProjectDirectory() {
        val settings = LanguageSettings(includePath = mutableListOf("Script/Shared", "../Common/./x", "D:/abs"))
        val json = settings.toServerJson("C:/work/game", listOf("*.as"))
        assertEquals(
            JsonArray().apply {
                add("C:/work/game/Script/Shared")
                add("C:/work/Common/x")
                add("D:/abs")
            },
            json.getPath("includePath"),
        )
    }

    @Test
    fun posixAndWindowsPathsNormalize() {
        assertEquals(listOf("/home/a/inc", "/opt/x"), resolvePaths(listOf("inc", "/opt/y/../x"), "/home/a"))
        assertEquals(listOf("C:/a/inc", "C:/b"), resolvePaths(listOf("inc", "C:\\b"), "C:\\a"))
    }

    @Test
    fun filePatternsAreSent() {
        val json = LanguageSettings().toServerJson("/p", listOf("*.as", "*.angelscript"))
        assertEquals(
            JsonArray().apply {
                add("*.as")
                add("*.angelscript")
            },
            json.getPath("files.angelScript"),
        )
    }

    @Test
    fun validationRules() {
        val typeName = setting<TypeNameSetting>("builtinStringType")
        assertNull(typeName.validate("Engine::FString"))
        assertNotNull(typeName.validate("1abc"))
        assertNotNull(typeName.validate("a b"))
        val symbols = setting<ListSetting>("definedSymbols")
        assertNull(symbols.validateEntries(listOf("EDITOR", "_X1")))
        assertNotNull(symbols.validateEntries(listOf("A::B")))
        val paths = setting<ListSetting>("includePath")
        assertNotNull(paths.validateEntries(listOf("")))
        assertNotNull(paths.validateEntries(listOf("a\u0000b")))
        val indent = setting<IntSetting>("formatter.indentSpaces")
        assertNull(indent.validate(16))
        assertNotNull(indent.validate(0))
        assertNotNull(indent.validate(17))
        val blank = setting<IntSetting>("formatter.maxBlankLines")
        assertNull(blank.validate(0))
        assertNotNull(blank.validate(11))
    }

    @Test
    fun readRejectsWrongTypes() {
        val bool = setting<BooleanSetting>("supportsForEach")
        assertNull(bool.read(JsonPrimitive("true"), "/p"))
        assertEquals(true, bool.read(JsonPrimitive(true), "/p"))
        val indent = setting<IntSetting>("formatter.indentSpaces")
        assertNull(indent.read(JsonPrimitive(2.5), "/p"))
        assertEquals(2, indent.read(JsonPrimitive(2), "/p"))
        assertTrue(SETTINGS.filterIsInstance<BooleanSetting>().all { it.read(JsonArray(), "/p") == null })
    }
}
