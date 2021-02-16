package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.definitions.DefinedFunctions
import com.cubearrow.cubelang.common.tokens.TokenType
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import java.io.File

class ImportStmntCompiler(val context: CompilerContext) : SpecificCompiler<Expression.ImportStmnt> {
    override fun accept(expression: Expression.ImportStmnt): String {
        val functions = DefinedFunctions.definedFunctions[expression.identifier.substring]
        if (functions == null) {
            context.error(
                expression.identifier.line,
                expression.identifier.index,
                "Could not find the specified file/module in the sources or in the stdlib."
            )
            return ""
        }
        context.addFunctions(functions)
        return "%include \"${if (expression.identifier.tokenType == TokenType.IDENTIFIER) Compiler.LIBRARY_PATH + expression.identifier.substring else expression.identifier.substring.substringBeforeLast(".")}.asm\""
    }
}
