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

    override fun getDescription(): String = AngelScriptBundle.message("filetype.predefined.description")

    override fun getDefaultExtension(): String = "predefined"

    override fun getIcon(): Icon = fileIcon
}

internal fun VirtualFile.isAngelScript(): Boolean = (fileType as? LanguageFileType)?.language == AngelScriptLanguage
