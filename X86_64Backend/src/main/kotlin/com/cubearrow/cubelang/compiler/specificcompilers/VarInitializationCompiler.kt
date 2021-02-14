package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.TypeUtils

class VarInitializationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarInitialization> {
    override fun accept(expression: Expression.VarInitialization): String {
        if (expression.valueExpression != null) {
            return initializeValueNotNull(expression)
        }

        context.stackIndex.add(context.stackIndex.removeLast() + (TypeUtils.getLength(expression.type!!)))
        context.variables.last()[expression.name.substring] =
                Compiler.LocalVariable(context.stackIndex.last(), expression.type!!)
        return ""
    }

    private fun initializeValueNotNull(expression: Expression.VarInitialization): String {
        return when (expression.valueExpression) {
            is Expression.VarCall -> {
                initializeVarCall(expression)
            }
            else -> {
                val moveInformation = context.moveExpressionToX(expression.valueExpression!!)
                initializeVariable(TypeUtils.getLength(moveInformation.type), expression, Compiler.LocalVariable(context.stackIndex.last() + TypeUtils.getLength(moveInformation.type),
                    moveInformation.type))
                moveInformation.moveTo(CompilerUtils.getASMPointerLength(TypeUtils.getRawLength(moveInformation.type)) + " [rbp - ${context.stackIndex.last()}]")
            }
        }
    }

    private fun initializeVarCall(expression: Expression.VarInitialization): String {
        val varCall = expression.valueExpression as Expression.VarCall
        val variableToAssign = context.variables.last()[varCall.varName.substring]
                ?: error("Variable not found")
        expression.type?.let { CompilerUtils.checkMatchingTypes(it, variableToAssign.type, -1, -1, context) }
        val length = TypeUtils.getLength(variableToAssign.type)
        val variable = Compiler.LocalVariable(context.stackIndex.last() + length, variableToAssign.type)

        initializeVariable(length, expression, variable)
        return CompilerUtils.assignVariableToVariable(variable, variableToAssign)
    }

    private fun initializeVariable(length: Int, varInitialization: Expression.VarInitialization, variable: Compiler.LocalVariable) {
        context.stackIndex.add(context.stackIndex.removeLast() + length)
        context.variables.last()[varInitialization.name.substring] = variable
    }

}