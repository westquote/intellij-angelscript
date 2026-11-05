package dev.plumfire.angelscript.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import dev.plumfire.angelscript.settings.AngelScriptSettingsService

internal class AngelScriptFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AngelScriptLanguage) {
    override fun getFileType(): FileType = viewProvider.fileType
}

/** A flat PSI: every token is a leaf directly under the file node. */
internal class AngelScriptParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = AngelScriptSettingsService.createLexer(project)

    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        marker.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = AngelScriptTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = AngelScriptTokenTypes.STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AngelScriptFile(viewProvider)

    private companion object {
        val FILE = IFileElementType(AngelScriptLanguage)
    }
}
