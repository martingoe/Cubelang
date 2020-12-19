package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class BlockCompiler (var context: CompilerContext): SpecificCompiler<Expression.BlockStatement>{
    override fun accept(expression: Expression.BlockStatement): String {
        var result = ""
        for (it in expression.expressionLst) {
            result += it.accept(context.compilerInstance) + "\n"
            if (it is Expression.ReturnStmnt ) {
                if(context.inIfStatement) {
                    result += "jmp .L${context.lIndex + 1}\n"
                    context.separateReturnSegment = true
                }
                break
            }
        }
        return result
    }
}