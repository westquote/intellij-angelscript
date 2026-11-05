package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class SemanticTokenPainterTest : BasePlatformTestCase() {
    // A cancelled scope: tracking a file never reaches the language server.
    private val painter by lazy { SemanticTokenPainter(project, CoroutineScope(Job().apply { cancel() })) }
    private lateinit var file: VirtualFile
    private lateinit var document: Document

    override fun setUp() {
        super.setUp()
        file = myFixture.configureByText("main.as", "int count = 1;").virtualFile
        document = myFixture.editor.document
    }

    override fun tearDown() {
        try {
            painter.dispose()
        } finally {
            super.tearDown()
        }
    }

    fun testRepaintKeepsUnchangedHighlighters() {
        painter.track(file)
        paint(TokenSpan(0, 3, AngelScriptColors.TYPE), TokenSpan(4, 9, AngelScriptColors.LOCAL_VARIABLE))
        val kept = painted().single { it.startOffset == 4 }
        paint(TokenSpan(4, 9, AngelScriptColors.LOCAL_VARIABLE), TokenSpan(12, 13, AngelScriptColors.FIELD))
        assertSame(kept, painted().single { it.startOffset == 4 })
        assertEquals(listOf(4, 12), painted().map { it.startOffset }.sorted())
    }

    fun testTokensForAnOlderDocumentAreDiscarded() {
        painter.track(file)
        val stamp = document.modificationStamp
        myFixture.type("// ")
        painter.paint(document, stamp, listOf(TokenSpan(0, 3, AngelScriptColors.TYPE)))
        assertEmpty(painted())
    }

    fun testUntrackedDocumentsAreNotPainted() {
        paint(TokenSpan(0, 3, AngelScriptColors.TYPE))
        assertEmpty(painted())
    }

    fun testUntrackRemovesHighlighters() {
        painter.track(file)
        paint(TokenSpan(0, 3, AngelScriptColors.TYPE))
        painter.untrack(file)
        assertEmpty(painted())
    }

    fun testDisposeRemovesHighlighters() {
        painter.track(file)
        paint(TokenSpan(0, 3, AngelScriptColors.TYPE))
        painter.dispose()
        assertEmpty(painted())
    }

    private fun paint(vararg spans: TokenSpan) = painter.paint(document, document.modificationStamp, spans.toList())

    private fun painted(): List<RangeHighlighter> = DocumentMarkupModel
        .forDocument(document, project, true)
        .allHighlighters
        .filter { it.layer == SemanticTokenPainter.LAYER }
}
