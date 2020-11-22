package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class ForLoopCompiler(var context: CompilerContext) : SpecificCompiler<Expression.ForStmnt> {
    override fun accept(expression: Expression.ForStmnt): String {
        context.inJmpCondition = true
        context.lIndex++
        val x = """${expression.expressionLst1[0].accept(context.compilerInstance)}
                |.L${context.lIndex++}:
                |${expression.expressionLst1[1].accept(context.compilerInstance)}
                |${expression.expression1.accept(context.compilerInstance)}
                |${expression.expressionLst1[2].accept(context.compilerInstance)}
                |jmp .L${context.lIndex - 1}
                |.L${context.lIndex}:""".trimMargin()
        context.inJmpCondition = false
        return x
    }
}