package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import Main
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils

class FunctionDefinitionCompiler(var context: CompilerContext) : SpecificCompiler<Expression.FunctionDefinition> {
    override fun accept(expression: Expression.FunctionDefinition): String {
        context.separateReturnSegment = false
        val args = ExpressionUtils.mapArgumentDefinitions(expression.expressionLst)
        if (args.size > 5)
            Main.error(expression.identifier.line, expression.identifier.index, null, "The function must only have 5 arguments")
        context.functions[expression.identifier.substring] = Compiler.Function(expression.identifier.substring, args, expression.typeNull)

        context.stackIndex.add(0)
        context.variables.add(HashMap())
        context.currentReturnLength = expression.typeNull?.getLength() // TODO: Figure out how to handle returning arrays
        context.argumentIndex = 0
        var statements = ""
        expression.expressionLst.forEach { statements += it.accept(context.compilerInstance) + "\n" }
        statements += expression.expression.accept(context.compilerInstance)
        context.variables.removeLast()
        context.currentReturnLength = null

        return """${expression.identifier.substring}:
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