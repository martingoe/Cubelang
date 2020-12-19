package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.UsualErrorMessages

class AssignmentCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Assignment> {
    override fun accept(expression: Expression.Assignment): String {
        val variable = context.variables.last()[expression.identifier.substring]
        if (variable == null) {
            UsualErrorMessages.xNotFound("variable '${expression.identifier.substring}'", expression.identifier)
            //Unreachable
            return ""
        }

        return when (expression.expression) {
            is Expression.Literal -> {
                "mov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} [rbp - ${variable.index}], ${expression.expression.accept(context.compilerInstance)}"
            }
            is Expression.VarCall -> {
                val varCall = expression.expression
                val localVariable = context.variables.last()[varCall.identifier.substring]
                if (localVariable != null) {
                    CompilerUtils.assignVariableToVariable(variable, localVariable)
                }
                UsualErrorMessages.xNotFound("variable", varCall.identifier)
                ""
            }
            else -> {
                "${expression.expression.accept(context.compilerInstance)} \n" +
                        "mov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} [rbp - ${variable.index}], ${CompilerUtils.getRegister("ax", variable.length)}"
            }
        }
    }
}