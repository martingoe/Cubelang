package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.UsualErrorMessages

class GroupingCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Grouping> {
    override fun accept(expression: Expression.Grouping): String {
        return when (expression.expression) {
            is Expression.Literal -> "mov ${
                CompilerUtils.getRegister(
                    "ax",
                    ExpressionUtils.getType(
                        null,
                        expression.expression.any
                    ).getRawLength()
                )
            }"
            is Expression.VarCall -> {
                val varCall = expression.expression
                val variable = context.variables.last()[varCall.identifier.substring]
                if (variable == null) {
                    UsualErrorMessages.xNotFound("variable", varCall.identifier)
                    return ""
                }
                context.operationResultSize = variable.type.getRawLength()
                "mov ${CompilerUtils.getRegister("ax", variable.length)} ${varCall.accept(context.compilerInstance)}"
            }
            else -> expression.expression.accept(context.compilerInstance)
        }
    }

}