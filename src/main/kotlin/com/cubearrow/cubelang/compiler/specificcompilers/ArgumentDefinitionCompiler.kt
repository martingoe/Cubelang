package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression

/**
 * Compiles a single argument in a function by moving from the required register to a new variable with the correct name.
 *
 * @param context The needed [CompilerContext].
 */
class ArgumentDefinitionCompiler(var context: CompilerContext): SpecificCompiler<Expression.ArgumentDefinition> {
    override fun accept(expression: Expression.ArgumentDefinition): String {
        context.stackIndex.add(context.stackIndex.removeLast() + expression.type.getLength())
        context.variables.last()[expression.name.substring] = Compiler.LocalVariable(context.stackIndex.last(), expression.type)

        val rawLength = expression.type.getRawLength()
        val register = Compiler.ARGUMENT_INDEXES[context.argumentIndex++]!!
        return if (rawLength > 2) {
            "mov ${CompilerUtils.getASMPointerLength(rawLength)}[rbp - ${context.stackIndex.last()}], ${CompilerUtils.getRegister(register, rawLength)}"
        } else {
            "mov eax, ${CompilerUtils.getRegister(register, 4)}\n" +
                    "mov ${CompilerUtils.getASMPointerLength(expression.type.getRawLength())}[rbp - ${context.stackIndex.last()}], ${CompilerUtils.getRegister("ax", rawLength)}"
        }
    }
}