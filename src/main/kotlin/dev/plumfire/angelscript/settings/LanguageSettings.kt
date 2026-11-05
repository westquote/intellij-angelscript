package dev.plumfire.angelscript.settings

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import dev.plumfire.angelscript.AngelScriptBundle.message
import kotlin.reflect.KMutableProperty1

/** The server settings stored on the project settings page. */
internal data class LanguageSettings(
    var builtinStringType: String = "string",
    var builtinArrayType: String = "array",
    var explicitPropertyAccessor: Boolean = true,
    var supportsForEach: Boolean = true,
    var supportsTypedEnumerations: Boolean = false,
    var supportsDigitSeparators: Boolean = false,
    var characterLiterals: Boolean = false,
    var allowUnicodeIdentifiers: Boolean = false,
    var definedSymbols: MutableList<String> = mutableListOf(),
    var includePath: MutableList<String> = mutableListOf(),
    var forceIncludePredefined: MutableList<String> = mutableListOf(),
    var suppressAnalyzerErrors: Boolean = true,
    var implicitMutualInclusion: Boolean = false,
    var hoistEnumParentScope: Boolean = false,
    var filesExclude: MutableList<String> = mutableListOf(),
    var completionBuiltinKeywords: Boolean = true,
    var completionSnippets: Boolean = true,
    var formatterMaxBlankLines: Int = 1,
    var formatterIndentSpaces: Int = 4,
    var formatterUseTabIndent: Boolean = false,
) {
    fun deepCopy(): LanguageSettings = copy(
        definedSymbols = definedSymbols.toMutableList(),
        includePath = includePath.toMutableList(),
        forceIncludePredefined = forceIncludePredefined.toMutableList(),
        filesExclude = filesExclude.toMutableList(),
    )
}

/** One key of the server's `angelScript` section, the page group that shows it, and the property that holds it. */
internal sealed class Setting<T : Any>(
    val path: String,
    val group: String,
    private val property: KMutableProperty1<LanguageSettings, T>,
) {
    fun get(settings: LanguageSettings): T = property.get(settings)

    fun set(settings: LanguageSettings, value: T) = property.set(settings, value)

    fun copy(from: LanguageSettings, to: LanguageSettings) = set(to, get(from))

    /** The error message for [value], or null if it is valid. */
    abstract fun validate(value: T): String?

    /** Reads a JSON value, or returns null if it has the wrong type or is invalid. */
    fun read(element: JsonElement, baseDirectory: String): T? =
        parse(element)?.takeIf { validate(it) == null }?.let { resolve(it, baseDirectory) }

    fun readOverride(element: JsonElement, baseDirectory: String): Override<T>? =
        read(element, baseDirectory)?.let { Override(this, it) }

    /** The value as written to `.angelscript.json`. */
    fun fileJson(settings: LanguageSettings): JsonElement = toJson(get(settings))

    /** The value as sent to the server, with relative paths resolved against [baseDirectory]. */
    fun serverJson(settings: LanguageSettings, baseDirectory: String): JsonElement =
        toJson(resolve(get(settings), baseDirectory))

    protected abstract fun parse(element: JsonElement): T?

    protected abstract fun toJson(value: T): JsonElement

    protected open fun resolve(value: T, baseDirectory: String): T = value
}

internal class BooleanSetting(path: String, group: String, property: KMutableProperty1<LanguageSettings, Boolean>) :
    Setting<Boolean>(path, group, property) {
    override fun validate(value: Boolean): String? = null

    override fun parse(element: JsonElement): Boolean? = (element as? JsonPrimitive)?.takeIf { it.isBoolean }?.asBoolean

    override fun toJson(value: Boolean): JsonElement = JsonPrimitive(value)
}

internal class IntSetting(
    path: String,
    group: String,
    property: KMutableProperty1<LanguageSettings, Int>,
    val range: IntRange,
) : Setting<Int>(path, group, property) {
    override fun validate(value: Int): String? =
        if (value in range) null else message("invalid.range", range.first, range.last)

    override fun parse(element: JsonElement): Int? =
        (element as? JsonPrimitive)?.takeIf { it.isNumber }?.asString?.toIntOrNull()

    override fun toJson(value: Int): JsonElement = JsonPrimitive(value)
}

internal class TypeNameSetting(path: String, group: String, property: KMutableProperty1<LanguageSettings, String>) :
    Setting<String>(path, group, property) {
    override fun validate(value: String): String? =
        if (TYPE_NAME.matches(value)) null else message("invalid.type", value)

    override fun parse(element: JsonElement): String? = (element as? JsonPrimitive)?.takeIf { it.isString }?.asString

    override fun toJson(value: String): JsonElement = JsonPrimitive(value)

    private companion object {
        val TYPE_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*(::[A-Za-z_][A-Za-z0-9_]*)*")
    }
}

internal class ListSetting(
    path: String,
    group: String,
    property: KMutableProperty1<LanguageSettings, MutableList<String>>,
    val kind: Kind,
) : Setting<MutableList<String>>(path, group, property) {
    enum class Kind { SYMBOLS, PATHS, GLOBS }

    override fun validate(value: MutableList<String>): String? = validateEntries(value)

    fun validateEntries(entries: List<String>): String? = if (kind == Kind.SYMBOLS) {
        entries.firstOrNull { !SYMBOL.matches(it) }?.let { message("invalid.symbol", it) }
    } else {
        entries.firstOrNull { !isValidPath(it) }?.let { message("invalid.entry", it) }
    }

    override fun parse(element: JsonElement): MutableList<String>? = element.asStringListOrNull()?.toMutableList()

    override fun toJson(value: MutableList<String>): JsonElement = stringArray(value)

    override fun resolve(value: MutableList<String>, baseDirectory: String): MutableList<String> =
        if (kind == Kind.PATHS) resolvePaths(value, baseDirectory).toMutableList() else value

    private companion object {
        val SYMBOL = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}

/** A value that `.angelscript.json` sets for one key. */
internal data class Override<T : Any>(val setting: Setting<T>, val value: T) {
    fun applyTo(settings: LanguageSettings) = setting.set(settings, value)
}

/** Every key the plugin sends except the derived `files.angelScript` and `trace.server`, in page order. */
internal val SETTINGS: List<Setting<*>> = listOf(
    TypeNameSetting("builtinStringType", "language", LanguageSettings::builtinStringType),
    TypeNameSetting("builtinArrayType", "language", LanguageSettings::builtinArrayType),
    BooleanSetting("explicitPropertyAccessor", "language", LanguageSettings::explicitPropertyAccessor),
    BooleanSetting("supportsForEach", "language", LanguageSettings::supportsForEach),
    BooleanSetting("supportsTypedEnumerations", "language", LanguageSettings::supportsTypedEnumerations),
    BooleanSetting("supportsDigitSeparators", "language", LanguageSettings::supportsDigitSeparators),
    BooleanSetting("characterLiterals", "language", LanguageSettings::characterLiterals),
    BooleanSetting("allowUnicodeIdentifiers", "language", LanguageSettings::allowUnicodeIdentifiers),
    ListSetting("definedSymbols", "preprocessor", LanguageSettings::definedSymbols, ListSetting.Kind.SYMBOLS),
    ListSetting("includePath", "preprocessor", LanguageSettings::includePath, ListSetting.Kind.PATHS),
    ListSetting(
        "forceIncludePredefined",
        "preprocessor",
        LanguageSettings::forceIncludePredefined,
        ListSetting.Kind.PATHS,
    ),
    BooleanSetting("suppressAnalyzerErrors", "analysis", LanguageSettings::suppressAnalyzerErrors),
    BooleanSetting("implicitMutualInclusion", "analysis", LanguageSettings::implicitMutualInclusion),
    BooleanSetting("hoistEnumParentScope", "analysis", LanguageSettings::hoistEnumParentScope),
    ListSetting("files.exclude", "analysis", LanguageSettings::filesExclude, ListSetting.Kind.GLOBS),
    BooleanSetting("completion.builtinKeywords", "completion", LanguageSettings::completionBuiltinKeywords),
    BooleanSetting("completion.snippets", "completion", LanguageSettings::completionSnippets),
    IntSetting("formatter.maxBlankLines", "formatting", LanguageSettings::formatterMaxBlankLines, 0..10),
    IntSetting("formatter.indentSpaces", "formatting", LanguageSettings::formatterIndentSpaces, 1..16),
    BooleanSetting("formatter.useTabIndent", "formatting", LanguageSettings::formatterUseTabIndent),
)

/** Resolves relative paths against [baseDirectory] and normalizes all of them. */
internal fun resolvePaths(paths: List<String>, baseDirectory: String): List<String> = paths.map { path ->
    val absolute = if (OSAgnosticPathUtil.isAbsolute(path)) path else "$baseDirectory/$path"
    FileUtil.toCanonicalPath(FileUtil.toSystemIndependentName(absolute))
}

internal fun isValidPath(path: String): Boolean = path.isNotEmpty() && '\u0000' !in path

/** Builds the `angelScript` section sent to the server. */
internal fun LanguageSettings.toServerJson(baseDirectory: String, filePatterns: List<String>): JsonObject {
    val root = JsonObject()
    for (setting in SETTINGS) root.setPath(setting.path, setting.serverJson(this, baseDirectory))
    root.setPath("files.angelScript", stringArray(filePatterns))
    root.setPath("trace.server", JsonPrimitive("off"))
    return root
}

internal fun stringArray(values: List<String>): JsonArray = JsonArray(values.size).apply { values.forEach(::add) }

internal fun JsonObject.setPath(path: String, value: JsonElement) {
    val parts = path.split('.')
    var node = this
    for (part in parts.dropLast(1)) {
        node = node.get(part) as? JsonObject ?: JsonObject().also { node.add(part, it) }
    }
    node.add(parts.last(), value)
}

internal fun JsonObject.getPath(path: String): JsonElement? {
    var node: JsonElement = this
    for (part in path.split('.')) node = (node as? JsonObject)?.get(part) ?: return null
    return node
}

internal fun JsonElement.asStringListOrNull(): List<String>? {
    val array = this as? JsonArray ?: return null
    return array.map { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.asString ?: return null }
}
