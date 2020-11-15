package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class IfStatementCompiler(var context: CompilerContext): SpecificCompiler<Expression.IfStmnt> {
    override fun accept(expression: Expression.IfStmnt): String {
        context.inIfCondition = true
        val condition = expression.expression1.accept(context.compilerInstance) + "\n"
        var first = ""
        context.jmpIfReturnStatement = true
        first += expression.expression2.accept(context.compilerInstance)

        first += if (expression.expressionNull1 != null && !context.separateReturnSegment) "jmp .L${context.lIndex + 1}\n" else ""
        context.jmpIfReturnStatement = false

        val after = ".L${context.lIndex++}:\n${expression.expressionNull1?.accept(context.compilerInstance) ?: ""}"
        return condition + first + after + if (expression.expressionNull1 != null && !context.separateReturnSegment) "\n.L${context.lIndex}:" else ""
    }
}