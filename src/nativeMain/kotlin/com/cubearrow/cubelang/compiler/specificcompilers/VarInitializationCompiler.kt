package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import Main
import com.cubearrow.cubelang.compiler.NormalType
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils

class VarInitializationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarInitialization> {
    override fun accept(expression: Expression.VarInitialization): String {
        if (expression.valueExpression != null) {
            return initializeValueNotNull(expression)
        }

        context.stackIndex.add(context.stackIndex.removeLast() + (expression.type?.getRawLength() ?: error("Unreachable")))
        context.variables.last()[expression.name.substring] =
                Compiler.LocalVariable(context.stackIndex.last(), ExpressionUtils.getType(expression.type, null),
                        expression.type.getLength())
        return ""
    }

    private fun initializeValueNotNull(expression: Expression.VarInitialization): String {
        val value = expression.valueExpression?.accept(context.compilerInstance)

        return when (expression.valueExpression) {
            is Expression.Literal -> {
                val type = ExpressionUtils.getType(expression.type, expression.valueExpression.value)
                val length = type.getLength()
                initializeVariable(length, expression, Compiler.LocalVariable(context.stackIndex.last() + length, type, length))
                "mov ${CompilerUtils.getASMPointerLength(type.getRawLength())} [rbp - ${context.stackIndex.last()}], $value"
            }
            is Expression.VarCall -> {
                initializeVarCall(expression)
            }
            is Expression.Call -> {
                initializeVariableWithCall(expression, value)
            }
            is Expression.Operation -> {
                initializeVariable(context.operationResultSize, expression,
                        Compiler.LocalVariable(context.stackIndex.last() + context.operationResultSize,
                            NormalType("any"),
                                context.operationResultSize)) // TODO actual type

                "$value \n" + CompilerUtils.moveAXToVariable(context.operationResultSize, context)
            }
            else -> {
                val length = 8
                initializeVariable(length, expression, Compiler.LocalVariable(context.stackIndex.last() + length,
                    NormalType("any"), length))

                "$value \n" + CompilerUtils.moveAXToVariable(length, context)
            }
        }
    }

    private fun initializeVarCall(expression: Expression.VarInitialization): String {
        val varCall = expression.valueExpression as Expression.VarCall
        val variableToAssign = context.variables.last()[varCall.varName.substring]
                ?: error("Variable not found")
        expression.type?.let { checkMatchingTypes(it, variableToAssign.type) }
        val length = variableToAssign.type.getLength()
        val variable = Compiler.LocalVariable(context.stackIndex.last() + length, variableToAssign.type, length)

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
                Main.error(name.line, name.line, null, "The function does not return a value")
                return ""
            }
            varInitialization.type?.let { checkMatchingTypes(it, function.returnType!!) }

            val length = function.returnType!!.getLength()
            initializeVariable(length, varInitialization, Compiler.LocalVariable(context.stackIndex.last() + length, function.returnType!!, length))
            return "$value \n" + CompilerUtils.moveAXToVariable(function.returnType!!.getRawLength(), context)
        }
        TODO()
    }
}