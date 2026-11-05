package dev.plumfire.angelscript.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPSemanticTokensFeature
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import dev.plumfire.angelscript.lang.isAngelScript
import dev.plumfire.angelscript.settings.isProjectTrusted
import java.nio.file.Path

internal class AngelScriptServerFactory :
    LanguageServerFactory,
    LanguageServerEnablementSupport {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        val binary = when (val located = ServerBinaryLocator.locate()) {
            is ServerBinary.Found -> located.path
            is ServerBinary.Missing -> error(located.reason)
        }
        val directory = project.basePath?.let(Path::of)
        val commandLine = GeneralCommandLine(binary.toString(), "--stdio").withWorkingDirectory(directory)
        return OSProcessStreamConnectionProvider(commandLine)
    }

    override fun createLanguageClient(project: Project): LanguageClientImpl = AngelScriptLanguageClient(project)

    // SemanticTokenPainter paints the server's tokens, so LSP4IJ must not request them too.
    override fun createClientFeatures(): LSPClientFeatures = LSPClientFeatures().setSemanticTokensFeature(
        object : LSPSemanticTokensFeature() {
            override fun isEnabled(file: PsiFile): Boolean = false
        },
    )

    override fun isEnabled(project: Project): Boolean = canStart(project)

    override fun setEnabled(enabled: Boolean, project: Project) {
        project.putUserData(DISABLED_BY_USER, !enabled)
    }

    companion object {
        const val SERVER_ID = "dev.plumfire.angelscript.server"
        private val DISABLED_BY_USER = Key.create<Boolean>("angelscript.server.disabledByUser")
        private val REPORTED = Key.create<Boolean>("angelscript.server.reported")

        /** True when the project is trusted and an executable resolves; reports a failure once per project. */
        fun canStart(project: Project): Boolean {
            val disabled = project.getUserData(DISABLED_BY_USER) == true
            if (disabled || !isProjectTrusted(project)) return false
            val binary = ServerBinaryLocator.locate()
            if (binary is ServerBinary.Missing && project.getUserData(REPORTED) == null) {
                project.putUserData(REPORTED, true)
                AngelScriptNotifications.warn(project, binary.reason, withSettingsAction = true)
            }
            return binary is ServerBinary.Found
        }

        /** Starts the server if it can run and an AngelScript file is open. */
        fun startIfReady(project: Project) {
            if (!project.isDisposed && hasOpenFile(project) && canStart(project)) {
                LanguageServerManager.getInstance(project).start(SERVER_ID)
            }
        }

        /** Restarts the server after the executable setting changed, or stops it if the new one cannot run. */
        fun restart(project: Project) {
            if (project.isDisposed) return
            project.putUserData(REPORTED, null)
            val manager = LanguageServerManager.getInstance(project)
            if (hasOpenFile(project) && canStart(project)) {
                manager.start(SERVER_ID, LanguageServerManager.StartOptions().setForceRestart(true))
            } else {
                manager.stop(SERVER_ID)
            }
        }

        private fun hasOpenFile(project: Project) =
            FileEditorManager.getInstance(project).openFiles.any { it.isAngelScript() }
    }
}
