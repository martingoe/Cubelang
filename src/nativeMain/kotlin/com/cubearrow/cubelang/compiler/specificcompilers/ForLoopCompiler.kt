package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class ForLoopCompiler(var context: CompilerContext) : SpecificCompiler<Expression.ForStmnt> {
    override fun accept(expression: Expression.ForStmnt): String {
        context.inJmpCondition = true
        val contextLIndex = ++context.lIndex
        context.lIndex++
        val x = """${expression.expressionLst[0].accept(context.compilerInstance)}
                |.L${contextLIndex}:
                |${expression.expressionLst[1].accept(context.compilerInstance)}
                |${expression.expression.accept(context.compilerInstance)}
                |${expression.expressionLst[2].accept(context.compilerInstance)}
                |jmp .L${contextLIndex}
                |.L${contextLIndex + 1}:""".trimMargin()
        context.inJmpCondition = false
        return x
    }
}