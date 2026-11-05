package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * Maps a semantic token type to its color key. The server sends no modifiers, so the table is keyed by type alone.
 * Types the lexer owns, and types the server never sends, are not painted.
 */
internal object SemanticTokenKeys {
    private val keys: Map<String, TextAttributesKey> = mapOf(
        "namespace" to AngelScriptColors.NAMESPACE,
        "class" to AngelScriptColors.CLASS,
        "interface" to AngelScriptColors.INTERFACE,
        "enum" to AngelScriptColors.ENUM,
        "enumMember" to AngelScriptColors.ENUM_MEMBER,
        "type" to AngelScriptColors.TYPE,
        "typeParameter" to AngelScriptColors.TYPE_PARAMETER,
        "parameter" to AngelScriptColors.PARAMETER,
        "variable" to AngelScriptColors.LOCAL_VARIABLE,
        "property" to AngelScriptColors.FIELD,
        "function" to AngelScriptColors.FUNCTION_CALL,
        "method" to AngelScriptColors.METHOD_CALL,
        "macro" to AngelScriptColors.MACRO,
        "keyword" to AngelScriptColors.KEYWORD,
        "controlKeyword" to AngelScriptColors.CONTROL_KEYWORD,
    )

    const val DEPRECATED = "deprecated"

    fun keyFor(tokenType: String): TextAttributesKey? = keys[tokenType]
}
