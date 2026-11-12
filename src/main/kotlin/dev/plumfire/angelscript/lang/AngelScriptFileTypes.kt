package dev.plumfire.angelscript.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import dev.plumfire.angelscript.AngelScriptBundle
import javax.swing.Icon

internal object AngelScriptFileType : LanguageFileType(AngelScriptLanguage) {
    private val fileIcon = IconLoader.getIcon("/icons/file.svg", AngelScriptFileType::class.java)

    override fun getName(): String = "AngelScript"

    override fun getDescription(): String = AngelScriptBundle.message("filetype.angelscript.description")

    override fun getDefaultExtension(): String = "as"

    override fun getIcon(): Icon = fileIcon
}

internal object AngelScriptPredefinedFileType : LanguageFileType(AngelScriptLanguage, true) {
    private val fileIcon = IconLoader.getIcon("/icons/predefined.svg", AngelScriptPredefinedFileType::class.java)

    override fun getName(): String = "AngelScript Predefined"

    // The default, the language's display name, is the same as the AngelScript file type's.
    override fun getDisplayName(): String = AngelScriptBundle.message("filetype.predefined.name")

    override fun getDescription(): String = AngelScriptBundle.message("filetype.predefined.description")

    override fun getDefaultExtension(): String = "predefined"

    override fun getIcon(): Icon = fileIcon
}

internal fun VirtualFile.isAngelScript(): Boolean = (fileType as? LanguageFileType)?.language == AngelScriptLanguage
