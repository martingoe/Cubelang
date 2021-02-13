package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

/**
 * Compiles defining a function by using a frame pointer.
 *
 * The [FunctionDefinitionCompiler.accept] function is not pure and has multiple side effects.
 *
 * @param context The needed [CompilerContext].
 */
class FunctionDefinitionCompiler(var context: CompilerContext) : SpecificCompiler<Expression.FunctionDefinition> {
    override fun accept(expression: Expression.FunctionDefinition): String {
        context.separateReturnSegment = false
        val args = ExpressionUtils.mapArgumentDefinitions(expression.args)
        if (args.size > 5)
            Main.error(expression.name.line, expression.name.index, "The function must only have 5 arguments")
        context.addFunction(Compiler.Function(expression.name.substring, args, expression.type))

        context.stackIndex.add(0)
        context.variables.add(HashMap())
        context.currentReturnType = expression.type
        context.argumentIndex = 0
        val statements = expression.args.fold("", {acc, it ->  acc + context.evaluate(it)}) + context.evaluate(expression.body)
        context.variables.removeLast()
        context.currentReturnType = null

        return """${expression.name.substring}:
            |push rbp
            |mov rbp, rsp
            |sub rsp, ${context.stackIndex.removeLast()}
            |$statements
            |${if (context.separateReturnSegment) ".L${context.lIndex}:" else ""}
            |leave
            |ret
        """.trimMargin()
    }
}