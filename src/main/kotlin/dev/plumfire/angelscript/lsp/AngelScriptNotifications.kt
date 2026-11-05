package dev.plumfire.angelscript.lsp

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import dev.plumfire.angelscript.AngelScriptBundle.message
import dev.plumfire.angelscript.settings.AngelScriptConfigurable

internal object AngelScriptNotifications {
    fun warn(project: Project, content: String, withSettingsAction: Boolean = false) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup("AngelScript")
        val notification = group.createNotification(message("notification.title"), content, NotificationType.WARNING)
        if (withSettingsAction) {
            notification.addAction(
                NotificationAction.createSimpleExpiring(message("notification.open.settings")) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, AngelScriptConfigurable::class.java)
                },
            )
        }
        notification.notify(project)
    }
}
