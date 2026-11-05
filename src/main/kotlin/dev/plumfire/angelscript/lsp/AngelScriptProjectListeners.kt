package dev.plumfire.angelscript.lsp

import com.intellij.ide.trustedProjects.TrustedProjectsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import dev.plumfire.angelscript.highlighting.SemanticTokenPainter
import dev.plumfire.angelscript.lang.isAngelScript
import dev.plumfire.angelscript.settings.AngelScriptSettingsListener
import dev.plumfire.angelscript.settings.AngelScriptSettingsService
import dev.plumfire.angelscript.settings.EffectiveSettings

/** Applies a settings change to open editors and the painter. */
internal class SettingsChangeHandler(private val project: Project) : AngelScriptSettingsListener {
    override fun settingsChanged(old: EffectiveSettings, new: EffectiveSettings) {
        val lexerChanged =
            old.values.supportsDigitSeparators != new.values.supportsDigitSeparators ||
                old.values.allowUnicodeIdentifiers != new.values.allowUnicodeIdentifiers
        if (lexerChanged) ApplicationManager.getApplication().invokeLater({ rehighlight() }, project.disposed)
        project.serviceIfCreated<SemanticTokenPainter>()?.refreshAll()
    }

    private fun rehighlight() {
        val documents = FileDocumentManager.getInstance()
        for (editor in EditorFactory.getInstance().allEditors) {
            val file = documents.getFile(editor.document) ?: continue
            if (editor.project == project && file.isAngelScript() && editor is EditorEx) {
                editor.highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file)
            }
        }
    }
}

/** Reads `.angelscript.json` and starts the server once the user trusts the project. */
internal class TrustChangeHandler : TrustedProjectsListener {
    override fun onProjectTrusted(project: Project) {
        project.serviceIfCreated<AngelScriptSettingsService>()?.refresh()
        AngelScriptServerFactory.startIfReady(project)
    }
}
