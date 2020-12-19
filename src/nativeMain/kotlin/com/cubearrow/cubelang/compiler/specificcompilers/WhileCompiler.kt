package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class WhileCompiler(var context: CompilerContext) : SpecificCompiler<Expression.WhileStmnt>{
    override fun accept(expression: Expression.WhileStmnt): String {
        context.inJmpCondition = true
        val contextLIndex = ++context.lIndex
        context.lIndex++
        val x = """$.L${contextLIndex}:
                |${expression.expression.accept(context.compilerInstance)}
                |${expression.expression2.accept(context.compilerInstance)}
                |jmp .L${contextLIndex}
                |.L${contextLIndex + 1}:""".trimMargin()
        context.inJmpCondition = false
        return x
    }
}