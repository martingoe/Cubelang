package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class ImportStmntCompiler(val context: CompilerContext) : SpecificCompiler<Expression.ImportStmnt> {
    override fun accept(expression: Expression.ImportStmnt): String {
        val functions = Main.definedFunctions[expression.identifier.substring]
        if (functions == null) {
            context.error(
                expression.identifier.line,
                expression.identifier.index,
                "Could not find the specified file/module in the sources or in the stdlib."
            )
            return ""
        }
        context.addFunctions(functions)
        return "%include \"${(if (expression.identifier.tokenType == TokenType.IDENTIFIER) Compiler.LIBRARY_PATH else "") + expression.identifier.substring}.asm\""
    }
}
