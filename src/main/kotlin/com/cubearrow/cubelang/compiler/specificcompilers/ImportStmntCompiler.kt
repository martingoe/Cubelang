package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class ImportStmntCompiler(val context: CompilerContext):SpecificCompiler<Expression.ImportStmnt> {
    override fun accept(expression: Expression.ImportStmnt): String {
        if (expression.identifier.tokenType == TokenType.IDENTIFIER) {
            val functions = Compiler.stdlib[expression.identifier.substring]
            if (functions == null) {
                Main.error(expression.identifier.line, expression.identifier.index, "Could not find the specified library in the stdlib.")
                return ""
            }
            context.addFunctions(functions)
            return "%include \"${Compiler.LIBRARY_PATH}${expression.identifier.substring}.asm\""
        }
        Main.error(expression.identifier.line, expression.identifier.index, "Cannot yet implement files that are not in the stdlib")
        return ""
    }
}
