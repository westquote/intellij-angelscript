package dev.plumfire.angelscript.lang

import com.intellij.lang.Language

internal object AngelScriptLanguage : Language("AngelScript") {
    @Suppress("unused")
    private fun readResolve(): Any = AngelScriptLanguage
}
