package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils

class FunctionDefinitionCompiler(var context: CompilerContext) : SpecificCompiler<Expression.FunctionDefinition> {
    override fun accept(expression: Expression.FunctionDefinition): String {
        context.separateReturnSegment = false
        val args = ExpressionUtils.mapArgumentDefinitions(expression.args)
        if (args.size > 5)
            Main.error(expression.name.line, expression.name.index, "The function must only have 5 arguments")
        context.functions[expression.name.substring] = Compiler.Function(expression.name.substring, args, expression.type)

        context.stackIndex.add(0)
        context.variables.add(HashMap())
        context.currentReturnType = expression.type // TODO: Figure out how to handle returning arrays
        context.argumentIndex = 0
        var statements = ""
        expression.args.forEach { statements += it.accept(context.compilerInstance) + "\n" }
        statements += expression.body.accept(context.compilerInstance)
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