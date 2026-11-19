package dev.plumfire.angelscript.lsp

import com.google.gson.JsonPrimitive
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.plumfire.angelscript.settings.AngelScriptSettingsListener
import dev.plumfire.angelscript.settings.EffectiveSettings
import dev.plumfire.angelscript.settings.LanguageSettings
import org.eclipse.lsp4j.ConfigurationItem
import org.eclipse.lsp4j.ConfigurationParams
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LanguageClientTest : BasePlatformTestCase() {
    private val configurationsSent = AtomicInteger()

    fun testStoppedClientNoLongerSendsSettings() {
        // LSP4IJ disposes the client of a stopped server and creates a new one for the next start.
        countingClient().dispose()
        val current = countingClient()
        try {
            val settings = EffectiveSettings(LanguageSettings())
            project.messageBus.syncPublisher(AngelScriptSettingsListener.TOPIC).settingsChanged(settings, settings)
            assertEquals(1, configurationsSent.get())
        } finally {
            current.dispose()
        }
    }

    fun testConfigurationRequestsResolveDottedSections() {
        val client = AngelScriptLanguageClient(project)
        try {
            val sections = listOf("angelScript.formatter.indentSpaces", "angelScript.builtinStringType")
            val params = ConfigurationParams(sections.map { ConfigurationItem().apply { section = it } })
            val values = client.configuration(params).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertEquals(listOf(JsonPrimitive(4), JsonPrimitive("string")), values)
        } finally {
            client.dispose()
        }
    }

    private fun countingClient() = object : AngelScriptLanguageClient(project) {
        override fun triggerChangeConfiguration() {
            configurationsSent.incrementAndGet()
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 10L
    }
}
