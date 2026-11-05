import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
    alias(libs.plugins.changelog)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

val jvmVersion = libs.versions.jvm.get()

kotlin {
    jvmToolchain(jvmVersion.toInt())
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmVersion)
        apiVersion = KotlinVersion.fromVersion(libs.versions.kotlinApi.get())
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.platform)
        plugin("com.redhat.devtools.lsp4ij", libs.versions.lsp4ij.get())
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Metrics)
        pluginVerifier()
        zipSigner()
    }
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        description =
            providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"
                check(start in it && end in it) { "README.md has no plugin description markers" }
                markdownToHTML(it.substringAfter(start).substringBefore(end))
            }
        val changelog = project.changelog
        changeNotes =
            providers.gradleProperty("pluginVersion").map { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            create(IntelliJPlatformType.Rider, libs.versions.platform.get())
            create(IntelliJPlatformType.IntellijIdea, libs.versions.platform.get())
            create(IntelliJPlatformType.IntellijIdeaCommunity, libs.versions.platformLowest.get())
        }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = listOf("beta")
    }
}

changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(layout.projectDirectory.file("config/detekt.yml"))
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.versions.detektKotlin.get())
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// The bundled language server: one executable per platform, built from the angel-lsp commit pinned in
// gradle/libs.versions.toml. CI builds all four; a local build takes the ones it finds.
val serverExecutables =
    mapOf(
        "windows-x64" to "angel-lsp-server-win32-x64.exe",
        "macos-arm64" to "angel-lsp-server-darwin-arm64",
        "macos-x64" to "angel-lsp-server-darwin-x64",
        "linux-x64" to "angel-lsp-server-linux-x64",
    )

abstract class PrepareServerTask : DefaultTask() {
    @get:Input
    abstract val executables: MapProperty<String, String>

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val serverDirectory: DirectoryProperty

    @get:Input
    abstract val requireAllPlatforms: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginNotices: RegularFileProperty

    @get:Input
    abstract val serverVersion: Property<String>

    @get:Input
    abstract val nodeVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val source =
            serverDirectory.orNull?.asFile
                ?: throw GradleException("Set -PangelLspServerDir to the folder with the server executables.")
        val out = outputDirectory.get().asFile.apply { deleteRecursively() }
        for ((platform, name) in executables.get()) {
            val executable = source.resolve(name)
            if (!executable.isFile) {
                if (requireAllPlatforms.get()) throw GradleException("Server executable not found: $executable")
                logger.warn("No server executable for $platform: $executable")
                continue
            }
            logger.lifecycle("${sha256(executable)}  $name")
            val target = if (platform.startsWith("windows")) "angel-lsp.exe" else "angel-lsp"
            executable.copyTo(out.resolve("$platform/$target"))
        }
        if (out.list().isNullOrEmpty()) throw GradleException("No server executable in $source")
        val serverNotices = source.resolve("THIRD-PARTY-NOTICES.txt")
        if (!serverNotices.isFile) throw GradleException("Server notices not found: $serverNotices")
        val text = serverNotices.readText().replace("\r\n", "\n")
        for (required in listOf("angel-lsp ${serverVersion.get()}", "Node.js v${nodeVersion.get()}")) {
            if (required !in text) throw GradleException("$serverNotices does not name $required")
        }
        out.resolve("THIRD-PARTY-NOTICES.txt").writeText(pluginNotices.get().asFile.readText() + "\n" + text)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(file.inputStream(), digest).use { it.copyTo(OutputStream.nullOutputStream()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

val prepareServer =
    tasks.register<PrepareServerTask>("prepareServer") {
        description = "Collects the language server executables and their notices, and prints their SHA-256."
        executables = serverExecutables
        serverDirectory = providers.gradleProperty("angelLspServerDir").map { layout.projectDirectory.dir(it) }
        requireAllPlatforms = providers.gradleProperty("angelLspAllPlatforms").map(String::toBoolean).orElse(false)
        pluginNotices = layout.projectDirectory.file("server/plugin-notices.txt")
        serverVersion = libs.versions.angelLsp
        nodeVersion = libs.versions.nodeRuntime
        outputDirectory = layout.buildDirectory.dir("server")
    }

val updateNotices =
    tasks.register("updateNotices") {
        description = "Rewrites THIRD-PARTY-NOTICES.txt from the notices shipped in the plugin."
        val shipped = prepareServer.flatMap { it.outputDirectory.file("THIRD-PARTY-NOTICES.txt") }
        val root = layout.projectDirectory.file("THIRD-PARTY-NOTICES.txt").asFile
        inputs.file(shipped)
        outputs.file(root)
        doLast { root.writeText(shipped.get().asFile.readText()) }
    }

val checkNotices =
    tasks.register("checkNotices") {
        description = "Checks that THIRD-PARTY-NOTICES.txt matches the notices shipped in the plugin."
        group = "verification"
        mustRunAfter(updateNotices)
        val shipped = prepareServer.flatMap { it.outputDirectory.file("THIRD-PARTY-NOTICES.txt") }
        val root = layout.projectDirectory.file("THIRD-PARTY-NOTICES.txt").asFile
        inputs.file(shipped)
        inputs.files(root)
        doLast {
            if (!root.isFile || root.readText() != shipped.get().asFile.readText()) {
                throw GradleException("THIRD-PARTY-NOTICES.txt is out of date. Run ./gradlew updateNotices.")
            }
        }
    }

val executableServers = serverExecutables.keys.filterNot { it.startsWith("windows") }.map { "**/server/$it/angel-lsp" }

tasks.prepareSandbox {
    from(prepareServer) {
        into(intellijPlatform.projectName.map { "$it/server" })
    }
    filesMatching(executableServers) { permissions { unix("755") } }
}

tasks.buildPlugin {
    dependsOn(checkNotices)
    filesMatching(executableServers) { permissions { unix("755") } }
}

tasks.check {
    dependsOn(checkNotices)
}

// The release workflow signs and publishes the ZIP its build job verified, passed as -PreleaseArchive.
val releaseArchive = providers.gradleProperty("releaseArchive").orNull?.let(layout.projectDirectory::file)
if (releaseArchive != null) {
    tasks.signPlugin { archiveFile = releaseArchive }
    tasks.publishPlugin { archiveFiles.setFrom(releaseArchive) }
}

tasks.test {
    systemProperty("angelscript.testData", layout.projectDirectory.dir("src/test/testData").asFile.path)
}
