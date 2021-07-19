package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.NoneType
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.MoveInformation
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.TypeUtils

class VarInitializationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarInitialization> {
    override fun accept(expression: Expression.VarInitialization): String {
        if (expression.valueExpression != null) {
            return initializeValueNotNull(expression)
        }

        context.stackIndex.add(context.stackIndex.removeLast() + (TypeUtils.getLength(expression.type)))
        context.variables.last()[expression.name.substring] =
            Compiler.LocalVariable(context.stackIndex.last(), expression.type)
        return ""
    }

    private fun initializeValueNotNull(expression: Expression.VarInitialization): String {
        return when (expression.valueExpression) {
            is Expression.ValueFromPointer -> {
                val pointerType = CompilerUtils.getPointerTypeFromValueFromPointer(expression.valueExpression as Expression.ValueFromPointer, context)
                // Check if it is a struct
                if (pointerType.subtype is NormalType && !Compiler.PRIMARY_TYPES.contains((pointerType.subtype as NormalType).typeName)) {
                    val localVariable = Compiler.LocalVariable(
                        context.stackIndex.last() + TypeUtils.getLength(pointerType.subtype), pointerType.subtype)
                    initializeVariable(expression, localVariable)

                    return CompilerUtils.setVariableToStructFromPointer(pointerType, localVariable)
                }
                val moveInformation = context.moveExpressionToX(expression.valueExpression!!)
                return defaultValueNotNull(moveInformation, expression)
            }

            is Expression.InstanceGet -> {
                val moveInformation = context.moveExpressionToX(expression.valueExpression!!)
                val rawLength = TypeUtils.getRawLength(moveInformation.type)
                val register = CompilerUtils.getRegister("ax", rawLength)

                val localVariable = Compiler.LocalVariable(context.stackIndex.last() + TypeUtils.getLength(moveInformation.type), moveInformation.type)

                initializeVariable(expression, localVariable)
                return CompilerUtils.moveLocationToLocation("${CompilerUtils.getASMPointerLength(rawLength)} [rbp - ${localVariable.index}]", moveInformation.pointer, register)
            }

            is Expression.VarCall -> {
                initializeVarCall(expression)
            }
            else -> {
                val moveInformation = context.moveExpressionToX(expression.valueExpression!!)
                defaultValueNotNull(moveInformation, expression)
            }
        }
    }

    private fun defaultValueNotNull(
        moveInformation: MoveInformation,
        expression: Expression.VarInitialization
    ): String {
        initializeVariable(
            expression, Compiler.LocalVariable(
                context.stackIndex.last() + TypeUtils.getLength(moveInformation.type),
                moveInformation.type
            )
        )
        return moveInformation.moveTo(CompilerUtils.getASMPointerLength(TypeUtils.getRawLength(moveInformation.type)) + " [rbp - ${context.stackIndex.last()}]")
    }

    private fun initializeVarCall(expression: Expression.VarInitialization): String {
        val varCall = expression.valueExpression as Expression.VarCall
        val variableToAssign = context.variables.last()[varCall.varName.substring]
            ?: error("Variable not found")
        val length = TypeUtils.getLength(variableToAssign.type)
        val variable = Compiler.LocalVariable(context.stackIndex.last() + length, variableToAssign.type)

        initializeVariable(expression, variable)
        return context.assignVariableToVariable(variable, variableToAssign)
    }

    private fun initializeVariable(variableInitialization: Expression.VarInitialization, variable: Compiler.LocalVariable) {
        if(variableInitialization.type !is NoneType) { CompilerUtils.checkMatchingTypes(variableInitialization.type, variable.type, -1, -1, context) }
        context.stackIndex.add(context.stackIndex.removeLast() + TypeUtils.getLength(variable.type))
        context.variables.last()[variableInitialization.name.substring] = variable
    }

}