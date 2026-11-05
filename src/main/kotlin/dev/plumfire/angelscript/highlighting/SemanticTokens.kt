package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import org.eclipse.lsp4j.SemanticTokensLegend

/** A range of a document painted with [key]. */
internal data class TokenSpan(val start: Int, val end: Int, val key: TextAttributesKey)

/** The result of [SemanticTokens.diff]: highlighters to remove and keep, and spans to add. */
internal class PaintPlan<H>(val remove: List<H>, val keep: List<H>, val add: List<TokenSpan>)

internal object SemanticTokens {
    private const val TOKEN_SIZE = 5

    /**
     * Turns LSP relative token data (delta line, delta start, length, type, modifier bits) into spans of
     * [document]. Tokens that leave their line and token types that are not painted are dropped.
     */
    fun decode(data: List<Int>, legend: SemanticTokensLegend, document: Document): List<TokenSpan> {
        val deprecated = legend.tokenModifiers.indexOf(SemanticTokenKeys.DEPRECATED)
        val spans = ArrayList<TokenSpan>(data.size / TOKEN_SIZE)
        var line = 0
        var column = 0
        var i = 0
        while (i + TOKEN_SIZE <= data.size) {
            val deltaLine = data[i++]
            val deltaStart = data[i++]
            val length = data[i++]
            val type = data[i++]
            val modifiers = data[i++]
            line += deltaLine
            column = if (deltaLine == 0) column + deltaStart else deltaStart
            val key = legend.tokenTypes.getOrNull(type)?.let(SemanticTokenKeys::keyFor)
            val start = document.offsetOf(line, column, length)
            if (key == null || start == null) continue
            spans += TokenSpan(start, start + length, key)
            if (deprecated >= 0 && modifiers and (1 shl deprecated) != 0) {
                spans += TokenSpan(start, start + length, CodeInsightColors.DEPRECATED_ATTRIBUTES)
            }
        }
        return spans
    }

    private fun Document.offsetOf(line: Int, column: Int, length: Int): Int? {
        if (line >= lineCount || column < 0 || length <= 0) return null
        val start = getLineStartOffset(line) + column
        return start.takeIf { it + length <= getLineEndOffset(line) }
    }

    /** Compares painted highlighters with the wanted spans; unchanged (range, key) pairs are kept. */
    fun <H> diff(painted: List<Pair<TokenSpan?, H>>, wanted: List<TokenSpan>): PaintPlan<H> {
        val missing = LinkedHashSet(wanted)
        val remove = ArrayList<H>()
        val keep = ArrayList<H>()
        for ((span, highlighter) in painted) {
            if (span != null && missing.remove(span)) keep += highlighter else remove += highlighter
        }
        return PaintPlan(remove, keep, missing.toList())
    }
}
