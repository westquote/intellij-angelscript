package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.LSPIJUtils
import com.redhat.devtools.lsp4ij.LanguageServerManager
import dev.plumfire.angelscript.lang.isAngelScript
import dev.plumfire.angelscript.lsp.AngelScriptServerFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Paints the server's semantic tokens as range highlighters on open AngelScript documents.
 *
 * LSP4IJ's own semantic token support is turned off in [AngelScriptServerFactory.createClientFeatures]. Switching to
 * it means deleting this file, its listeners in `plugin.xml` and the painter calls in `AngelScriptLanguageClient`.
 */
@Service(Service.Level.PROJECT)
internal class SemanticTokenPainter(private val project: Project, private val scope: CoroutineScope) : Disposable {
    private class DocumentState(val uri: String) {
        var job: Job? = null

        @Volatile var highlighters: List<RangeHighlighter> = emptyList()
    }

    private class Tokens(val stamp: Long, val spans: List<TokenSpan>)

    private val documents = ConcurrentHashMap<Document, DocumentState>()
    private val loggedFailures = ConcurrentHashMap.newKeySet<String>()

    fun track(file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        documents.computeIfAbsent(document) { DocumentState(LSPIJUtils.toUriAsString(file)) }
        schedule(document, 0)
    }

    fun untrack(file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getCachedDocument(file) ?: return
        val state = documents.remove(document) ?: return
        synchronized(state) { state.job?.cancel() }
        removeAll(state)
    }

    fun onDocumentChanged(document: Document) {
        if (documents.containsKey(document)) schedule(document, DEBOUNCE_MS)
    }

    /** Requests tokens for [uri] unless an edit to it is still being debounced. */
    fun onServerUpdated(uri: String) {
        val (document, state) = documents.entries.firstOrNull { it.value.uri == uri } ?: return
        val debouncing = synchronized(state) { state.job?.isActive == true }
        if (!debouncing) schedule(document, 0)
    }

    fun refreshAll() {
        documents.keys.forEach { schedule(it, 0) }
    }

    fun clearAll() {
        ApplicationManager.getApplication().invokeLater({ documents.values.forEach(::removeAll) }, project.disposed)
    }

    /**
     * Paints [spans] on [document] if it is tracked and still at [stamp], keeping highlighters that are already
     * right. Runs on the EDT.
     */
    fun paint(document: Document, stamp: Long, spans: List<TokenSpan>) {
        val state = documents[document]
        if (state == null || document.modificationStamp != stamp) return
        val plan = SemanticTokens.diff(state.highlighters.map { liveSpan(it) to it }, spans)
        val markup = DocumentMarkupModel.forDocument(document, project, true)
        plan.remove.forEach(RangeHighlighter::dispose)
        val added = plan.add.map { markup.addRangeHighlighter(it.key, it.start, it.end, LAYER, EXACT_RANGE) }
        state.highlighters = plan.keep + added
    }

    override fun dispose() {
        val states = documents.values.toList()
        documents.clear()
        // A plugin unload disposes services on the EDT, where the highlighters must go before the classes do.
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            states.forEach(::removeAll)
        } else {
            application.invokeLater { states.forEach(::removeAll) }
        }
    }

    private fun schedule(document: Document, delayMs: Long) {
        val state = documents[document] ?: return
        synchronized(state) {
            state.job?.cancel()
            state.job = scope.launch {
                delay(delayMs)
                update(document, state)
            }
        }
    }

    // A failure costs only this document's semantic colors, so every exception is caught and logged once.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun update(document: Document, state: DocumentState) {
        try {
            val tokens = requestTokens(document, state.uri) ?: return
            withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
                paint(document, tokens.stamp, tokens.spans)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            if (loggedFailures.add(e.javaClass.name)) thisLogger().warn("No semantic tokens for ${state.uri}", e)
        }
    }

    private suspend fun requestTokens(document: Document, uri: String): Tokens? {
        val manager = LanguageServerManager.getInstance(project)
        val server = manager.getLanguageServer(AngelScriptServerFactory.SERVER_ID).await() ?: return null
        val legend = server.serverCapabilities?.semanticTokensProvider?.legend ?: return null
        val stamp = document.modificationStamp
        val params = SemanticTokensParams(TextDocumentIdentifier(uri))
        val documentService = server.initializedServer.await().textDocumentService
        val tokens = documentService.semanticTokensFull(params).await() ?: return null
        return readAction {
            if (document.modificationStamp != stamp) return@readAction null
            Tokens(stamp, SemanticTokens.decode(tokens.data, legend, document))
        }
    }

    private fun liveSpan(highlighter: RangeHighlighter): TokenSpan? {
        val key = highlighter.textAttributesKey
        if (!highlighter.isValid || key == null) return null
        return TokenSpan(highlighter.startOffset, highlighter.endOffset, key)
    }

    private fun removeAll(state: DocumentState) {
        state.highlighters.forEach(RangeHighlighter::dispose)
        state.highlighters = emptyList()
    }

    companion object {
        private const val DEBOUNCE_MS = 300L

        // Above the lexer's colors, below diagnostics.
        const val LAYER = HighlighterLayer.ADDITIONAL_SYNTAX
    }
}

internal class SemanticTokenEditorListener(private val project: Project) : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.isAngelScript()) project.service<SemanticTokenPainter>().track(file)
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        if (!file.isAngelScript() || source.isFileOpen(file)) return
        project.serviceIfCreated<SemanticTokenPainter>()?.untrack(file)
    }
}

internal class SemanticTokenDocumentListener : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
        if (!file.isAngelScript()) return
        for (project in ProjectManager.getInstance().openProjects) {
            project.serviceIfCreated<SemanticTokenPainter>()?.onDocumentChanged(event.document)
        }
    }
}
