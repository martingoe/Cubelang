package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils

class VarInitializationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarInitialization> {
    override fun accept(expression: Expression.VarInitialization): String {
        if (expression.expressionNull1 != null) {
            val value = expression.expressionNull1?.accept(context.compilerInstance)

            return when (expression.expressionNull1) {
                is Expression.Literal -> {
                    val type = ExpressionUtils.getType(expression.identifierNull1?.substring, (expression.expressionNull1 as Expression.Literal).any1)
                    val length = Compiler.LENGTHS_OF_TYPES[type]
                    initializeVariable(length, expression, Compiler.LocalVariable(context.stackIndex.peek() + length, type, length))
                    "mov ${CompilerUtils.getASMPointerLength(length)} [rbp - ${context.stackIndex.peek()}], $value"
                }
                is Expression.VarCall -> {
                    val varCall = expression.expressionNull1 as Expression.VarCall
                    val variableToAssign = context.variables.peek()[varCall.identifier1.substring]
                            ?: error("Variable not found")
                    expression.identifierNull1?.let { checkMatchingTypes(it, variableToAssign.type) }
                    val length = Compiler.LENGTHS_OF_TYPES[variableToAssign.type]
                    val variable = Compiler.LocalVariable(context.stackIndex.peek() + length, variableToAssign.type, length)

                    initializeVariable(length, expression, variable)
                    CompilerUtils.assignVariableToVariable(variable, variableToAssign)
                }
                is Expression.Call -> {
                    initializeVariableWithCall(expression, value)
                }
                is Expression.Operation -> {
                    initializeVariable(context.operationResultSize, expression,
                            Compiler.LocalVariable(context.stackIndex.peek() + context.operationResultSize, "any",
                                    context.operationResultSize)) // TODO

                    "$value \n" + CompilerUtils.moveAXToVariable(context.operationResultSize, context)

                }
                else -> {
                    val length = 8
                    initializeVariable(length, expression, Compiler.LocalVariable(context.stackIndex.peek() + length, "any", length))

                    "$value \n" + CompilerUtils.moveAXToVariable(length, context)
                }
            }
        }

        context.variables.peek()[expression.identifier1.substring] =
                Compiler.LocalVariable(context.stackIndex.peek(), expression.identifierNull1!!.substring,
                        Compiler.LENGTHS_OF_TYPES[expression.identifierNull1!!.substring])
        return ""
    }

    private fun initializeVariable(length: Int, varInitialization: Expression.VarInitialization, variable: Compiler.LocalVariable) {
        context.stackIndex.push(context.stackIndex.pop() + length)
        context.variables.peek()[varInitialization.identifier1.substring] = variable
    }

    private fun initializeVariableWithCall(varInitialization: Expression.VarInitialization, value: String?): String {
        val call = varInitialization.expressionNull1 as Expression.Call
        val function = context.functions[call.identifier1.substring] ?: error("The called function does not exist")
        if (function.returnType == null) {
            Main.error(call.identifier1.line, call.identifier1.line, null, "The function does not return a value")
            return ""
        }
        varInitialization.identifierNull1?.let { checkMatchingTypes(it, function.returnType!!) }

        val length = Compiler.LENGTHS_OF_TYPES[function.returnType]!!
        initializeVariable(length, varInitialization, Compiler.LocalVariable(context.stackIndex.peek() + length, function.returnType!!, length))
        return "$value \n" + CompilerUtils.moveAXToVariable(length, context)
    }
}