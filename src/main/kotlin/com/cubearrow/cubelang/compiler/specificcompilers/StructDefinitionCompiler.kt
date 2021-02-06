package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.Type

class StructDefinitionCompiler(val context: CompilerContext): SpecificCompiler<Expression.StructDefinition> {
    override fun accept(expression: Expression.StructDefinition): String {
        val length = expression.body.fold(0, { acc, varInitialization -> acc + varInitialization.type!!.getLength()})
        Compiler.lengthsOfTypes[expression.name.substring] = length
        context.structs[expression.name.substring] = Compiler.Struct(expression.name.substring,
            expression.body.map { it.name.substring to it.type!! } as MutableList<Pair<String, Type>>)
        return ""
    }
}