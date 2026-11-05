package dev.plumfire.angelscript.settings

import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

/** Re-reads `.angelscript.json` when it is created, changed, moved or deleted. */
internal class ConfigFileListener(private val project: Project) : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        val service = project.serviceIfCreated<AngelScriptSettingsService>() ?: return
        val configPath = project.basePath?.let { "$it/$CONFIG_FILE_NAME" } ?: return
        if (events.any { it.path == configPath || (it as? VFilePropertyChangeEvent)?.oldPath == configPath }) {
            service.refresh()
        }
    }
}
