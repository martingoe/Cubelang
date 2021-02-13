package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.CommonErrorMessages

/**
 * Compiles assigning to a variable.
 *
 * @param context The needed [CompilerContext].
 */
class AssignmentCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Assignment> {
    override fun accept(expression: Expression.Assignment): String {
        val variable = context.getVariable(expression.name.substring)
        if (variable == null) {
            CommonErrorMessages.xNotFound("variable '${expression.name.substring}'", expression.name, context)
            //Unreachable
            return ""
        }

        return when (expression.valueExpression) {
            is Expression.VarCall,
            is Expression.ArrayGet-> {
                val localVariable = context.getVariableFromArrayGet(expression.valueExpression)
                if (localVariable != null) {
                    CompilerUtils.assignVariableToVariable(variable, localVariable)
                }
                val token = CompilerUtils.getTokenFromArrayGet(expression.valueExpression)
                context.error(token.line, token.index, "Could not find the requested variable.")
                ""
            }
            else -> {
                context.moveExpressionToX(expression.valueExpression).moveTo("${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} [rbp - ${variable.index}]")
            }
        }
    }
}