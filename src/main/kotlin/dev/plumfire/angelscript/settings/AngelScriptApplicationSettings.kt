package dev.plumfire.angelscript.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/** Settings for this machine only, never read from the project. */
@Service(Service.Level.APP)
@State(
    name = "AngelScriptApplicationSettings",
    storages = [Storage("angelscript.xml", roamingType = RoamingType.DISABLED)],
)
internal class AngelScriptApplicationSettings :
    SimplePersistentStateComponent<AngelScriptApplicationSettings.Options>(Options()) {
    class Options : BaseState() {
        var serverExecutable by string()
    }

    var serverExecutable: String
        get() = state.serverExecutable.orEmpty()
        set(value) {
            state.serverExecutable = value
        }

    companion object {
        fun getInstance(): AngelScriptApplicationSettings = service()
    }
}
