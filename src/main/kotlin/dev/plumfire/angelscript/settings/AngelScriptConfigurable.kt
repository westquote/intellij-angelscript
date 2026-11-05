package dev.plumfire.angelscript.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import dev.plumfire.angelscript.AngelScriptBundle.message
import dev.plumfire.angelscript.lsp.AngelScriptServerFactory
import dev.plumfire.angelscript.lsp.ServerBinaryLocator
import java.awt.Dimension

internal class AngelScriptConfigurable(private val project: Project) : BoundConfigurable(message("settings.title")) {
    private val service = AngelScriptSettingsService.getInstance(project)
    private val edited = LanguageSettings()
    private var fromFile = emptySet<String>()
    private var serverExecutable = ""

    override fun createPanel(): DialogPanel {
        load()
        return panel {
            for ((group, settings) in SETTINGS.groupBy { it.group }) {
                group(message("settings.group.$group")) { settings.forEach { settingRow(it) } }
            }
            group(message("settings.group.server")) {
                row(message("settings.serverExecutable")) {
                    textFieldWithBrowseButton(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(), project)
                        .bindText(::serverExecutable)
                        .align(AlignX.FILL)
                        .validationOnApply { validateExecutable(it.text)?.let(::error) }
                        .comment(message("settings.serverExecutable.comment"))
                }
            }
        }
    }

    override fun reset() {
        load()
        super.reset()
    }

    override fun apply() {
        super.apply()
        service.applyPage(edited)
        val application = AngelScriptApplicationSettings.getInstance()
        if (application.serverExecutable != serverExecutable) {
            application.serverExecutable = serverExecutable
            ProjectManager.getInstance().openProjects.forEach(AngelScriptServerFactory::restart)
        }
    }

    private fun load() {
        val effective = service.effective()
        val values = effective.values.deepCopy()
        SETTINGS.forEach { it.copy(values, edited) }
        fromFile = effective.fromFile
        serverExecutable = AngelScriptApplicationSettings.getInstance().serverExecutable
    }

    private fun validateExecutable(path: String): String? =
        if (path.isBlank()) null else ServerBinaryLocator.validateOverride(path)

    private fun Panel.settingRow(setting: Setting<*>) {
        val label = message("setting.${setting.path}")
        val row = when (setting) {
            is BooleanSetting -> row {
                checkBox(label).bindSelected({ setting.get(edited) }, { setting.set(edited, it) })
            }
            is IntSetting -> row(label) {
                spinner(setting.range).bindIntValue({ setting.get(edited) }, { setting.set(edited, it) })
            }
            is TypeNameSetting -> row(label) {
                textField()
                    .bindText({ setting.get(edited) }, { setting.set(edited, it) })
                    .validationOnApply { setting.validate(it.text)?.let(::error) }
            }
            is ListSetting -> row(label) {
                val model = CollectionListModel<String>()
                val property = MutableProperty({ setting.get(edited) }, { setting.set(edited, it.toMutableList()) })
                cell(listEditor(setting, model))
                    .align(Align.FILL)
                    .bind({ model.items }, { _, items -> model.replaceAll(items) }, property)
                    .validationOnApply { setting.validateEntries(model.items)?.let(::error) }
            }
        }
        if (setting.path in fromFile) row.rowComment(message("settings.fromFile"))
    }

    private fun listEditor(setting: ListSetting, model: CollectionListModel<String>) = ToolbarDecorator
        .createDecorator(JBList(model))
        .setAddAction { addEntry(setting)?.let(model::add) }
        .disableUpDownActions()
        .setPreferredSize(Dimension(0, LIST_HEIGHT))
        .createPanel()

    private fun addEntry(setting: ListSetting): String? {
        if (setting.kind != ListSetting.Kind.PATHS) {
            return Messages.showInputDialog(project, message("settings.addEntry"), message("settings.title"), null)
        }
        val descriptor = when (setting.path) {
            "includePath" -> FileChooserDescriptorFactory.createSingleFolderDescriptor()
            else -> FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        }
        val chosen = FileChooser.chooseFile(descriptor, project, null) ?: return null
        val base = project.basePath ?: return chosen.path
        return FileUtil.getRelativePath(base, chosen.path, '/')?.takeUnless { it.startsWith("..") } ?: chosen.path
    }

    private companion object {
        const val LIST_HEIGHT = 90
    }
}
