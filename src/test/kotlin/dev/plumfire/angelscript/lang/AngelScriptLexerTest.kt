package dev.plumfire.angelscript.lang

import com.intellij.psi.tree.IElementType
import dev.plumfire.angelscript.TestData
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random
import dev.plumfire.angelscript.lang.AngelScriptTokenTypes as T

class AngelScriptLexerTest {
    private fun lex(text: String, lexer: AngelScriptLexer = AngelScriptLexer()): List<Pair<IElementType, String>> {
        val tokens = mutableListOf<Pair<IElementType, String>>()
        lexer.start(text, 0, text.length, 0)
        while (lexer.tokenType != null) {
            tokens += lexer.tokenType!! to text.substring(lexer.tokenStart, lexer.tokenEnd)
            lexer.advance()
        }
        return tokens
    }

    private fun significant(text: String) = lex(text).filter { it.first != T.WHITE_SPACE }

    private fun assertSingle(type: IElementType, vararg texts: String) {
        for (text in texts) assertEquals(text, listOf(type to text), lex(text))
    }

    @Test
    fun whitespace() = assertSingle(T.WHITE_SPACE, " ", "\t\n", "\r\n  $BYTE_ORDER_MARK")

    @Test
    fun lineComment() {
        assertEquals(listOf(T.LINE_COMMENT to "// a", T.WHITE_SPACE to "\n", T.IDENTIFIER to "b"), lex("// a\nb"))
    }

    @Test
    fun blockComment() = assertSingle(T.BLOCK_COMMENT, "/* a\n b */", "/* unterminated\n", "/**/")

    @Test
    fun strings() = assertSingle(T.STRING, "\"a\\\"b\"", "'c'", "'\\''", "\"\"")

    @Test
    fun unterminatedStringEndsAtLineEnd() {
        assertEquals(listOf(T.STRING to "\"abc", T.WHITE_SPACE to "\n", T.IDENTIFIER to "x"), lex("\"abc\nx"))
    }

    @Test
    fun heredoc() = assertSingle(T.HEREDOC, "\"\"\"a\n\"b\" \\\"\"\"", "\"\"\"open\nto end")

    @Test
    fun numbers() {
        val numbers = "0 42 0xFF 0X1a 0b101 0B1 0o17 0O7 0d99 0D1 1.0 1.0f 1.5F .5 1e-3 2.5E+10 2.f 3e5f"
        assertSingle(T.NUMBER, *numbers.split(' ').toTypedArray())
    }

    @Test
    fun digitSeparatorsFollowTheSetting() {
        assertEquals(listOf(T.NUMBER to "1'000"), lex("1'000", AngelScriptLexer(digitSeparators = true)))
        assertEquals(listOf(T.NUMBER to "1", T.STRING to "'000"), lex("1'000"))
        assertEquals(listOf(T.NUMBER to "0xFF'FF"), lex("0xFF'FF", AngelScriptLexer(digitSeparators = true)))
    }

    @Test
    fun reservedKeywords() {
        for (word in AngelScriptKeywords.RESERVED - AngelScriptKeywords.CONTROL) assertSingle(T.KEYWORD, word)
    }

    @Test
    fun contextualKeywords() {
        for (word in AngelScriptKeywords.CONTEXTUAL - AngelScriptKeywords.LEXED_AS_IDENTIFIERS) {
            assertSingle(T.KEYWORD, word)
        }
    }

    @Test
    fun controlKeywords() {
        for (word in AngelScriptKeywords.CONTROL) assertSingle(T.CONTROL_KEYWORD, word)
    }

    @Test
    fun getSetAndFromAreIdentifiers() = assertSingle(T.IDENTIFIER, "get", "set", "from")

    @Test
    fun identifiers() = assertSingle(T.IDENTIFIER, "a", "_x1", "Vector3", "classy", "int128")

    @Test
    fun unicodeIdentifiersFollowTheSetting() {
        assertEquals(listOf(T.IDENTIFIER to "größe"), lex("größe", AngelScriptLexer(unicodeIdentifiers = true)))
        assertEquals(T.BAD_CHARACTER, lex("größe")[1].first)
    }

    @Test
    fun operatorsUseLongestMatch() {
        for (op in AngelScriptKeywords.OPERATORS) assertSingle(T.OPERATOR, op)
        assertEquals(listOf(T.OPERATOR to ">>>=", T.OPERATOR to ">"), lex(">>>=>"))
        assertEquals(listOf(T.OPERATOR to "**=", T.OPERATOR to "*"), lex("**=*"))
    }

    @Test
    fun punctuation() {
        for ((char, type) in T.PUNCTUATION) {
            val text = char.toString()
            assertEquals(listOf(type to text), lex(if (char == '[') "a$text" else text).takeLast(1))
        }
    }

    @Test
    fun badCharacter() = assertSingle(T.BAD_CHARACTER, "$", "`")

    @Test
    fun metadataAtLineStartAndAfterTerminators() {
        assertEquals(T.METADATA to "[Editable]", significant("[Editable] int x;")[0])
        assertEquals(T.METADATA to "[B]", significant("[A] [B] int x;")[1])
        assertEquals(T.METADATA to "[Meta(\"a]\", [1])]", significant("{ [Meta(\"a]\", [1])] }")[1])
        assertEquals(T.METADATA to "[M]", significant("x; [M]")[2])
        assertEquals(T.METADATA to "[M]", significant("}\n  /* c */ [M]")[2])
    }

    @Test
    fun bracketOtherwise() {
        assertEquals(T.LBRACKET to "[", significant("a[0] = 1;")[1])
        assertEquals(T.LBRACKET to "[", significant("[Unclosed\n]")[0])
        assertEquals(T.LBRACKET to "[", significant("f([1])")[2])
    }

    @Test
    fun preprocessorAtLineStart() {
        assertEquals(T.PREPROCESSOR to "#include \"a.as\"", significant("#include \"a.as\"\nint x;")[0])
        assertEquals(T.PREPROCESSOR to "#define A \\\n  B", significant("  #define A \\\n  B\nC")[0])
        assertEquals(T.PREPROCESSOR to "#pragma once", significant("x;\n#pragma once")[2])
        assertEquals(T.BAD_CHARACTER to "#", significant("x # y")[1])
    }

    @Test
    fun roundTripOnFixtures() {
        for (text in TestData.lexerFixtures()) assertRoundTrip(text)
    }

    @Test
    fun roundTripOnRandomInput() {
        val alphabet = "abcXYZ_09 \t\n\r\"'\\/*#[]{}();,.:<>=!&|^~@?+-%`$BYTE_ORDER_MARK${Char(0xE9)}$"
        val random = Random(20261105)
        repeat(10_000) {
            val text = String(CharArray(random.nextInt(1, 60)) { alphabet[random.nextInt(alphabet.length)] })
            assertRoundTrip(text)
            assertRestartable(text)
        }
    }

    @Test
    fun restartAtEveryTokenBoundary() {
        for (text in TestData.lexerFixtures()) assertRestartable(text)
    }

    private fun assertRoundTrip(text: String) {
        for (lexer in listOf(AngelScriptLexer(), AngelScriptLexer(digitSeparators = true, unicodeIdentifiers = true))) {
            val tokens = lex(text, lexer)
            assertEquals(text, tokens.joinToString("") { it.second })
        }
    }

    private fun assertRestartable(text: String) {
        val full = mutableListOf<Triple<Int, Int, IElementType>>()
        val states = mutableListOf<Int>()
        val lexer = AngelScriptLexer()
        lexer.start(text, 0, text.length, 0)
        while (lexer.tokenType != null) {
            full += Triple(lexer.tokenStart, lexer.tokenEnd, lexer.tokenType!!)
            states += lexer.state
            lexer.advance()
        }
        for (index in full.indices) {
            val restarted = mutableListOf<Triple<Int, Int, IElementType>>()
            lexer.start(text, full[index].first, text.length, states[index])
            while (lexer.tokenType != null) {
                restarted += Triple(lexer.tokenStart, lexer.tokenEnd, lexer.tokenType!!)
                lexer.advance()
            }
            assertEquals("restart at ${full[index].first} in ${text.take(40)}", full.drop(index), restarted)
        }
    }

    private companion object {
        val BYTE_ORDER_MARK = Char(0xFEFF)
    }
}
