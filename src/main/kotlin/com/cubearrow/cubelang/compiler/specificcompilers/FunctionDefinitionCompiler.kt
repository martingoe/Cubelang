package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils

class FunctionDefinitionCompiler(var context: CompilerContext) : SpecificCompiler<Expression.FunctionDefinition> {
    override fun accept(expression: Expression.FunctionDefinition): String {
        val args = ExpressionUtils.mapArgumentDefinitions(expression.expressionLst1)
        if (args.size > 5) Main.error(expression.identifier1.line, expression.identifier1.index, null, "The function must only have 5 arguments")
        context.functions[expression.identifier1.substring] = Compiler.Function(expression.identifier1.substring, args, expression.identifierNull1?.substring)

        context.stackIndex.push(0)
        context.variables.push(HashMap())
        context.currentReturnLength = Compiler.LENGTHS_OF_TYPES[expression.identifierNull1?.substring]
        context.argumentIndex = 0
        var statements = ""
        expression.expressionLst1.forEach { statements += it.accept(context.compilerInstance) + "\n" }
        expression.expressionLst2.forEach { statements += it.accept(context.compilerInstance) + "\n" }
        context.variables.pop()
        context.currentReturnLength = null

        return """${expression.identifier1.substring}:
            |push rbp
            |mov rbp, rsp
            |sub rsp, ${context.stackIndex.pop()}
            |$statements
            |${if (context.separateReturnSegment) ".L${context.lIndex}:" else ""}
            |leave
            |ret
        """.trimMargin()
    }
}