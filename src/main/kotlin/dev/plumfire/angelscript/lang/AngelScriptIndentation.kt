package dev.plumfire.angelscript.lang

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider

/**
 * Indents a new line after `{` one level deeper and a line starting with `}` to the level of its `{`, without a
 * formatter that would compete with LSP formatting.
 */
internal class AngelScriptLineIndentProvider : LineIndentProvider {
    override fun isSuitableFor(language: Language?): Boolean = language is AngelScriptLanguage

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        val document = editor.document
        val text = document.charsSequence
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
        val firstChar = indentEnd(text, lineStart)
        if (firstChar < text.length && text[firstChar] == '}') {
            return matchingOpenBrace(editor, firstChar)?.let { indentOf(document, document.getLineNumber(it)) }
        }
        var previous = lineStart - 1
        while (previous >= 0 && text[previous].isWhitespace()) previous--
        if (previous < 0) return ""
        val indent = indentOf(document, document.getLineNumber(previous))
        return if (text[previous] == '{') indent + indentUnit(project, document) else indent
    }

    private fun indentUnit(project: Project, document: Document): String {
        val options = CodeStyle.getIndentOptions(project, document)
        return if (options.USE_TAB_CHARACTER) "\t" else " ".repeat(options.INDENT_SIZE)
    }

    private fun indentEnd(text: CharSequence, start: Int): Int {
        var end = start
        while (end < text.length && (text[end] == ' ' || text[end] == '\t')) end++
        return end
    }

    private fun indentOf(document: Document, line: Int): String {
        val start = document.getLineStartOffset(line)
        return document.charsSequence.subSequence(start, indentEnd(document.charsSequence, start)).toString()
    }

    private fun matchingOpenBrace(editor: Editor, closeOffset: Int): Int? {
        val iterator = (editor as? EditorEx)?.highlighter?.createIterator(closeOffset) ?: return null
        var depth = 0
        while (!iterator.atEnd()) {
            when (iterator.tokenType) {
                AngelScriptTokenTypes.RBRACE -> depth++
                AngelScriptTokenTypes.LBRACE -> if (--depth == 0) return iterator.start
            }
            iterator.retreat()
        }
        return null
    }
}

/** Re-indents a `}` typed as the first character of its line; the platform does this only through a formatter. */
internal class AngelScriptBraceTypedHandler : TypedHandlerDelegate() {
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != '}' || file.language !is AngelScriptLanguage) return Result.CONTINUE
        val document = editor.document
        val brace = editor.caretModel.offset - 1
        val lineStart = document.getLineStartOffset(document.getLineNumber(brace))
        if (document.charsSequence.subSequence(lineStart, brace).isNotBlank()) return Result.CONTINUE
        val indent = AngelScriptLineIndentProvider().getLineIndent(project, editor, file.language, brace)
        if (indent != null) document.replaceString(lineStart, brace, indent)
        return Result.CONTINUE
    }
}
