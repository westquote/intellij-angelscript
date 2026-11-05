package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.eclipse.lsp4j.SemanticTokensLegend
import java.awt.Color

class SemanticTokensTest : BasePlatformTestCase() {
    // The legend angel-lsp 0.3.103 sends in its initialize response.
    private val types =
        (
            " namespace class enum interface struct typeParameter type parameter variable property enumMember " +
                "decorator event function method macro label comment string keyword number regexp operator controlKeyword"
            ).split(' ')
    private val modifiers =
        " declaration definition readonly static deprecated abstract async modification documentation defaultLibrary"
            .split(' ')

    private fun decode(data: List<Int>, text: String): List<TokenSpan> =
        SemanticTokens.decode(data, SemanticTokensLegend(types, modifiers), DocumentImpl(text))

    private fun type(name: String) = types.indexOf(name)

    fun testDecodesRelativePositionsOverSeveralLines() {
        val text = "class Circle\n{\n    float m_Radius;\n}"
        val data = listOf(0, 6, 6, type("class"), 0, 2, 10, 8, type("property"), 0)
        assertEquals(
            listOf(TokenSpan(6, 12, AngelScriptColors.CLASS), TokenSpan(25, 33, AngelScriptColors.FIELD)),
            decode(data, text),
        )
    }

    fun testStartsOnTheSameLineAreRelative() {
        val data = listOf(0, 0, 1, type("variable"), 0, 0, 2, 1, type("parameter"), 0)
        assertEquals(
            listOf(TokenSpan(0, 1, AngelScriptColors.LOCAL_VARIABLE), TokenSpan(2, 3, AngelScriptColors.PARAMETER)),
            decode(data, "a b"),
        )
    }

    fun testDropsTokensOutsideTheDocumentAndUnpaintedTypes() {
        val data =
            listOf(
                0,
                0,
                2,
                type("comment"),
                0,
                0,
                0,
                50,
                type("variable"),
                0,
                5,
                0,
                1,
                type("variable"),
                0,
                0,
                0,
                1,
                99,
                0,
            )
        assertEmpty(decode(data, "// x\ny"))
    }

    fun testDeprecatedAddsASecondSpan() {
        val data = listOf(0, 0, 3, type("function"), 1 shl modifiers.indexOf("deprecated"))
        assertEquals(
            listOf(
                TokenSpan(0, 3, AngelScriptColors.FUNCTION_CALL),
                TokenSpan(0, 3, CodeInsightColors.DEPRECATED_ATTRIBUTES),
            ),
            decode(data, "Old()"),
        )
    }

    fun testEveryLegendTypeIsMappedOrDeliberatelyUnpainted() {
        val lexerOwned = setOf("", "comment", "string", "number", "operator", "regexp")
        val neverSent = setOf("struct", "decorator", "event", "label")
        for (type in types) {
            if (type in lexerOwned || type in neverSent) {
                assertNull(type, SemanticTokenKeys.keyFor(type))
            } else {
                assertNotNull(type, SemanticTokenKeys.keyFor(type))
            }
        }
        assertEquals(AngelScriptColors.KEYWORD, SemanticTokenKeys.keyFor("keyword"))
        assertEquals(AngelScriptColors.CONTROL_KEYWORD, SemanticTokenKeys.keyFor("controlKeyword"))
    }

    fun testDiffKeepsUnchangedSpansAndReplacesTheRest() {
        val a = TokenSpan(0, 3, AngelScriptColors.CLASS)
        val b = TokenSpan(4, 7, AngelScriptColors.FIELD)
        val c = TokenSpan(8, 9, AngelScriptColors.PARAMETER)
        val plan = SemanticTokens.diff(listOf(a to "A", b to "B", null to "INVALID"), listOf(a, c))
        assertEquals(listOf("A"), plan.keep)
        assertEquals(listOf("B", "INVALID"), plan.remove)
        assertEquals(listOf(c), plan.add)
    }

    fun testPresentCppKeyWinsAndAbsentOneDoesNot() {
        val scheme = EditorColorsManager.getInstance().globalScheme.clone() as EditorColorsScheme
        val cppName = "ReSharper.CPP_CLASS_IDENTIFIER"
        val hasAttributes = { key: TextAttributesKey -> scheme.getAttributes(key)?.isEmpty == false }
        val platformKey = DefaultLanguageHighlighterColors.CLASS_NAME
        assertEquals(platformKey, AngelScriptColors.fallback(cppName, platformKey, hasAttributes))
        scheme.setAttributes(TextAttributesKey.find(cppName), TextAttributes(Color.RED, null, null, null, 0))
        assertEquals(cppName, AngelScriptColors.fallback(cppName, platformKey, hasAttributes).externalName)
    }

    fun testKeysWithoutCppAttributesUseThePlatformKey() {
        assertEquals(DefaultLanguageHighlighterColors.CLASS_NAME, AngelScriptColors.CLASS.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.KEYWORD, AngelScriptColors.KEYWORD.fallbackAttributeKey)
    }

    fun testColorPageShowsEveryKeyAndItsDemoUsesKnownTags() {
        val page = AngelScriptColorSettingsPage()
        assertEquals(AngelScriptColors.ALL, page.attributeDescriptors.map { it.key })
        val tags = Regex("</?(\\w+)>").findAll(page.demoText).map { it.groupValues[1] }.toSet()
        assertContainsElements(page.additionalHighlightingTagToDescriptorMap.keys, tags)
        val semantic =
            "NAMESPACE CLASS INTERFACE ENUM ENUM_MEMBER TYPE TYPE_PARAMETER PARAMETER LOCAL_VARIABLE FIELD " +
                "FUNCTION_CALL METHOD_CALL MACRO"
        assertContainsElements(tags, semantic.split(' '))
    }
}
