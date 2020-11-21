package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class BlockCompiler (var context: CompilerContext): SpecificCompiler<Expression.BlockStatement>{
    override fun accept(expression: Expression.BlockStatement): String {
        var result = ""
        for (it in expression.expressionLst1) {
            var x = it.accept(context.compilerInstance)
            result += x + "\n"
            if (it is Expression.ReturnStmnt ) {
                if(context.inIfCondition) {
                    x += "\njmp .L${context.lIndex + 1}"
                    context.separateReturnSegment = true
                }
                break
            }
        }
        return result
    }
}