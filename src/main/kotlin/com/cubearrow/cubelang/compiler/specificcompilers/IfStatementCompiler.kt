package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class IfStatementCompiler(var context: CompilerContext): SpecificCompiler<Expression.IfStmnt> {
    override fun accept(expression: Expression.IfStmnt): String {
        var ifBlock = ""
        if(expression.condition is Expression.Logical){
            ifBlock = ".L${++context.lIndex}:\n"
        }
        context.lIndex++
        context.jmpOnReturn = true
        context.inJmpCondition = true
        val condition = expression.condition.accept(context.compilerInstance) + "\n"
        context.inJmpCondition = false

        ifBlock += expression.ifBody.accept(context.compilerInstance) + "\n" +
            if (expression.elseBody != null && !context.separateReturnSegment) "jmp .L${context.lIndex + 1}\n" else ""


        val elseBlock = ".L${context.lIndex++}:\n${expression.elseBody?.accept(context.compilerInstance) ?: ""}"
        context.jmpOnReturn = false
        return condition + ifBlock + elseBlock + if (expression.elseBody != null && !context.separateReturnSegment) "\n.L${context.lIndex}:" else ""
    }
}