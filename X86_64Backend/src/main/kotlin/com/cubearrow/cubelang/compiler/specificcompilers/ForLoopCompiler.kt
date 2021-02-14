package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext

/**
 * Compiles for loops using the l-labels.
 *
 * During the compilation, [CompilerContext.inJmpCondition] is set to true.
 *
 * @param context The needed [CompilerContext].
 */
class ForLoopCompiler(var context: CompilerContext) : SpecificCompiler<Expression.ForStmnt> {
    override fun accept(expression: Expression.ForStmnt): String {
        context.inJmpCondition = true
        val contextLIndex = ++context.lIndex
        context.lIndex++
        val x = """${expression.inBrackets[0].accept(context.compilerInstance)}
                |.L${contextLIndex}:
                |${expression.inBrackets[1].accept(context.compilerInstance)}
                |${expression.body.accept(context.compilerInstance)}
                |${expression.inBrackets[2].accept(context.compilerInstance)}
                |jmp .L${contextLIndex}
                |.L${contextLIndex + 1}:""".trimMargin()
        context.inJmpCondition = false
        return x
    }
}