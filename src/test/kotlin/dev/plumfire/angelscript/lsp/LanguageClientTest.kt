package dev.plumfire.angelscript.lsp

import com.google.gson.JsonPrimitive
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import dev.plumfire.angelscript.settings.AngelScriptSettingsListener
import dev.plumfire.angelscript.settings.EffectiveSettings
import dev.plumfire.angelscript.settings.LanguageSettings
import org.eclipse.lsp4j.ConfigurationItem
import org.eclipse.lsp4j.ConfigurationParams
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.WorkspaceService
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LanguageClientTest : BasePlatformTestCase() {
    private val configurationsSent = AtomicInteger()

    fun testStoppedClientNoLongerSendsSettings() {
        // LSP4IJ disposes the client of a stopped server and creates a new one for the next start.
        connectedClient().dispose()
        val current = connectedClient()
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

    private fun connectedClient(): AngelScriptLanguageClient {
        val workspace = proxy<WorkspaceService> { method ->
            if (method.name == "didChangeConfiguration") configurationsSent.incrementAndGet()
            null
        }
        val server = proxy<LanguageServer> { method -> if (method.name == "getWorkspaceService") workspace else null }
        // connect() also needs the LSP4IJ server wrapper; the settings path reads only the server field.
        val client = AngelScriptLanguageClient(project)
        LanguageClientImpl::class.java.getDeclaredField("server").apply { isAccessible = true }.set(client, server)
        return client
    }

    private inline fun <reified T> proxy(crossinline handler: (Method) -> Any?): T =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(T::class.java)) { self, method, args ->
            when (method.name) {
                "hashCode" -> System.identityHashCode(self)
                "equals" -> self === args?.firstOrNull()
                "toString" -> T::class.java.simpleName
                else -> handler(method)
            }
        } as T

    private companion object {
        const val TIMEOUT_SECONDS = 10L
    }
}
