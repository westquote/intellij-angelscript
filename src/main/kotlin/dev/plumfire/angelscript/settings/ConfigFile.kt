package dev.plumfire.angelscript.settings

import com.google.gson.JsonElement
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
    /** Keys the plugin does not model, sent to the server as written. */
    val passThrough: JsonObject = JsonObject(),
    val error: String? = null,
) {
    companion object {
        private val NESTED = setOf("completion", "files", "formatter")
        private const val FILE_PATTERNS = "files.angelScript"
        private const val SCHEMA = "\$schema"

        // Not sent from the file: a malformed `$schema` or group, the IDE's own trace setting, and the machine-level
        // executable.
        private val RESERVED = NESTED + setOf(SCHEMA, "trace", "serverExecutable")

        val ABSENT = ConfigFile()

        fun invalid(error: String?) = ConfigFile(error = error)

        /** Parses [text]; relative paths resolve against [directory]. */
        fun parse(text: String, directory: String): ConfigFile {
            val root = try {
                JsonParser.parseString(text) as? JsonObject ?: return invalid(message("config.notObject"))
            } catch (e: JsonParseException) {
                return invalid(e.cause?.message ?: e.message)
            }
            val passThrough = JsonObject()
            val ignored = ArrayList<String>()
            for ((path, value) in unknownKeys(root)) {
                if (path in RESERVED) ignored += path else passThrough.setPath(path, value.deepCopy())
            }
            val overrides = ArrayList<Override<*>>()
            for (setting in SETTINGS) {
                val element = root.getPath(setting.path) ?: continue
                val override = setting.readOverride(element, directory)
                if (override == null) ignored += setting.path else overrides += override
            }
            val patternsElement = root.getPath(FILE_PATTERNS)
            val patterns = patternsElement?.asStringListOrNull()?.takeIf { it.all(::isValidPath) }
            if (patternsElement != null && patterns == null) ignored += FILE_PATTERNS
            return ConfigFile(overrides, patterns.orEmpty(), ignored, passThrough)
        }

        /** The keys the plugin does not model, with their values. */
        private fun unknownKeys(root: JsonObject): List<Pair<String, JsonElement>> {
            val known = SETTINGS.mapTo(HashSet()) { it.path } + FILE_PATTERNS
            return root.entrySet().flatMap { (name, value) ->
                when {
                    name == SCHEMA && value is JsonPrimitive && value.isString -> emptyList()
                    name in NESTED && value is JsonObject -> value.entrySet().map { (key, v) -> "$name.$key" to v }
                    else -> listOf(name to value)
                }.filter { it.first !in known }
            }
        }
    }
}
