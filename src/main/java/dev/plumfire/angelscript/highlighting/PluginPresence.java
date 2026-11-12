package dev.plumfire.angelscript.highlighting;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

/**
 * Plugin lookups by ID. Java, because Kotlin compiled against 2026.1 reaches {@code PluginId.getId} through
 * {@code PluginId.Companion}, which does not exist before 2026.1, where {@code PluginId} is a Java class.
 */
final class PluginPresence {
    private PluginPresence() {
    }

    /** Whether the plugin with this ID is installed and enabled. */
    static boolean isEnabled(String id) {
        PluginId pluginId = PluginId.getId(id);
        return PluginManagerCore.isPluginInstalled(pluginId) && !PluginManagerCore.isDisabled(pluginId);
    }
}
