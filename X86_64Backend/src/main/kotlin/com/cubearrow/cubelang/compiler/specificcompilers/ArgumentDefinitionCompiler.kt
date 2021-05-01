package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.TypeUtils

/**
 * Compiles a single argument in a function by moving from the required register to a new variable with the correct name.
 *
 * @param context The needed [CompilerContext].
 */
class ArgumentDefinitionCompiler(var context: CompilerContext) : SpecificCompiler<Expression.ArgumentDefinition> {
    override fun accept(expression: Expression.ArgumentDefinition): String {
        context.stackIndex.add(context.stackIndex.removeLast() + TypeUtils.getLength(expression.type))
        context.variables.last()[expression.name.substring] = Compiler.LocalVariable(context.stackIndex.last(), expression.type)

        return moveValue(expression.type)
    }


    private fun moveSingleNonPushedValue(rawLength: Int, index: Int): String {
        val register = Compiler.ARGUMENT_REGISTERS[context.argumentIndex++]
        return if (rawLength > 2) {
            "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${index}], ${CompilerUtils.getRegister(register, rawLength)}\n"
        } else {
            "mov eax, ${CompilerUtils.getRegister(register, 4)}\n" +
                    "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${index}], ${CompilerUtils.getRegister("ax", rawLength)}\n"
        }
    }

    private fun moveValue(type: Type): String {
        val sizes = CompilerUtils.splitLengthIntoRegisterLengths(TypeUtils.getLength(type))
        var negativeIndex = 0
        var positiveIndex = 16
        var resultingString = ""
        for (size in sizes) {
            for (times in 0 until size.second) {
                if (hasAvailableRegisters()) {
                    resultingString += moveSingleNonPushedValue(size.first, context.stackIndex.last() - negativeIndex)
                } else {
                    resultingString += moveSinglePushedValue(size.first, context.stackIndex.last() - negativeIndex, positiveIndex)
                    positiveIndex += 8
                }
                negativeIndex += size.first
            }
        }
        return resultingString
    }

    private fun hasAvailableRegisters() = context.argumentIndex < Compiler.ARGUMENT_REGISTERS.size

    private fun moveSinglePushedValue(rawLength: Int, index: Int, oldIndex: Int): String {
        val register = CompilerUtils.getRegister("ax", rawLength)
        return "mov $register, ${CompilerUtils.getASMPointerLength(rawLength)}[rbp + ${oldIndex}]\n" +
                "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${index}], $register\n"
    }

}