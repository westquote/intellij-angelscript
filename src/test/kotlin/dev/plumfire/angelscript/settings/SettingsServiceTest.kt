package dev.plumfire.angelscript.settings

import com.google.gson.JsonParser
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.plumfire.angelscript.lang.AngelScriptFileType
import dev.plumfire.angelscript.lsp.AngelScriptServerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.nio.file.Files
import java.nio.file.Path

class SettingsServiceTest : BasePlatformTestCase() {
    private val scope = CoroutineScope(SupervisorJob())
    private val platformTrust = projectTrust
    private lateinit var configFile: Path

    override fun setUp() {
        super.setUp()
        configFile = Path.of(project.basePath!!, CONFIG_FILE_NAME)
        Files.createDirectories(configFile.parent)
    }

    override fun tearDown() {
        try {
            scope.cancel()
            projectTrust = platformTrust
            Files.deleteIfExists(configFile)
        } finally {
            super.tearDown()
        }
    }

    // A service of its own, so that no test sees settings cached by another.
    private fun newService() = AngelScriptSettingsService(project, scope)

    fun testApplyWritesOnlyTheChangedKeysIntoTheConfigFile() {
        Files.writeString(configFile, """{ "${'$'}schema": "schema.json", "builtinStringType": "FString" }""")
        val service = newService()
        service.applyPage(service.effective().values.deepCopy().apply { supportsForEach = false })
        val written = JsonParser.parseString(Files.readString(configFile)).asJsonObject
        assertEquals(setOf("\$schema", "builtinStringType", "supportsForEach"), written.keySet())
        assertEquals("FString", written["builtinStringType"].asString)
        assertFalse(written["supportsForEach"].asBoolean)
        assertEquals(LanguageSettings(), service.state)
    }

    fun testApplyWithoutAConfigFileStoresInTheProject() {
        val service = newService()
        service.applyPage(LanguageSettings(supportsForEach = false))
        assertFalse(service.state.supportsForEach)
        assertFalse(Files.exists(configFile))
    }

    fun testAnUntrustedProjectIgnoresTheConfigFileAndStartsNoServer() {
        Files.writeString(configFile, """{ "builtinStringType": "FString" }""")
        projectTrust = { false }
        assertEquals("string", newService().effective().values.builtinStringType)
        assertFalse(AngelScriptServerFactory.canStart(project))
        projectTrust = platformTrust
        assertEquals("FString", newService().effective().values.builtinStringType)
    }

    fun testFilePatternsFollowTheFileType() {
        val matcher = ExtensionFileNameMatcher("angelscript")
        runWriteAction { FileTypeManager.getInstance().associate(AngelScriptFileType, matcher) }
        try {
            val patterns = newService().serverJson().getPath("files.angelScript")!!.asJsonArray.map { it.asString }
            assertEquals(listOf("*.as", "*.angelscript"), patterns)
        } finally {
            runWriteAction { FileTypeManager.getInstance().removeAssociation(AngelScriptFileType, matcher) }
        }
    }
}
