package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

/**
 * The plugin's color keys. A key with a Rider C++ counterpart falls back to it when the IDE's color scheme has
 * attributes for it, and to a platform key otherwise.
 *
 * A key's fallback is fixed when the key is created, so the keys are created when this object is first used,
 * not when the plugin loads. Switching to a scheme with different C++ keys takes effect after a restart.
 */
internal object AngelScriptColors {
    private val scheme = EditorColorsManager.getInstance().globalScheme

    private val keys = mutableListOf<TextAttributesKey>()

    /** Every key, in declaration order. */
    val ALL: List<TextAttributesKey> get() = keys

    @JvmField val KEYWORD = key("KEYWORD", Default.KEYWORD)

    @JvmField val CONTROL_KEYWORD = key("CONTROL_KEYWORD", Default.KEYWORD)

    @JvmField val NUMBER = key("NUMBER", Default.NUMBER)

    @JvmField val STRING = key("STRING", Default.STRING)

    @JvmField val LINE_COMMENT = key("LINE_COMMENT", Default.LINE_COMMENT)

    @JvmField val BLOCK_COMMENT = key("BLOCK_COMMENT", Default.BLOCK_COMMENT)

    @JvmField val OPERATOR = key("OPERATOR", Default.OPERATION_SIGN)

    @JvmField val BRACES = key("BRACES", Default.BRACES)

    @JvmField val PARENTHESES = key("PARENTHESES", Default.PARENTHESES)

    @JvmField val BRACKETS = key("BRACKETS", Default.BRACKETS)

    @JvmField val SEMICOLON = key("SEMICOLON", Default.SEMICOLON)

    @JvmField val COMMA = key("COMMA", Default.COMMA)

    @JvmField val DOT = key("DOT", Default.DOT)

    @JvmField val METADATA = key("METADATA", Default.METADATA)

    @JvmField val PREPROCESSOR = key("PREPROCESSOR", Default.METADATA)

    @JvmField val IDENTIFIER = key("IDENTIFIER", Default.IDENTIFIER)

    @JvmField val BAD_CHARACTER = key("BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    @JvmField val NAMESPACE = key("NAMESPACE", Default.CLASS_REFERENCE)

    @JvmField val CLASS = key("CLASS", Default.CLASS_NAME, "CLASS_IDENTIFIER")

    @JvmField val INTERFACE = key("INTERFACE", Default.INTERFACE_NAME, "CLASS_IDENTIFIER")

    @JvmField val ENUM = key("ENUM", Default.CLASS_NAME, "ENUM_IDENTIFIER")

    @JvmField val ENUM_MEMBER = key("ENUM_MEMBER", Default.STATIC_FIELD, "ENUM_ENUMERATOR_IDENTIFIER")

    @JvmField val TYPE = key("TYPE", Default.CLASS_NAME, "CLASS_IDENTIFIER")

    @JvmField val TYPE_PARAMETER = key("TYPE_PARAMETER", Default.PARAMETER, "TEMPLATE_PARAMETER_IDENTIFIER")

    @JvmField val PARAMETER = key("PARAMETER", Default.PARAMETER)

    @JvmField val LOCAL_VARIABLE = key("LOCAL_VARIABLE", Default.LOCAL_VARIABLE, "LOCAL_VARIABLE_IDENTIFIER")

    @JvmField val FIELD = key("FIELD", Default.INSTANCE_FIELD, "CLASS_FIELD_IDENTIFIER")

    @JvmField val FUNCTION_CALL = key("FUNCTION_CALL", Default.FUNCTION_CALL, "GLOBAL_FUNCTION_IDENTIFIER")

    @JvmField val METHOD_CALL = key("METHOD_CALL", Default.INSTANCE_METHOD)

    @JvmField val MACRO = key("MACRO", Default.METADATA, "PREPROCESSOR_MACRO_IDENTIFIER")

    /** [cppName] if [hasAttributes] says the scheme defines it, else [platformKey]. */
    internal fun fallback(
        cppName: String,
        platformKey: TextAttributesKey,
        hasAttributes: (TextAttributesKey) -> Boolean,
    ): TextAttributesKey {
        // find() creates an empty key when the IDE has none, so presence is decided by the attributes.
        val cppKey = TextAttributesKey.find(cppName)
        return if (hasAttributes(cppKey)) cppKey else platformKey
    }

    private fun key(name: String, platformKey: TextAttributesKey, cppName: String? = null): TextAttributesKey {
        val fallback = cppName?.let { fallback("ReSharper.CPP_$it", platformKey, ::hasAttributes) } ?: platformKey
        return TextAttributesKey.createTextAttributesKey("ANGELSCRIPT_$name", fallback).also { keys += it }
    }

    private fun hasAttributes(key: TextAttributesKey): Boolean = scheme.getAttributes(key)?.isEmpty == false
}
