package dev.plumfire.angelscript.lsp

import com.google.gson.JsonObject
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.ServerStatus
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import dev.plumfire.angelscript.highlighting.SemanticTokenPainter
import dev.plumfire.angelscript.settings.AngelScriptSettingsListener
import dev.plumfire.angelscript.settings.AngelScriptSettingsService
import org.eclipse.lsp4j.PublishDiagnosticsParams
import java.util.concurrent.CompletableFuture

internal class AngelScriptLanguageClient(project: Project) : LanguageClientImpl(project) {
    // LSP4IJ creates a client for every server start and, on stop, calls dispose() directly rather than through
    // Disposer, so the connection cannot have the client as its parent.
    private val connection = project.messageBus.connect().apply {
        val listener = AngelScriptSettingsListener { _, _ -> triggerChangeConfiguration() }
        subscribe(AngelScriptSettingsListener.TOPIC, listener)
    }

    override fun createSettings(): Any =
        JsonObject().apply { add("angelScript", AngelScriptSettingsService.getInstance(project).serverJson()) }

    override fun handleServerStatusChanged(serverStatus: ServerStatus) {
        when (serverStatus) {
            ServerStatus.started -> project.service<SemanticTokenPainter>().refreshAll()
            ServerStatus.stopping -> project.serviceIfCreated<SemanticTokenPainter>()?.clearAll()
            else -> Unit
        }
    }

    // The server publishes diagnostics after it has processed a document, so its tokens are current then too.
    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        super.publishDiagnostics(diagnostics)
        project.serviceIfCreated<SemanticTokenPainter>()?.onServerUpdated(diagnostics.uri)
    }

    override fun refreshSemanticTokens(): CompletableFuture<Void> {
        project.serviceIfCreated<SemanticTokenPainter>()?.refreshAll()
        return CompletableFuture.completedFuture(null)
    }

    override fun dispose() {
        connection.disconnect()
        super.dispose()
    }
}
