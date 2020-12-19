package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class IfStatementCompiler(var context: CompilerContext): SpecificCompiler<Expression.IfStmnt> {
    override fun accept(expression: Expression.IfStmnt): String {
        context.lIndex++
        context.inIfStatement = true
        context.inJmpCondition = true
        val condition = expression.condition.accept(context.compilerInstance) + "\n"
        context.inJmpCondition = false

        val first = expression.ifBody.accept(context.compilerInstance) + "\n" +
            if (expression.elseBody != null && !context.separateReturnSegment) "jmp .L${context.lIndex + 1}\n" else ""

        val after = ".L${context.lIndex++}:\n${expression.elseBody?.accept(context.compilerInstance) ?: ""}"
        context.inIfStatement = false
        return condition + first + after + if (expression.elseBody != null && !context.separateReturnSegment) "\n.L${context.lIndex}:" else ""
    }
}