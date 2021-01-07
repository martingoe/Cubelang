package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.isAXRegister
import com.cubearrow.cubelang.parser.Expression

class GroupingCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Grouping> {
    override fun accept(expression: Expression.Grouping): String {
        val triple = context.moveExpressionToX(expression.expression)
        return if (triple.first.isNotBlank()) triple.first + "\n" else "" +
                if (!isAXRegister(triple.second)) "mov ${getRegister("ax", triple.third.getRawLength())}, ${triple.second}" else ""
    }
}