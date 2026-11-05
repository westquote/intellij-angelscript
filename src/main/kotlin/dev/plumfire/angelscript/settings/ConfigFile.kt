package dev.plumfire.angelscript.settings

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import dev.plumfire.angelscript.AngelScriptBundle.message

internal const val CONFIG_FILE_NAME = ".angelscript.json"

/** The content of a `.angelscript.json` file. */
internal class ConfigFile(
    val overrides: List<Override<*>> = emptyList(),
    val extraFilePatterns: List<String> = emptyList(),
    val ignoredKeys: List<String> = emptyList(),
    val error: String? = null,
) {
    companion object {
        private val NESTED = setOf("completion", "files", "formatter")
        private const val FILE_PATTERNS = "files.angelScript"

        val ABSENT = ConfigFile()

        fun invalid(error: String?) = ConfigFile(error = error)

        /** Parses [text]; relative paths resolve against [directory]. */
        fun parse(text: String, directory: String): ConfigFile {
            val root = try {
                JsonParser.parseString(text) as? JsonObject ?: return invalid(message("config.notObject"))
            } catch (e: JsonParseException) {
                return invalid(e.cause?.message ?: e.message)
            }
            val ignored = unknownKeys(root).toMutableList()
            val overrides = ArrayList<Override<*>>()
            for (setting in SETTINGS) {
                val element = root.getPath(setting.path) ?: continue
                val override = setting.readOverride(element, directory)
                if (override == null) ignored += setting.path else overrides += override
            }
            val patternsElement = root.getPath(FILE_PATTERNS)
            val patterns = patternsElement?.asStringListOrNull()?.takeIf { it.all(::isValidPath) }
            if (patternsElement != null && patterns == null) ignored += FILE_PATTERNS
            return ConfigFile(overrides, patterns.orEmpty(), ignored)
        }

        private fun unknownKeys(root: JsonObject): List<String> {
            val known = SETTINGS.mapTo(HashSet()) { it.path } + FILE_PATTERNS
            return root.entrySet().flatMap { (name, value) ->
                when {
                    name == "\$schema" -> if (value is JsonPrimitive && value.isString) emptyList() else listOf(name)
                    name in NESTED && value is JsonObject -> value.keySet().map { "$name.$it" }.filter { it !in known }
                    else -> listOf(name).filter { it !in known }
                }
            }
        }
    }
}
