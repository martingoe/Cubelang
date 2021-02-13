package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.parser.Expression

class VarInitializationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarInitialization> {
    override fun accept(expression: Expression.VarInitialization): String {
        if (expression.valueExpression != null) {
            return initializeValueNotNull(expression)
        }

        context.stackIndex.add(context.stackIndex.removeLast() + (expression.type?.getLength() ?: error("Unreachable")))
        context.variables.last()[expression.name.substring] =
                Compiler.LocalVariable(context.stackIndex.last(), expression.type)
        return ""
    }

    private fun initializeValueNotNull(expression: Expression.VarInitialization): String {
        return when (expression.valueExpression) {
            is Expression.VarCall -> {
                initializeVarCall(expression)
            }
            else -> {
                val moveInformation = context.moveExpressionToX(expression.valueExpression!!)
                initializeVariable(moveInformation.type.getLength(), expression, Compiler.LocalVariable(context.stackIndex.last() + moveInformation.type.getLength(),
                    moveInformation.type))
                moveInformation.moveTo(CompilerUtils.getASMPointerLength(moveInformation.type.getRawLength()) + " [rbp - ${context.stackIndex.last()}]")
            }
        }
    }

    private fun initializeVarCall(expression: Expression.VarInitialization): String {
        val varCall = expression.valueExpression as Expression.VarCall
        val variableToAssign = context.variables.last()[varCall.varName.substring]
                ?: error("Variable not found")
        expression.type?.let { checkMatchingTypes(it, variableToAssign.type, -1, -1, context) }
        val length = variableToAssign.type.getLength()
        val variable = Compiler.LocalVariable(context.stackIndex.last() + length, variableToAssign.type)

        initializeVariable(length, expression, variable)
        return CompilerUtils.assignVariableToVariable(variable, variableToAssign)
    }

    private fun initializeVariable(length: Int, varInitialization: Expression.VarInitialization, variable: Compiler.LocalVariable) {
        context.stackIndex.add(context.stackIndex.removeLast() + length)
        context.variables.last()[varInitialization.name.substring] = variable
    }

}