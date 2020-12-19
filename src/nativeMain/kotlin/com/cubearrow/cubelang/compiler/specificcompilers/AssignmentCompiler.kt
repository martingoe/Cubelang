package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.UsualErrorMessages

class AssignmentCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Assignment> {
    override fun accept(expression: Expression.Assignment): String {
        val variable = context.variables.last()[expression.name.substring]
        if (variable == null) {
            UsualErrorMessages.xNotFound("variable '${expression.name.substring}'", expression.name)
            //Unreachable
            return ""
        }

        return when (expression.valueExpression) {
            is Expression.Literal -> {
                "mov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} [rbp - ${variable.index}], ${expression.valueExpression.accept(context.compilerInstance)}"
            }
            is Expression.VarCall -> {
                val localVariable = context.variables.last()[expression.valueExpression.varName.substring]
                if (localVariable != null) {
                    CompilerUtils.assignVariableToVariable(variable, localVariable)
                }
                UsualErrorMessages.xNotFound("variable", expression.valueExpression.varName)
                ""
            }
            else -> {
                "${expression.valueExpression.accept(context.compilerInstance)} \n" +
                        "mov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} [rbp - ${variable.index}], ${CompilerUtils.getRegister("ax", variable.length)}"
            }
        }
    }
}