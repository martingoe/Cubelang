package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CommonErrorMessages
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getASMPointerLength
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.utils.TypeUtils
import kotlin.math.max

/**
 * The compiler needed for calling functions in the source code.
 *
 * The arguments use the appropriate registers and the returned result is stored in the 'ax' register.
 */
class CallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Call> {
    var registerIndex = 0
    var numberOfPushedValues = 0
    override fun accept(expression: Expression.Call): String {
        if (expression.callee is Expression.VarCall) {
            val varCall = expression.callee as Expression.VarCall
            val function = context.getFunction(varCall.varName.substring, expression.arguments.size)
            if (function != null) {
                context.argumentIndex = 0
                val args = compileCallArguments(expression, function)
                return "${args}call ${varCall.varName.substring}\n${if(numberOfPushedValues != 0) "add rsp, ${numberOfPushedValues * 8}" else ""}"
            }
            CommonErrorMessages.xNotFound("called function", varCall.varName, context)
        }
        return ""
    }

    private fun compileCallArguments(call: Expression.Call, function: Function): String {
        val callArguments: MutableMap<Int, Expression> = HashMap()
        var args = compileNonCallArguments(call, callArguments, function)

        for (entry in callArguments) {
            val i = entry.key
            val argumentExpression = call.arguments[i]
            args += getSingleArgument(function, i, argumentExpression)
        }
        return args
    }

    private fun compileNonCallArguments(
        call: Expression.Call,
        excludedArgs: MutableMap<Int, Expression>,
        function: Function
    ): String {
        var result = ""
        for (i in call.arguments.indices) {
            val argumentExpression = call.arguments[i]
            if (argumentExpression !is Expression.Call) {
                excludedArgs[i] = argumentExpression
            } else {
                result += getSingleArgument(function, i, argumentExpression)
            }
        }
        return result
    }

    private fun getSingleArgument(function: Function, argumentIndex: Int, argumentExpression: Expression): String {
        val expectedArgumentType = function.args[function.args.keys.elementAt(argumentIndex)] ?: error("Unreachable")

        if (expectedArgumentType is NormalType && !Compiler.PRIMARY_TYPES.contains(expectedArgumentType.typeName))
            return moveStruct(expectedArgumentType, argumentExpression)
        val moveInformation = context.moveExpressionToX(argumentExpression)
        checkMatchingTypes(expectedArgumentType, moveInformation.type, -1, -1, context)
        return compileSingleArgument(moveInformation.before, moveInformation.pointer, max(4, TypeUtils.getRawLength(moveInformation.type)))
        // TODO check for splitting lengths?

    }

    private fun moveStruct(expectedArgumentType: NormalType, expression: Expression): String {
        if (expression !is Expression.VarCall) {
            context.error(-1, -1, "Expected a variable call when passing structs.")
            return ""
        }
        val variable = context.getVariable(expression.varName.substring)!!
        val sizes = CompilerUtils.splitLengthIntoRegisterLengths(TypeUtils.getLength(expectedArgumentType))
        var indexRemoved = 0
        var resultingString = ""
        for (size in sizes) {
            for (times in 0 until size.second) {
                resultingString += compileSingleArgument(
                    "",
                    "${getASMPointerLength(size.first)} [rbp - ${variable.index - indexRemoved}]",
                    size.first
                )
                indexRemoved += size.first
            }
        }
        return resultingString
    }

    private fun compileSingleArgument(pointer: String, before: String, size: Int): String {
        return if (registerIndex + 1 > Compiler.ARGUMENT_REGISTERS.size) {
            numberOfPushedValues++
            pushSingleArgument(pointer, before)
        } else {
            moveSingleArgument(pointer, before, size)
        }
    }

    private fun moveSingleArgument(before: String, pointer: String, size: Int): String {
        if (size < 4 && !pointer[0].isDigit()) {
            getLineIfNotEmpty(before) +
                    "movsx ${getRegister(Compiler.ARGUMENT_REGISTERS[registerIndex++], 4)}, ${pointer}\n"
        }
        return "${getLineIfNotEmpty(before)}mov ${getRegister(Compiler.ARGUMENT_REGISTERS[registerIndex++], size)}, $pointer\n"
    }

    private fun pushSingleArgument(before: String, pointer: String): String {
        return "${getLineIfNotEmpty(before)}push ${pointer}\n"
    }
    private fun getLineIfNotEmpty(string: String): String{
        return if (string.isNotEmpty())
            string + "\n"
        else ""
    }
}
