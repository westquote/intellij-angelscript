package dev.plumfire.angelscript.lsp

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.system.CpuArch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class ServerBinaryLocatorTest : UsefulTestCase() {
    private lateinit var directory: Path

    override fun setUp() {
        super.setUp()
        directory = Files.createTempDirectory("angelscript-locator")
    }

    override fun tearDown() {
        try {
            directory.toFile().deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testPlatformMatrix() {
        val expected =
            mapOf(
                (OsFamily.WINDOWS to CpuArch.X86_64) to "server/windows-x64/angel-lsp.exe",
                (OsFamily.MACOS to CpuArch.ARM64) to "server/macos-arm64/angel-lsp",
                (OsFamily.MACOS to CpuArch.X86_64) to "server/macos-x64/angel-lsp",
                (OsFamily.LINUX to CpuArch.X86_64) to "server/linux-x64/angel-lsp",
            )
        for (os in OsFamily.entries) {
            for (arch in CpuArch.entries) {
                assertEquals("$os $arch", expected[os to arch], ServerBinaryLocator.bundledPath(os, arch))
            }
        }
    }

    fun testUnsupportedPlatformNamesThePlatform() {
        val result = ServerBinaryLocator.resolve("", directory, OsFamily.LINUX, CpuArch.ARM64)
        assertTrue(result.toString(), (result as ServerBinary.Missing).reason.contains("linux-arm64"))
    }

    fun testMissingBundledBinary() {
        val result = ServerBinaryLocator.resolve("", directory, OsFamily.WINDOWS, CpuArch.X86_64)
        assertInstanceOf(result, ServerBinary.Missing::class.java)
    }

    fun testBundledBinaryIsFound() {
        val binary = directory.resolve("server/windows-x64/angel-lsp.exe")
        Files.createDirectories(binary.parent)
        Files.writeString(binary, "")
        assertEquals(
            ServerBinary.Found(binary),
            ServerBinaryLocator.resolve("", directory, OsFamily.WINDOWS, CpuArch.X86_64),
        )
    }

    fun testOverrideValidation() {
        assertNotNull(ServerBinaryLocator.validateOverride("relative/angel-lsp"))
        assertNotNull(ServerBinaryLocator.validateOverride(directory.resolve("missing").toString()))
        assertNotNull(ServerBinaryLocator.validateOverride(directory.toString()))
        val file = Files.writeString(directory.resolve(if (SystemInfo.isWindows) "server.exe" else "server"), "")
        if (!SystemInfo.isWindows) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"))
            assertNotNull(ServerBinaryLocator.validateOverride(file.toString()))
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"))
        }
        assertNull(ServerBinaryLocator.validateOverride(file.toString()))
        assertEquals(
            ServerBinary.Found(file),
            ServerBinaryLocator.resolve(file.toString(), null, OsFamily.OTHER, CpuArch.OTHER),
        )
    }

    fun testOverrideIsNeverMadeExecutable() {
        if (SystemInfo.isWindows) return
        val file = Files.writeString(directory.resolve("server"), "")
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"))
        assertInstanceOf(
            ServerBinaryLocator.resolve(file.toString(), null, OsFamily.LINUX, CpuArch.X86_64),
            ServerBinary.Missing::class.java,
        )
        assertEquals("rw-r--r--", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)))
    }

    fun testBundledBinaryBecomesExecutable() {
        if (SystemInfo.isWindows) return
        val binary = directory.resolve("server/linux-x64/angel-lsp")
        Files.createDirectories(binary.parent)
        Files.writeString(binary, "")
        Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rw-r--r--"))
        assertEquals(
            ServerBinary.Found(binary),
            ServerBinaryLocator.resolve("", directory, OsFamily.LINUX, CpuArch.X86_64),
        )
        assertEquals("rwxr--r--", PosixFilePermissions.toString(Files.getPosixFilePermissions(binary)))
    }
}
