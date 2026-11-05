package dev.plumfire.angelscript.lang

import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EditorFeaturesTest : BasePlatformTestCase() {
    fun testFileTypes() {
        val fileTypes = FileTypeManager.getInstance()
        assertEquals(AngelScriptFileType, fileTypes.getFileTypeByFileName("main.as"))
        assertEquals(AngelScriptPredefinedFileType, fileTypes.getFileTypeByFileName("as.predefined"))
        assertEquals(AngelScriptPredefinedFileType, fileTypes.getFileTypeByFileName("engine.as.predefined"))
    }

    fun testPsiIsFlat() {
        val text = "[Editable]\nclass A { int x = 1; } // c"
        val file = myFixture.configureByText("a.as", text)
        assertEquals(text, file.text)
        assertTrue(file.children.all { it.firstChild == null })
    }

    fun testBracesMatch() {
        myFixture.configureByText("a.as", "void f() <caret>{ a[(1)]; }")
        val file = myFixture.file
        val editor = myFixture.editor
        assertEquals(19, BraceMatchingUtil.getMatchedBraceOffset(editor, true, file))
        editor.caretModel.moveToOffset(12)
        assertEquals(16, BraceMatchingUtil.getMatchedBraceOffset(editor, true, file))
    }

    fun testLineComment() {
        myFixture.configureByText("a.as", "<caret>int x;")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("//int x;")
    }

    fun testBlockComment() {
        myFixture.configureByText("a.as", "int <selection>x</selection>;")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        myFixture.checkResult("int /*x*/;")
    }

    fun testQuotesPairAndStepOver() {
        myFixture.configureByText("a.as", "string s = <caret>;")
        myFixture.type('"')
        myFixture.checkResult("string s = \"<caret>\";")
        myFixture.type("a\"")
        myFixture.checkResult("string s = \"a\"<caret>;")
        myFixture.type(" + '")
        myFixture.checkResult("string s = \"a\" + '<caret>';")
    }

    fun testEnterAfterBraceIndents() {
        myFixture.configureByText("a.as", "    void f() {<caret>")
        myFixture.type('\n')
        myFixture.type("x;")
        myFixture.checkResult("    void f() {\n        x;<caret>\n    }")
    }

    fun testEnterKeepsIndent() {
        myFixture.configureByText("a.as", "{\n    x;<caret>")
        myFixture.type('\n')
        myFixture.checkResult("{\n    x;\n    <caret>")
    }

    fun testClosingBraceOutdents() {
        myFixture.configureByText("a.as", "  if (a) {\n      x;\n      <caret>")
        myFixture.type('}')
        myFixture.checkResult("  if (a) {\n      x;\n  }<caret>")
    }
}
