package dev.plumfire.angelscript.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Color

class AngelScriptColorsTest : BasePlatformTestCase() {
    fun testEveryKeyFallsBackToAPlatformKey() {
        for (key in AngelScriptColors.ALL) {
            val fallback = key.fallbackAttributeKey
            assertNotNull(key.externalName, fallback)
            assertFalse(key.externalName, fallback!!.externalName.startsWith("ReSharper."))
        }
        assertSame(DefaultLanguageHighlighterColors.CLASS_NAME, AngelScriptColors.CLASS.fallbackAttributeKey)
    }

    // Rider's own schemes define C++ keys such as this one only by inheritance, as below.
    fun testNamesTakeThePlatformColorWhenTheSchemeOnlyInheritsCppColors() {
        val scheme = EditorColorsSchemeImpl(null)
        scheme.readExternal(
            JDOMUtil.load(
                """
                <scheme name="Rider-like" version="142" parent_scheme="Darcula">
                  <attributes>
                    <option name="DEFAULT_CLASS_NAME"><value><option name="FOREGROUND" value="C191FF"/></value></option>
                    <option name="ReSharper.CPP_CLASS_IDENTIFIER" baseAttributes="DEFAULT_CLASS_NAME"/>
                  </attributes>
                </scheme>
                """.trimIndent(),
            ),
        )
        assertEquals(Color(0xC191FF), scheme.getAttributes(AngelScriptColors.CLASS).foregroundColor)
    }
}
