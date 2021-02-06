package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getASMPointerLength
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.CommonErrorMessages
import com.cubearrow.cubelang.utils.NormalType
import kotlin.math.max

/**
 *
 */
class CallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Call> {
    var registerIndex = 0
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

    private fun getSingleArgument(function: Compiler.Function, argumentIndex: Int, argumentExpression: Expression): String {
        val expectedArgumentType = function.args[function.args.keys.elementAt(argumentIndex)] ?: error("Unreachable")

        if(expectedArgumentType is NormalType && !Compiler.PRIMARY_TYPES.contains(expectedArgumentType.typeName))
            return moveStruct(expectedArgumentType, argumentExpression)
        val moveInformation = context.moveExpressionToX(argumentExpression)
        checkMatchingTypes(expectedArgumentType, moveInformation.type, -1, -1)
        for (i in CompilerUtils.splitLengthIntoRegisterLengths(moveInformation.type.getLength()))
        if(moveInformation.type.getRawLength() < 4 && argumentExpression !is Expression.Literal){
            return "${moveInformation.before}\n" +
                    "movsx ${getRegister(Compiler.ARGUMENT_INDEXES[registerIndex++]!!, 4)}, ${getASMPointerLength(moveInformation.type.getRawLength())} ${moveInformation.pointer}\n"
        }
        return moveInformation.moveTo(getRegister(Compiler.ARGUMENT_INDEXES[registerIndex++]!!, max(4, moveInformation.type.getRawLength())))
    }

    private fun moveStruct(expectedArgumentType: NormalType, expression: Expression): String {
        if(expression !is Expression.VarCall) {
            Main.error(-1, -1, "Expected a variable call when passing structs.")
            return ""
        }
        val variable = context.getVariable(expression.varName.substring)!!
        val sizes = CompilerUtils.splitLengthIntoRegisterLengths(expectedArgumentType.getLength())
        var indexRemoved = 0
        var resultingString = ""
        for (size in sizes) {
            for (times in 0 until size.second) {
                resultingString += "mov ${getRegister(Compiler.ARGUMENT_INDEXES[registerIndex++]!!, size.first)}, ${getASMPointerLength(size.first)} [rbp - ${variable.index - indexRemoved}]\n"
                indexRemoved += size.first
            }
        }
        return resultingString
    }
}