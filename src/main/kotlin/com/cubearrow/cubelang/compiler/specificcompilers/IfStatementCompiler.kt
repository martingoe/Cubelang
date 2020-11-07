package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class IfStatementCompiler(var context: CompilerContext): SpecificCompiler<Expression.IfStmnt> {
    override fun accept(expression: Expression.IfStmnt): String {
        context.inIfCondition = true
        val condition = expression.expression1.accept(context.compilerInstance) + "\n"
        var first = ""
        expression.expressionLst1.forEach {
            var x = it.accept(context.compilerInstance)
            if (it is Expression.ReturnStmnt) {
                x += "\njmp .L${context.lIndex + 1}"
                context.separateReturnSegment = true
            }
            first += x + "\n"
        }
        first += if (expression.expressionLst2.isNotEmpty() && !context.separateReturnSegment) "jmp .L${context.lIndex + 1}\n" else ""

        val after = ".L${context.lIndex++}:\n${expression.expressionLst2.joinToString { it.accept(context.compilerInstance) }}"
        return condition + first + after + if (expression.expressionLst2.isNotEmpty() && !context.separateReturnSegment) "\n.L${context.lIndex}:" else ""

    }
}