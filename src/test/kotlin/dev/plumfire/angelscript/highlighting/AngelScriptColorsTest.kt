package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AngelScriptColorsTest : BasePlatformTestCase() {
    private val platformKey = DefaultLanguageHighlighterColors.CLASS_NAME

    fun testFallsBackToTheCppKeyWithCppColors() {
        val key = AngelScriptColors.fallback("CLASS_IDENTIFIER", platformKey, cppColors = true)
        assertEquals("ReSharper.CPP_CLASS_IDENTIFIER", key.externalName)
    }

    fun testFallsBackToThePlatformKeyWithoutCppColorsOrACppCounterpart() {
        assertSame(platformKey, AngelScriptColors.fallback("CLASS_IDENTIFIER", platformKey, cppColors = false))
        assertSame(platformKey, AngelScriptColors.fallback(null, platformKey, cppColors = true))
    }

    fun testKeysUsePlatformFallbacksOutsideRider() {
        assertSame(platformKey, AngelScriptColors.CLASS.fallbackAttributeKey)
        assertSame(DefaultLanguageHighlighterColors.KEYWORD, AngelScriptColors.KEYWORD.fallbackAttributeKey)
    }
}
