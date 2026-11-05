package dev.plumfire.angelscript.settings

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.messages.Topic
import dev.plumfire.angelscript.AngelScriptBundle.message
import dev.plumfire.angelscript.lang.AngelScriptFileType
import dev.plumfire.angelscript.lang.AngelScriptLexer
import dev.plumfire.angelscript.lsp.AngelScriptNotifications
import dev.plumfire.angelscript.settings.AngelScriptSettingsListener.Companion.TOPIC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/** Settings in effect: the page values with the `.angelscript.json` overrides applied. */
internal data class EffectiveSettings(
    val values: LanguageSettings,
    val fromFile: Set<String> = emptySet(),
    val extraFilePatterns: List<String> = emptyList(),
)

// The replacement, TrustedProjects.isProjectTrusted, is internal API before 2026.1. The platform trusts every project
// in tests, so tests replace the check to cover an untrusted one.
@Suppress("DEPRECATION")
@VisibleForTesting
internal var projectTrust: (Project) -> Boolean = { it.isTrusted() }

internal fun isProjectTrusted(project: Project): Boolean = projectTrust(project)

internal fun interface AngelScriptSettingsListener {
    fun settingsChanged(old: EffectiveSettings, new: EffectiveSettings)

    companion object {
        @Topic.ProjectLevel
        val TOPIC = Topic(AngelScriptSettingsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}

@Service(Service.Level.PROJECT)
@State(name = "AngelScriptSettings", storages = [Storage("angelscript.xml")])
internal class AngelScriptSettingsService(private val project: Project, private val scope: CoroutineScope) :
    PersistentStateComponent<LanguageSettings> {
    @Volatile private var pageState = LanguageSettings()

    // Written only under the lock, so a slow first read cannot replace a newer result.
    @Volatile private var cached: EffectiveSettings? = null
    private val lock = Any()
    private val loadRequested = AtomicBoolean()

    @Volatile private var reportedProblem: String? = null

    override fun getState(): LanguageSettings = pageState

    override fun loadState(state: LanguageSettings) {
        pageState = state
    }

    private val configPath: Path?
        get() = project.basePath?.let { Path.of(it, CONFIG_FILE_NAME) }

    /** The effective settings. The first call reads `.angelscript.json` from disk. */
    fun effective(): EffectiveSettings = cached ?: synchronized(lock) { cached ?: load().also { cached = it } }

    /** The `angelScript` section for the server. */
    fun serverJson(): JsonObject {
        val settings = effective()
        val patterns = FileTypeManager.getInstance().getAssociations(AngelScriptFileType).map { it.presentableString }
        val allPatterns = (listOf("*.as") + patterns + settings.extraFilePatterns).distinct()
        return settings.values.toServerJson(project.basePath.orEmpty(), allPatterns)
    }

    /** Re-reads the settings in the background and publishes a change if the effective settings differ. */
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            synchronized(lock) {
                val new = load()
                val old = cached ?: EffectiveSettings(pageState.deepCopy())
                cached = new
                if (old != new) project.messageBus.syncPublisher(TOPIC).settingsChanged(old, new)
            }
        }
    }

    /** Stores values edited on the settings page: into `.angelscript.json` when it exists, else in the project. */
    fun applyPage(edited: LanguageSettings) {
        val current = effective().values
        val changed = SETTINGS.filter { it.get(edited) != it.get(current) }
        if (changed.isEmpty()) return
        val path = configPath
        if (path != null && isProjectTrusted(project) && Files.isRegularFile(path)) {
            writeConfigFile(path, changed, edited)
        } else {
            pageState = edited.deepCopy()
            refresh()
        }
    }

    private fun lexerSettings(): LanguageSettings {
        cached?.let { return it.values }
        if (loadRequested.compareAndSet(false, true)) refresh()
        return pageState
    }

    private fun load(): EffectiveSettings {
        val values = pageState.deepCopy()
        val file = readConfigFile()
        file.overrides.forEach { it.applyTo(values) }
        report(file)
        val fromFile = file.overrides.mapTo(HashSet()) { it.setting.path }
        return EffectiveSettings(values, fromFile, file.extraFilePatterns)
    }

    private fun readConfigFile(): ConfigFile {
        val path = configPath ?: return ConfigFile.ABSENT
        if (!isProjectTrusted(project) || !Files.isRegularFile(path)) return ConfigFile.ABSENT
        return try {
            ConfigFile.parse(Files.readString(path), path.parent.toString())
        } catch (e: IOException) {
            thisLogger().warn("Cannot read $path", e)
            ConfigFile.invalid(e.message)
        }
    }

    private fun report(file: ConfigFile) {
        val problem = when {
            file.error != null -> message("config.invalid", file.error)
            file.ignoredKeys.isNotEmpty() -> message("config.ignored", file.ignoredKeys.joinToString())
            else -> null
        }
        if (problem != null && problem != reportedProblem) AngelScriptNotifications.warn(project, problem)
        reportedProblem = problem
    }

    // Rewrites the whole file with two-space indentation; the server reads the same keys either way.
    private fun writeConfigFile(path: Path, changed: List<Setting<*>>, edited: LanguageSettings) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)
        val document = file?.let { FileDocumentManager.getInstance().getDocument(it) }
        val root = try {
            document?.let { JsonParser.parseString(it.text) } as? JsonObject
        } catch (_: JsonParseException) {
            null
        }
        if (document == null || root == null) throw ConfigurationException(message("config.not.writable"))
        changed.forEach { root.setPath(it.path, it.fileJson(edited)) }
        WriteCommandAction.runWriteCommandAction(project, message("config.write.command"), null, {
            document.setText(GSON.toJson(root) + "\n")
            FileDocumentManager.getInstance().saveDocument(document)
        })
    }

    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        fun getInstance(project: Project): AngelScriptSettingsService = project.service()

        /** A lexer configured with the project's lexical settings, without file I/O. */
        fun createLexer(project: Project?): AngelScriptLexer {
            val open = project?.takeUnless { it.isDisposed || it.isDefault }
            val settings = open?.let { getInstance(it).lexerSettings() } ?: LanguageSettings()
            return AngelScriptLexer(settings.supportsDigitSeparators, settings.allowUnicodeIdentifiers)
        }
    }
}
