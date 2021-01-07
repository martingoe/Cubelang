package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getASMPointerLength
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.CommonErrorMessages
import kotlin.math.max

/**
 *
 */
class CallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Call> {
    override fun accept(expression: Expression.Call): String {
        if (expression.callee is Expression.VarCall) {
            val varCall = expression.callee
            val function = context.getFunction(expression.callee.varName.substring, expression.arguments.size)
            if (function != null) {
                context.argumentIndex = 0
                val args = getFunctionCallArguments(expression, function)
                return "${args}call ${varCall.varName.substring}"
            }
            CommonErrorMessages.xNotFound("called function", varCall.varName)
        }
        return ""
    }

    private fun getFunctionCallArguments(call: Expression.Call, function: Compiler.Function): String {
        var args = ""
        val laterArgs: MutableMap<Int, Expression> = HashMap()
        for (i in call.arguments.indices) {
            val argumentExpression = call.arguments[i]
            if (argumentExpression !is Expression.Call) {
                laterArgs[i] = argumentExpression
                continue
            }
            args += getSingleArgument(function, i, argumentExpression)
        }

        for (entry in laterArgs) {
            val i = entry.key
            val argumentExpression = call.arguments[i]
            args += getSingleArgument(function, i, argumentExpression)
        }
        return args
    }

    private fun getSingleArgument(function: Compiler.Function, index: Int, argumentExpression: Expression): String {
        val expectedArgumentType = function.args[function.args.keys.elementAt(index)] ?: error("Unreachable")
        val triple = context.moveExpressionToX(argumentExpression)
        checkMatchingTypes(expectedArgumentType, triple.third, -1, -1)
        if(triple.third.getRawLength() < 4 && argumentExpression !is Expression.Literal){
            return "${triple.first}\n" +
                    "movsx ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[index]!!, 4)}, ${getASMPointerLength(triple.third.getRawLength())} ${triple.second}\n"
        }
        return "${triple.first}\n" +
                "mov ${CompilerUtils.getRegister(Compiler.ARGUMENT_INDEXES[index]!!, max(4, triple.third.getRawLength()))}, ${triple.second}\n"
    }
}