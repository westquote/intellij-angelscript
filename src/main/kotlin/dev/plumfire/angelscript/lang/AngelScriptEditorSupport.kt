package dev.plumfire.angelscript.lang

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import dev.plumfire.angelscript.lang.AngelScriptTokenTypes as T

internal class AngelScriptBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(T.LPAREN, T.RPAREN, false),
        BracePair(T.LBRACKET, T.RBRACKET, false),
        BracePair(T.LBRACE, T.RBRACE, true),
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}

internal class AngelScriptCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"

    override fun getBlockCommentPrefix(): String = "/*"

    override fun getBlockCommentSuffix(): String = "*/"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}

internal class AngelScriptQuoteHandler : SimpleTokenSetQuoteHandler(T.STRING)
