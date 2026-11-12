package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

/**
 * The plugin's color keys. A key with a Rider C++ counterpart falls back to it when Rider's C++ support is enabled,
 * and to a platform key otherwise.
 *
 * The choice depends only on the installed plugins, because the platform forbids reading the color scheme while a
 * class initializes.
 */
internal object AngelScriptColors {
    private val cppColors = PluginPresence.isEnabled("com.jetbrains.rider-cpp")

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

    /** `ReSharper.CPP_`[cppName] if [cppColors], else [platformKey]. */
    internal fun fallback(cppName: String?, platformKey: TextAttributesKey, cppColors: Boolean): TextAttributesKey =
        if (cppName != null && cppColors) TextAttributesKey.find("ReSharper.CPP_$cppName") else platformKey

    private fun key(name: String, platformKey: TextAttributesKey, cppName: String? = null): TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("ANGELSCRIPT_$name", fallback(cppName, platformKey, cppColors))
            .also { keys += it }
}
