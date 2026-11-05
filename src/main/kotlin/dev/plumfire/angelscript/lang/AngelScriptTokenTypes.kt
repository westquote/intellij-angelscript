package dev.plumfire.angelscript.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

internal class AngelScriptTokenType(debugName: String) : IElementType(debugName, AngelScriptLanguage)

internal object AngelScriptTokenTypes {
    @JvmField val LINE_COMMENT = AngelScriptTokenType("LINE_COMMENT")

    @JvmField val BLOCK_COMMENT = AngelScriptTokenType("BLOCK_COMMENT")

    @JvmField val STRING = AngelScriptTokenType("STRING")

    @JvmField val HEREDOC = AngelScriptTokenType("HEREDOC")

    @JvmField val NUMBER = AngelScriptTokenType("NUMBER")

    @JvmField val KEYWORD = AngelScriptTokenType("KEYWORD")

    @JvmField val CONTROL_KEYWORD = AngelScriptTokenType("CONTROL_KEYWORD")

    @JvmField val IDENTIFIER = AngelScriptTokenType("IDENTIFIER")

    @JvmField val OPERATOR = AngelScriptTokenType("OPERATOR")

    @JvmField val METADATA = AngelScriptTokenType("METADATA")

    @JvmField val PREPROCESSOR = AngelScriptTokenType("PREPROCESSOR")

    @JvmField val LPAREN = AngelScriptTokenType("(")

    @JvmField val RPAREN = AngelScriptTokenType(")")

    @JvmField val LBRACE = AngelScriptTokenType("{")

    @JvmField val RBRACE = AngelScriptTokenType("}")

    @JvmField val LBRACKET = AngelScriptTokenType("[")

    @JvmField val RBRACKET = AngelScriptTokenType("]")

    @JvmField val SEMICOLON = AngelScriptTokenType(";")

    @JvmField val COMMA = AngelScriptTokenType(",")

    @JvmField val DOT = AngelScriptTokenType(".")

    @JvmField val COLON = AngelScriptTokenType(":")

    @JvmField val WHITE_SPACE: IElementType = TokenType.WHITE_SPACE

    @JvmField val BAD_CHARACTER: IElementType = TokenType.BAD_CHARACTER

    @JvmField val COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)

    @JvmField val STRINGS = TokenSet.create(STRING, HEREDOC)

    val PUNCTUATION: Map<Char, IElementType> = mapOf(
        '(' to LPAREN,
        ')' to RPAREN,
        '{' to LBRACE,
        '}' to RBRACE,
        '[' to LBRACKET,
        ']' to RBRACKET,
        ';' to SEMICOLON,
        ',' to COMMA,
        '.' to DOT,
        ':' to COLON,
    )
}
