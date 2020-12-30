package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getVariable
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.CommonErrorMessages

class AssignmentCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Assignment> {
    override fun accept(expression: Expression.Assignment): String {
        val variable = getVariable(expression.name.substring, context)
        if (variable == null) {
            CommonErrorMessages.xNotFound("variable '${expression.name.substring}'", expression.name)
            //Unreachable
            return ""
        }

        return when (expression.valueExpression) {
            is Expression.Literal -> {
                "mov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} [rbp - ${variable.index}], ${expression.valueExpression.accept(context.compilerInstance)}"
            }
            is Expression.VarCall -> {
                val localVariable = getVariable(expression.valueExpression.varName.substring, context)
                if (localVariable != null) {
                    CompilerUtils.assignVariableToVariable(variable, localVariable)
                }
                CommonErrorMessages.xNotFound("variable", expression.valueExpression.varName)
                ""
            }
            else -> {
                "${expression.valueExpression.accept(context.compilerInstance)} \n" +
                        "mov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} [rbp - ${variable.index}], ${CompilerUtils.getRegister("ax", variable.type.getRawLength())}"
            }
        }
    }
}