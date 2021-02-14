package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext

/**
 * Compiles the context of a block of code. A new map of variables is created and popped again.
 *
 * Stops after the first [Expression.ReturnStmnt].
 * @param context The needed [CompilerContext].
 */
class BlockCompiler (var context: CompilerContext): SpecificCompiler<Expression.BlockStatement>{
    override fun accept(expression: Expression.BlockStatement): String {
        var result = ""
        context.variables.add(HashMap())
        for (it in expression.statements) {
            result += context.evaluate(it) + "\n"
            if (it is Expression.ReturnStmnt) break
        }
        context.variables.removeLast()
        return result
    }
}