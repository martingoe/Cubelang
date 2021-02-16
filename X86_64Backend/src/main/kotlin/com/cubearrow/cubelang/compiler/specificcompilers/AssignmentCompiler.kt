package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CommonErrorMessages
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.TypeUtils

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
            is Expression.ValueFromPointer -> {
                val pointerType = CompilerUtils.getPointerTypeFromValueFromPointer(expression.valueExpression as Expression.ValueFromPointer, context)
                // Check if it is a struct
                if (pointerType.subtype is NormalType && !Compiler.PRIMARY_TYPES.contains((pointerType.subtype as NormalType).typeName)) {
                    return CompilerUtils.setVariableToStructFromPointer(pointerType, variable)
                }
                moveDefault(expression, variable)
            }

            is Expression.InstanceGet -> {
                val moveInformation = context.moveExpressionToX(expression.valueExpression)
                val rawLength = TypeUtils.getRawLength(moveInformation.type)

                return CompilerUtils.moveLocationToLocation(
                    "${CompilerUtils.getASMPointerLength(rawLength)} [rbp - ${variable.index}]",
                    moveInformation.pointer,
                    CompilerUtils.getRegister("ax", rawLength)
                )
            }

            is Expression.VarCall,
            is Expression.ArrayGet -> {
                val localVariable = context.getVariableFromArrayGet(expression.valueExpression)
                if (localVariable != null) {
                    context.assignVariableToVariable(variable, localVariable)
                }
                val token = CompilerUtils.getTokenFromArrayGet(expression.valueExpression)
                context.error(token.line, token.index, "Could not find the requested variable.")
                ""
            }
            else -> {
                moveDefault(expression, variable)
            }
        }
    }

    private fun moveDefault(expression: Expression.Assignment, variable: Compiler.LocalVariable): String{
        val moveInformation = context.moveExpressionToX(expression.valueExpression)
        CompilerUtils.checkMatchingTypes(variable.type, moveInformation.type, context = context)
        return moveInformation.moveTo("${CompilerUtils.getASMPointerLength(TypeUtils.getRawLength(variable.type))} [rbp - ${variable.index}]")
    }


}