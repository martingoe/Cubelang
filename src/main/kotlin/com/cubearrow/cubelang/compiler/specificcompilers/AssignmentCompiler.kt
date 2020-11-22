package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.UsualErrorMessages

class AssignmentCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Assignment> {
    override fun accept(expression: Expression.Assignment): String {
        val variable = context.variables.peek()[expression.identifier1.substring]
        if (variable == null) {
            UsualErrorMessages.xNotFound("variable '${expression.identifier1.substring}'", expression.identifier1)
            //Unreachable
            return ""
        }

        return when {
            expression.expression1 is Expression.Literal && (expression.expression1 as Expression.Literal).any1 is Int -> {
                "mov ${CompilerUtils.getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${expression.expression1.accept(context.compilerInstance)}"
            }
            expression.expression1 is Expression.VarCall -> {
                val varCall = expression.expression1 as Expression.VarCall
                val localVariable = context.variables.peek()[varCall.identifier1.substring]
                if (localVariable != null) {
                    CompilerUtils.assignVariableToVariable(variable, localVariable)
                }
                UsualErrorMessages.xNotFound("variable", varCall.identifier1)
                ""
            }
            else -> {
                "${expression.expression1.accept(context.compilerInstance)} \n" +
                "mov ${CompilerUtils.getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${CompilerUtils.getRegister("ax", variable.length)}"
            }
        }
    }
}