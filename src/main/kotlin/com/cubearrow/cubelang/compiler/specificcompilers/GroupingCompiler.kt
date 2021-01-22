package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.parser.Expression

class GroupingCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Grouping> {
    override fun accept(expression: Expression.Grouping): String {
        val moveInformation = context.moveExpressionToX(expression.expression)
        return moveInformation.moveTo(getRegister("ax", moveInformation.type.getRawLength()))
    }
}