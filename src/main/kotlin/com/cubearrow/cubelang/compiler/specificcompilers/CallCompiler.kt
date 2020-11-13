package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.UsualErrorMessages
import kotlin.math.max

class CallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Call> {
    override fun accept(expression: Expression.Call): String {
        val function = context.functions[expression.identifier1.substring]
        if (function != null) {
            context.argumentIndex = 0
            val args = getFunctionCallArguments(expression, function)
            return "${args}call ${expression.identifier1.substring}"
        }
        UsualErrorMessages.xNotFound("called function", expression.identifier1)
        return ""
    }

    private fun getFunctionCallArguments(call: Expression.Call, function: Compiler.Function): String {
        var args = ""
        for (i in 0 until call.expressionLst1.size) {
            val argumentExpression = call.expressionLst1[i]
            val expectedArgumentType = function.args[function.args.keys.elementAt(i)] ?: error("Unreachable")
            val argumentLength = Compiler.LENGTHS_OF_TYPES[expectedArgumentType]
            val axRegister = CompilerUtils.getRegister("ax", argumentLength)
            args += if (argumentLength >= 4 || argumentExpression is Expression.Literal) {
                getHigherSizedArgument(argumentLength, argumentExpression, axRegister)
            } else {
                getLowSizedArgument(argumentExpression, axRegister)
            }
        }
        return args
    }

    private fun getHigherSizedArgument(argumentLength: Int, argumentExpression: Expression, axRegister: String): String {
        val argumentLength1 = max(argumentLength, 4)
        val baseString = "mov ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[context.argumentIndex++], argumentLength1)}, "
        return if (argumentExpression is Expression.Literal || argumentExpression is Expression.VarCall) {
            "$baseString${argumentExpression.accept(context.compilerInstance)} \n"
        } else {
            "${argumentExpression.accept(context.compilerInstance)} \n" +
                    "$baseString$axRegister \n"
        }
    }

    private fun getLowSizedArgument(argumentExpression: Expression, axRegister: String): String {
        return if (argumentExpression is Expression.VarCall) {
            "mov $axRegister, ${argumentExpression.accept(context.compilerInstance)} \n" +
                    "movsx ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[context.argumentIndex++], 8)}, $axRegister\n"
        } else {
            "${argumentExpression.accept(context.compilerInstance)} \n" +
                    "movsx ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[context.argumentIndex++], 8)}, $axRegister\n"
        }
    }
}