package dev.plumfire.angelscript

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.plumfire.angelscript.highlighting.AngelScriptColors
import dev.plumfire.angelscript.highlighting.SemanticTokens
import dev.plumfire.angelscript.highlighting.TokenSpan
import dev.plumfire.angelscript.lang.AngelScriptLexer
import org.eclipse.lsp4j.SemanticTokensLegend

/**
 * Time budgets for the code that runs on every keystroke. Each benchmark keeps the fastest attempt after warm-up,
 * which keeps the check stable on shared CI machines, and compares it with a budget several times the measured time.
 */
class PerformanceTest : BasePlatformTestCase() {
    fun testLexingTwoThousandLines() {
        val block = TestData.text("lexer/all-tokens.as")
        val text = block.repeat(2_000 / block.count { it == '\n' } + 1)
        assertTrue(text.lines().size >= 2_000)
        val lexer = AngelScriptLexer()
        benchmark("lex 2,000 lines", budgetMillis = 10) {
            lexer.start(text, 0, text.length, 0)
            while (lexer.tokenType != null) lexer.advance()
        }
    }

    fun testDecodingAndDiffingTenThousandTokens() {
        val legend = SemanticTokensLegend(listOf("", "variable", "method"), emptyList())
        val data = (0 until 10_000).flatMap { listOf(1, 4, 6, 1 + it % 2, 0) }
        val document = DocumentImpl("\n" + "    identifier;\n".repeat(10_000))
        val painted =
            SemanticTokens
                .decode(data, legend, document)
                .mapIndexed { index, span ->
                    val shifted = if (index % 10 == 0) span.copy(key = AngelScriptColors.FIELD) else span
                    shifted as TokenSpan? to index
                }
        benchmark("decode and diff 10,000 tokens", budgetMillis = 10) {
            val wanted = SemanticTokens.decode(data, legend, document)
            SemanticTokens.diff(painted, wanted)
        }
    }

    private fun benchmark(name: String, budgetMillis: Long, body: () -> Unit) {
        var fastest = Long.MAX_VALUE
        PlatformTestUtil
            .newBenchmark(name) {
                val start = System.nanoTime()
                body()
                fastest = minOf(fastest, System.nanoTime() - start)
            }.warmupIterations(20)
            .attempts(20)
            .runAsStressTest()
            .start()
        val millis = fastest / 1_000_000.0
        println("$name: %.2f ms (budget $budgetMillis ms)".format(millis))
        assertTrue("$name took $millis ms", millis < budgetMillis)
    }
}
