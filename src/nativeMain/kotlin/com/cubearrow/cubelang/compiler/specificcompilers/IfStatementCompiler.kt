package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class IfStatementCompiler(var context: CompilerContext): SpecificCompiler<Expression.IfStmnt> {
    override fun accept(expression: Expression.IfStmnt): String {
        context.lIndex++
        context.inIfStatement = true
        context.inJmpCondition = true
        val condition = expression.expression1.accept(context.compilerInstance) + "\n"
        context.inJmpCondition = false

        val first = expression.expression2.accept(context.compilerInstance) + "\n" +
            if (expression.expressionNull1 != null && !context.separateReturnSegment) "jmp .L${context.lIndex + 1}\n" else ""

        val after = ".L${context.lIndex++}:\n${expression.expressionNull1?.accept(context.compilerInstance) ?: ""}"
        context.inIfStatement = false
        return condition + first + after + if (expression.expressionNull1 != null && !context.separateReturnSegment) "\n.L${context.lIndex}:" else ""
    }
}