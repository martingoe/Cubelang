package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils

class VarInitializationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarInitialization> {
    override fun accept(expression: Expression.VarInitialization): String {
        if (expression.expressionNull1 != null) {
            return initializeValueNotNull(expression)
        }

        context.variables.last()[expression.identifier1.substring] =
                Compiler.LocalVariable(context.stackIndex.last(), expression.identifierNull1!!.substring,
                        Compiler.LENGTHS_OF_TYPES[expression.identifierNull1!!.substring]!!)
        return ""
    }

    private fun initializeValueNotNull(expression: Expression.VarInitialization): String {
        val value = expression.expressionNull1?.accept(context.compilerInstance)

        return when (expression.expressionNull1) {
            is Expression.Literal -> {
                val type = ExpressionUtils.getType(expression.identifierNull1?.substring, (expression.expressionNull1 as Expression.Literal).any1)
                val length = Compiler.LENGTHS_OF_TYPES[type]!!
                initializeVariable(length, expression, Compiler.LocalVariable(context.stackIndex.last() + length, type, length))
                "mov ${CompilerUtils.getASMPointerLength(length)} [rbp - ${context.stackIndex.last()}], $value"
            }
            is Expression.VarCall -> {
                initializeVarCall(expression)
            }
            is Expression.Call -> {
                initializeVariableWithCall(expression, value)
            }
            is Expression.Operation -> {
                initializeVariable(context.operationResultSize, expression,
                        Compiler.LocalVariable(context.stackIndex.last() + context.operationResultSize, "any",
                                context.operationResultSize)) // TODO actual type

                "$value \n" + CompilerUtils.moveAXToVariable(context.operationResultSize, context)
            }
            else -> {
                val length = 8
                initializeVariable(length, expression, Compiler.LocalVariable(context.stackIndex.last() + length, "any", length))

                "$value \n" + CompilerUtils.moveAXToVariable(length, context)
            }
        }
    }

    private fun initializeVarCall(expression: Expression.VarInitialization): String {
        val varCall = expression.expressionNull1 as Expression.VarCall
        val variableToAssign = context.variables.last()[varCall.identifier1.substring]
                ?: error("Variable not found")
        expression.identifierNull1?.let { checkMatchingTypes(it, variableToAssign.type) }
        val length = Compiler.LENGTHS_OF_TYPES[variableToAssign.type]!!
        val variable = Compiler.LocalVariable(context.stackIndex.last() + length, variableToAssign.type, length)

        initializeVariable(length, expression, variable)
        return CompilerUtils.assignVariableToVariable(variable, variableToAssign)
    }

    private fun initializeVariable(length: Int, varInitialization: Expression.VarInitialization, variable: Compiler.LocalVariable) {
        context.stackIndex.add(context.stackIndex.removeLast() + length)
        context.variables.last()[varInitialization.identifier1.substring] = variable
    }

    private fun initializeVariableWithCall(varInitialization: Expression.VarInitialization, value: String?): String {
        val call = varInitialization.expressionNull1 as Expression.Call
        if(call.expression1 is Expression.VarCall) {
            val name = (call.expression1 as Expression.VarCall).identifier1
            val function = context.functions[name.substring] ?: error("The called function does not exist")
            if (function.returnType == null) {
                Main.error(name.line, name.line, null, "The function does not return a value")
                return ""
            }
            varInitialization.identifierNull1?.let { checkMatchingTypes(it, function.returnType!!) }

            val length = Compiler.LENGTHS_OF_TYPES[function.returnType]!!
            initializeVariable(length, varInitialization, Compiler.LocalVariable(context.stackIndex.last() + length, function.returnType!!, length))
            return "$value \n" + CompilerUtils.moveAXToVariable(length, context)
        }
        TODO()
    }
}