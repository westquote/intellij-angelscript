package dev.plumfire.angelscript.lsp

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.system.CpuArch
import dev.plumfire.angelscript.AngelScriptBundle.message
import dev.plumfire.angelscript.settings.AngelScriptApplicationSettings
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

internal sealed interface ServerBinary {
    data class Found(val path: Path) : ServerBinary

    data class Missing(val reason: String) : ServerBinary
}

internal enum class OsFamily { WINDOWS, MACOS, LINUX, OTHER }

internal object ServerBinaryLocator {
    private val SUPPORTED = setOf("windows-x64", "macos-arm64", "macos-x64", "linux-x64")

    @Volatile private var cached: Pair<String, ServerBinary>? = null

    /** The platform name, such as `linux-arm64`, used for the bundled directory and in messages. */
    fun platformName(os: OsFamily, arch: CpuArch): String {
        val archName = when (arch) {
            CpuArch.X86_64 -> "x64"
            CpuArch.ARM64 -> "arm64"
            else -> arch.name
        }
        return "$os-$archName".lowercase()
    }

    /** The bundled executable relative to the plugin directory, or null on an unsupported platform. */
    fun bundledPath(os: OsFamily, arch: CpuArch): String? {
        val platform = platformName(os, arch).takeIf { it in SUPPORTED } ?: return null
        return "server/$platform/" + if (os == OsFamily.WINDOWS) "angel-lsp.exe" else "angel-lsp"
    }

    /** The reason [path] cannot be used as the server executable override, or null if it can. */
    fun validateOverride(path: String): String? {
        val file = try {
            Path.of(path)
        } catch (_: InvalidPathException) {
            null
        }
        return when {
            file == null || !file.isAbsolute -> message("server.override.not.absolute", path)
            !Files.exists(file) -> message("server.override.missing", path)
            !Files.isRegularFile(file) -> message("server.override.not.file", path)
            !Files.isExecutable(file) -> message("server.override.not.executable", path)
            else -> null
        }
    }

    /** Resolves the override, or else the bundled binary of this platform. */
    fun resolve(override: String, pluginDirectory: Path?, os: OsFamily, arch: CpuArch): ServerBinary {
        if (override.isNotBlank()) {
            return validateOverride(override)?.let(ServerBinary::Missing) ?: ServerBinary.Found(Path.of(override))
        }
        val platform = platformName(os, arch)
        val relative = bundledPath(os, arch) ?: return ServerBinary.Missing(message("server.unsupported", platform))
        val binary = pluginDirectory?.resolve(relative)
        if (binary == null || !Files.isRegularFile(binary)) {
            return ServerBinary.Missing(message("server.missing", platform, binary ?: relative))
        }
        return makeExecutable(binary)?.let(ServerBinary::Missing) ?: ServerBinary.Found(binary)
    }

    /** The executable for this IDE session; resolved once per override value. */
    fun locate(): ServerBinary {
        val override = AngelScriptApplicationSettings.getInstance().serverExecutable
        cached?.takeIf { it.first == override }?.let { return it.second }
        // PluginId.getId compiles to a companion call against 2026.1 that 2025.1 cannot link.
        val pluginDirectory = PluginManager.getPluginByClass(javaClass)?.pluginPath
        val os = when {
            SystemInfo.isWindows -> OsFamily.WINDOWS
            SystemInfo.isMac -> OsFamily.MACOS
            SystemInfo.isLinux -> OsFamily.LINUX
            else -> OsFamily.OTHER
        }
        val result = resolve(override, pluginDirectory, os, CpuArch.CURRENT)
        if (result is ServerBinary.Missing) logger<ServerBinaryLocator>().warn(result.reason)
        cached = override to result
        return result
    }

    // The plugin installer does not reliably keep the ZIP's permission bits.
    private fun makeExecutable(binary: Path): String? {
        if (Files.isExecutable(binary)) return null
        return try {
            Files.setPosixFilePermissions(
                binary,
                Files.getPosixFilePermissions(binary) + PosixFilePermission.OWNER_EXECUTE,
            )
            null
        } catch (e: IOException) {
            message("server.not.executable", binary, e.message.orEmpty())
        } catch (e: UnsupportedOperationException) {
            message("server.not.executable", binary, e.message.orEmpty())
        }
    }
}
