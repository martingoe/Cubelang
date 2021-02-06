package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.Type

/**
 * Compiles a single argument in a function by moving from the required register to a new variable with the correct name.
 *
 * @param context The needed [CompilerContext].
 */
class ArgumentDefinitionCompiler(var context: CompilerContext) : SpecificCompiler<Expression.ArgumentDefinition> {
    override fun accept(expression: Expression.ArgumentDefinition): String {
        context.stackIndex.add(context.stackIndex.removeLast() + expression.type.getLength())
        context.variables.last()[expression.name.substring] = Compiler.LocalVariable(context.stackIndex.last(), expression.type)

        return moveStruct(expression.type)
    }

    private fun moveValue(rawLength: Int, index: Int): String {
        val register = Compiler.ARGUMENT_INDEXES[context.argumentIndex++]!!
        return if (rawLength > 2) {
            "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${index}], ${CompilerUtils.getRegister(register, rawLength)}\n"
        } else {
            "mov eax, ${CompilerUtils.getRegister(register, 4)}\n" +
                    "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${index}], ${CompilerUtils.getRegister("ax", rawLength)}\n"
        }
    }

    private fun moveStruct(type: Type): String {
        val sizes = CompilerUtils.splitLengthIntoRegisterLengths(type.getLength())
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