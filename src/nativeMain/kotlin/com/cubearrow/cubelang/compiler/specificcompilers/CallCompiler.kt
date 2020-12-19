package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getVariableFromArrayGet
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.moveArrayGetToSth
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.UsualErrorMessages
import kotlin.math.max

class CallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Call> {
    override fun accept(expression: Expression.Call): String {
        if (expression.expression is Expression.VarCall) {
            val varCall = expression.expression
            val function = context.functions[varCall.identifier.substring]
            if (function != null) {
                context.argumentIndex = 0
                val args = getFunctionCallArguments(expression, function)
                return "${args}call ${varCall.identifier.substring}"
            }
            UsualErrorMessages.xNotFound("called function", varCall.identifier)
        }
        return ""
    }

    private fun getFunctionCallArguments(call: Expression.Call, function: Compiler.Function): String {
        var args = ""
        val laterArgs: MutableMap<Int, Expression> = HashMap()
        for (i in call.expressionLst.indices) {
            val argumentExpression = call.expressionLst[i]
            if (argumentExpression !is Expression.Call) {
                laterArgs[i] = argumentExpression
                continue
            }
            args += getSingleArgument(function, i, argumentExpression)
        }

        for (entry in laterArgs) {
            val i = entry.key
            val argumentExpression = call.expressionLst[i]
            args += getSingleArgument(function, i, argumentExpression)
        }
        return args
    }

    private fun getSingleArgument(function: Compiler.Function, index: Int, argumentExpression: Expression): String {
        val expectedArgumentType = function.args[function.args.keys.elementAt(index)] ?: error("Unreachable")
        val argumentLength = expectedArgumentType.getRawLength()
        val axRegister = CompilerUtils.getRegister("ax", argumentLength)
        return if (argumentLength >= 4 || argumentExpression is Expression.Literal || argumentExpression is Expression.ArrayGet) {
            getHigherSizedArgument(argumentLength, argumentExpression, axRegister, index)
        } else {
            getLowSizedArgument(argumentExpression, axRegister, index)
        }
    }

    private fun getHigherSizedArgument(
        argumentLength: Int,
        argumentExpression: Expression,
        axRegister: String,
        argumentIndex: Int
    ): String {
        val argumentLength1 = max(argumentLength, 4)
        val baseString =
            "mov ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[argumentIndex]!!, argumentLength1)}, "
        return if (argumentExpression is Expression.Literal || argumentExpression is Expression.VarCall) {
            "$baseString${argumentExpression.accept(context.compilerInstance)} \n"
        } else if (argumentExpression is Expression.ArrayGet) {
            val variable = getVariableFromArrayGet(argumentExpression, context)
            if (variable != null) {
                moveArrayGetToSth(
                    argumentExpression,
                    "mov ${
                        CompilerUtils.getRegister(
                            Compiler.ARGUMENT_INDEXES[argumentIndex]!!,
                            variable.type.getRawLength()
                        )
                    }, " +
                            CompilerUtils.getASMPointerLength(variable.type.getRawLength()), context
                )
            } else {
                ""
            }
        } else {
            "${argumentExpression.accept(context.compilerInstance)} \n" +
                    "$baseString$axRegister \n"
        }
    }


    private fun getLowSizedArgument(argumentExpression: Expression, axRegister: String, index: Int): String {
        return when (argumentExpression) {
            is Expression.VarCall -> {
                "mov $axRegister, ${argumentExpression.accept(context.compilerInstance)} \n" +
                        "movsx ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[index]!!, 8)}, $axRegister\n"
            }
            else -> {
                println("Test")
                "${argumentExpression.accept(context.compilerInstance)} \n" +
                        "movsx ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[index]!!, 8)}, $axRegister\n"
            }
        }
    }
}