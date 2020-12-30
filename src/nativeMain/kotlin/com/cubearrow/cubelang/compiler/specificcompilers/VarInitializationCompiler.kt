package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import Main
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.moveExpressionToX
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils

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
        val value = expression.valueExpression?.accept(context.compilerInstance)

        return when (expression.valueExpression) {
            is Expression.Literal -> {
                val type = ExpressionUtils.getType(expression.type, expression.valueExpression.value)
                val length = type.getLength()
                initializeVariable(length, expression, Compiler.LocalVariable(context.stackIndex.last() + length, type))
                "mov ${CompilerUtils.getASMPointerLength(type.getRawLength())} [rbp - ${context.stackIndex.last()}], $value"
            }
            is Expression.VarCall -> {
                initializeVarCall(expression)
            }
            is Expression.Call -> {
                initializeVariableWithCall(expression, value)
            }
            is Expression.Operation -> {
                initializeVariable(context.operationResultType!!.getLength(), expression,
                        Compiler.LocalVariable(context.stackIndex.last() + context.operationResultType!!.getLength(),
                                context.operationResultType!!))

                "$value \n" + CompilerUtils.moveAXToVariable(context.operationResultType!!.getRawLength(), context)
            }
            else -> {
                val (first, pointer, type) = moveExpressionToX(expression.valueExpression!!, context)
                initializeVariable(type.getLength(), expression, Compiler.LocalVariable(context.stackIndex.last() + type.getLength(),
                    type))
                "$first\nmov ${CompilerUtils.getASMPointerLength(type.getRawLength())} [rbp - ${context.stackIndex.last()}], $pointer"
            }
        }
    }

    private fun initializeVarCall(expression: Expression.VarInitialization): String {
        val varCall = expression.valueExpression as Expression.VarCall
        val variableToAssign = context.variables.last()[varCall.varName.substring]
                ?: error("Variable not found")
        expression.type?.let { checkMatchingTypes(it, variableToAssign.type, -1, -1) }
        val length = variableToAssign.type.getLength()
        val variable = Compiler.LocalVariable(context.stackIndex.last() + length, variableToAssign.type)

        initializeVariable(length, expression, variable)
        return CompilerUtils.assignVariableToVariable(variable, variableToAssign)
    }

    private fun initializeVariable(length: Int, varInitialization: Expression.VarInitialization, variable: Compiler.LocalVariable) {
        context.stackIndex.add(context.stackIndex.removeLast() + length)
        context.variables.last()[varInitialization.name.substring] = variable
    }

    private fun initializeVariableWithCall(varInitialization: Expression.VarInitialization, value: String?): String {
        val call = varInitialization.valueExpression as Expression.Call
        if(call.callee is Expression.VarCall) {
            val name = call.callee.varName
            val function = context.functions[name.substring] ?: error("The called function does not exist")
            if (function.returnType == null) {
                Main.error(name.line, name.line, "The function does not return a value")
                return ""
            }
            varInitialization.type?.let { checkMatchingTypes(it, function.returnType!!, -1, -1) }

            val length = function.returnType!!.getLength()
            initializeVariable(length, varInitialization, Compiler.LocalVariable(context.stackIndex.last() + length, function.returnType!!))
            return "$value \n" + CompilerUtils.moveAXToVariable(function.returnType!!.getRawLength(), context)
        }
        TODO()
    }
}