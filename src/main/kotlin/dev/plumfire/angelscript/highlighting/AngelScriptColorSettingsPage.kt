package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import dev.plumfire.angelscript.AngelScriptBundle.message
import dev.plumfire.angelscript.lang.AngelScriptFileType
import dev.plumfire.angelscript.lang.AngelScriptLexer
import javax.swing.Icon

/** Settings | Editor | Color Scheme | AngelScript. The demo marks semantic keys with tags named after them. */
internal class AngelScriptColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "AngelScript"

    override fun getIcon(): Icon = AngelScriptFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = AngelScriptSyntaxHighlighter { AngelScriptLexer() }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> =
        AngelScriptColors.ALL.map { AttributesDescriptor(message("color.${tagOf(it)}"), it) }.toTypedArray()

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        AngelScriptColors.ALL.associateBy(::tagOf)

    override fun getDemoText(): String = javaClass.getResource("/colorSettingsDemo.as")!!.readText()

    private fun tagOf(key: TextAttributesKey): String = key.externalName.removePrefix("ANGELSCRIPT_")
}
