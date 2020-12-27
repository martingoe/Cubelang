package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression

class ArgumentDefinitionCompiler(var context: CompilerContext): SpecificCompiler<Expression.ArgumentDefinition> {
    override fun accept(expression: Expression.ArgumentDefinition): String {
        val length: Int = expression.type.getLength()

        context.stackIndex.add(context.stackIndex.removeLast() + length)
        context.variables.last()[expression.name.substring] = Compiler.LocalVariable(context.stackIndex.last(), expression.type)

        val register = Compiler.ARGUMENT_INDEXES[context.argumentIndex++]!!
        return if (length > 2) {
            "mov ${CompilerUtils.getASMPointerLength(length)}[rbp - ${context.stackIndex.last()}], ${CompilerUtils.getRegister(register, length)}"
        } else {
            "mov eax, ${CompilerUtils.getRegister(register, 4)}\n" +
                    "mov ${CompilerUtils.getASMPointerLength(expression.type.getRawLength())}[rbp - ${context.stackIndex.last()}], ${CompilerUtils.getRegister("ax", length)}"
        }
    }
}