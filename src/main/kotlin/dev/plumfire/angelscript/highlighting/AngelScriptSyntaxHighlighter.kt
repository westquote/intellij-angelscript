package dev.plumfire.angelscript.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import dev.plumfire.angelscript.settings.AngelScriptSettingsService
import dev.plumfire.angelscript.lang.AngelScriptTokenTypes as T

internal class AngelScriptSyntaxHighlighter(private val lexerFactory: () -> Lexer) : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = lexerFactory()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = pack(KEYS[tokenType])

    companion object {
        val KEYS: Map<IElementType, TextAttributesKey> = mapOf(
            T.KEYWORD to AngelScriptColors.KEYWORD,
            T.CONTROL_KEYWORD to AngelScriptColors.CONTROL_KEYWORD,
            T.NUMBER to AngelScriptColors.NUMBER,
            T.STRING to AngelScriptColors.STRING,
            T.HEREDOC to AngelScriptColors.STRING,
            T.LINE_COMMENT to AngelScriptColors.LINE_COMMENT,
            T.BLOCK_COMMENT to AngelScriptColors.BLOCK_COMMENT,
            T.OPERATOR to AngelScriptColors.OPERATOR,
            T.LBRACE to AngelScriptColors.BRACES,
            T.RBRACE to AngelScriptColors.BRACES,
            T.LPAREN to AngelScriptColors.PARENTHESES,
            T.RPAREN to AngelScriptColors.PARENTHESES,
            T.LBRACKET to AngelScriptColors.BRACKETS,
            T.RBRACKET to AngelScriptColors.BRACKETS,
            T.SEMICOLON to AngelScriptColors.SEMICOLON,
            T.COMMA to AngelScriptColors.COMMA,
            T.DOT to AngelScriptColors.DOT,
            T.METADATA to AngelScriptColors.METADATA,
            T.PREPROCESSOR to AngelScriptColors.PREPROCESSOR,
            T.IDENTIFIER to AngelScriptColors.IDENTIFIER,
            T.BAD_CHARACTER to AngelScriptColors.BAD_CHARACTER,
        )
    }
}

internal class AngelScriptSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        AngelScriptSyntaxHighlighter { AngelScriptSettingsService.createLexer(project) }
}
