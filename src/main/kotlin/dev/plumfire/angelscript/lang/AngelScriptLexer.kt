package dev.plumfire.angelscript.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import dev.plumfire.angelscript.lang.AngelScriptTokenTypes as T

/**
 * Hand-written lexer for AngelScript.
 *
 * The state at a token start holds two flags: whether the token is the first significant one on its line, and
 * whether the previous significant token allows metadata to follow. State 0 is the start of a file.
 */
internal class AngelScriptLexer(
    private val digitSeparators: Boolean = false,
    private val unicodeIdentifiers: Boolean = false,
) : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null
    private var state = 0
    private var nextState = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenEnd = startOffset
        nextState = initialState
        advance()
    }

    override fun getState(): Int = state

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        tokenStart = tokenEnd
        state = nextState
        if (tokenStart >= endOffset) {
            tokenType = null
            return
        }
        val atLineStart = state and NOT_LINE_START == 0
        val metadataAllowed = atLineStart || state and NO_METADATA == 0
        val type = lex(tokenStart, atLineStart, metadataAllowed)
        tokenType = type
        nextState = when (type) {
            T.WHITE_SPACE -> if (hasLineBreak(tokenStart, tokenEnd)) state and NOT_LINE_START.inv() else state
            T.LINE_COMMENT, T.BLOCK_COMMENT -> state
            T.SEMICOLON, T.LBRACE, T.RBRACE, T.METADATA -> NOT_LINE_START
            else -> NOT_LINE_START or NO_METADATA
        }
    }

    private fun lex(start: Int, atLineStart: Boolean, metadataAllowed: Boolean): IElementType {
        val c = buffer[start]
        val next = charAt(start + 1)
        return when {
            isWhitespace(c) -> token(skipWhile(start) { isWhitespace(it) }, T.WHITE_SPACE)
            c == '/' && next == '/' -> token(lineEnd(start), T.LINE_COMMENT)
            c == '/' && next == '*' -> token(delimitedEnd(start, "/*", "*/"), T.BLOCK_COMMENT)
            c == '"' && isAt(start, HEREDOC) -> token(delimitedEnd(start, HEREDOC, HEREDOC), T.HEREDOC)
            c == '"' || c == '\'' -> token(stringEnd(start, c), T.STRING)
            c.isAsciiDigit() || c == '.' && next.isAsciiDigit() -> token(numberEnd(start), T.NUMBER)
            isIdentifierStart(c) -> identifier(start)
            c == '#' && atLineStart -> token(preprocessorEnd(start), T.PREPROCESSOR)
            c == '[' && metadataAllowed -> metadataEnd(start)?.let { token(it, T.METADATA) }
                ?: token(start + 1, T.LBRACKET)
            else -> operatorOrPunctuation(start, c)
        }
    }

    private fun token(end: Int, type: IElementType): IElementType {
        tokenEnd = end
        return type
    }

    private fun charAt(offset: Int): Char = if (offset < endOffset) buffer[offset] else '\u0000'

    private inline fun skipWhile(start: Int, predicate: (Char) -> Boolean): Int {
        var i = start
        while (i < endOffset && predicate(buffer[i])) i++
        return i
    }

    private fun hasLineBreak(start: Int, end: Int): Boolean {
        for (i in start until end) if (buffer[i] == '\n' || buffer[i] == '\r') return true
        return false
    }

    private fun lineEnd(start: Int): Int = skipWhile(start) { it != '\n' && it != '\r' }

    private fun isAt(offset: Int, text: String): Boolean {
        if (offset + text.length > endOffset) return false
        for (i in 0 until text.length) if (buffer[offset + i] != text[i]) return false
        return true
    }

    /** The end of a token that opens with [delimiter] and closes with [closing], or the input end if unclosed. */
    private fun delimitedEnd(start: Int, delimiter: String, closing: String): Int {
        var i = start + delimiter.length
        while (i < endOffset && !isAt(i, closing)) i++
        return minOf(i + closing.length, endOffset)
    }

    private fun stringEnd(start: Int, quote: Char): Int {
        var i = start + 1
        while (i < endOffset) {
            when (buffer[i]) {
                quote -> return i + 1
                '\n', '\r' -> return i
                '\\' -> if (i + 1 < endOffset && buffer[i + 1] != '\n' && buffer[i + 1] != '\r') i++
            }
            i++
        }
        return endOffset
    }

    private fun numberEnd(start: Int): Int {
        val prefix = charAt(start + 1).lowercaseChar()
        if (buffer[start] == '0' && prefix in RADIX_PREFIXES) {
            val radixDigit = RADIX_PREFIXES.getValue(prefix)
            if (radixDigit(charAt(start + 2))) return digitsEnd(start + 2, radixDigit)
        }
        var i = digitsEnd(start, Char::isAsciiDigit)
        if (charAt(i) == '.' && charAt(i + 1) != '.') i = digitsEnd(i + 1, Char::isAsciiDigit)
        if (charAt(i) == 'e' || charAt(i) == 'E') {
            val sign = if (charAt(i + 1) == '+' || charAt(i + 1) == '-') 1 else 0
            if (charAt(i + 1 + sign).isAsciiDigit()) i = digitsEnd(i + 1 + sign, Char::isAsciiDigit)
        }
        if (charAt(i) == 'f' || charAt(i) == 'F') i++
        return i
    }

    private fun digitsEnd(start: Int, isDigit: (Char) -> Boolean): Int {
        var i = start
        while (i < endOffset) {
            val c = buffer[i]
            val separator =
                digitSeparators && c == '\'' && i > start && isDigit(buffer[i - 1]) && isDigit(charAt(i + 1))
            if (!isDigit(c) && !separator) break
            i++
        }
        return i
    }

    private fun identifier(start: Int): IElementType {
        val end = skipWhile(start + 1) { isIdentifierPart(it) }
        val first = buffer[start]
        if (first !in 'a'..'z') return token(end, T.IDENTIFIER)
        val length = end - start
        val candidates = KEYWORDS_BY_LETTER[first - 'a']
        val keyword = candidates.firstOrNull { (word, _) -> word.length == length && isAt(start, word) }
        return token(end, keyword?.second ?: T.IDENTIFIER)
    }

    private fun preprocessorEnd(start: Int): Int {
        var i = start
        while (true) {
            i = lineEnd(i)
            var j = i - 1
            while (j > start && (buffer[j] == ' ' || buffer[j] == '\t')) j--
            if (i >= endOffset || buffer[j] != '\\') return i
            i += if (buffer[i] == '\r' && charAt(i + 1) == '\n') 2 else 1
        }
    }

    /** The end of a metadata block opened at [start], or null when its `]` is not on the same line. */
    private fun metadataEnd(start: Int): Int? {
        var depth = 0
        var i = start
        while (i < endOffset) {
            when (val c = buffer[i]) {
                '\n', '\r' -> return null
                '[' -> depth++
                ']' -> if (--depth == 0) return i + 1
                '"', '\'' -> {
                    i = stringEnd(i, c)
                    continue
                }
            }
            i++
        }
        return null
    }

    private fun operatorOrPunctuation(start: Int, c: Char): IElementType {
        val operator = AngelScriptKeywords.OPERATORS.firstOrNull { it[0] == c && isAt(start, it) }
        if (operator != null) return token(start + operator.length, T.OPERATOR)
        return token(start + 1, T.PUNCTUATION[c] ?: T.BAD_CHARACTER)
    }

    private fun isWhitespace(c: Char): Boolean =
        c == ' ' || c == '\t' || c == '\n' || c == '\r' || c.code == BYTE_ORDER_MARK

    private fun isIdentifierStart(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c == '_' || unicodeIdentifiers && c.code >= NON_ASCII && c.isLetter()

    private fun isIdentifierPart(c: Char): Boolean =
        isIdentifierStart(c) || c.isAsciiDigit() || unicodeIdentifiers && c.code >= NON_ASCII && c.isLetterOrDigit()

    private companion object {
        const val NOT_LINE_START = 1
        const val NO_METADATA = 2
        const val BYTE_ORDER_MARK = 0xFEFF
        const val NON_ASCII = 0x80
        const val HEREDOC = "\"\"\""

        // Keywords grouped by their first letter, so an identifier is compared in place without creating a string.
        val KEYWORDS_BY_LETTER: Array<List<Pair<String, IElementType>>> = run {
            val control = AngelScriptKeywords.CONTROL.map { it to T.CONTROL_KEYWORD }
            val byLetter = (control + AngelScriptKeywords.KEYWORDS.map { it to T.KEYWORD }).groupBy { it.first[0] }
            Array('z' - 'a' + 1) { byLetter['a' + it].orEmpty() }
        }

        val RADIX_PREFIXES: Map<Char, (Char) -> Boolean> = mapOf(
            'x' to { c -> c.isAsciiDigit() || c.lowercaseChar() in 'a'..'f' },
            'b' to { c -> c == '0' || c == '1' },
            'o' to { c -> c in '0'..'7' },
            'd' to { c -> c.isAsciiDigit() },
        )
    }
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
