package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.common.Type
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

    private fun moveValue(rawLength: Int, index: Int): String {
        val register = Compiler.ARGUMENT_INDEXES[context.argumentIndex]
        return if (rawLength > 2) {
            "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${index}], ${CompilerUtils.getRegister(register, rawLength)}\n"
        } else {
            "mov eax, ${CompilerUtils.getRegister(register, 4)}\n" +
                    "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${index}], ${CompilerUtils.getRegister("ax", rawLength)}\n"
        }
    }

    private fun moveValue(type: Type): String {
        val sizes = CompilerUtils.splitLengthIntoRegisterLengths(TypeUtils.getLength(type))
        var indexRemoved = 0
        var resultingString = ""
        for (size in sizes) {
            for (times in 0 until size.second) {
                resultingString += moveValue(size.first, context.stackIndex.last() - indexRemoved)
                indexRemoved += size.first
            }
        }
        return resultingString
    }

}