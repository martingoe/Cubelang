package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.utils.TypeUtils

/**
 * Compiles a grouping and moves the result to the 'ax' register.
 *
 * @param context The needed [CompilerContext].
 */
class GroupingCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Grouping> {
    override fun accept(expression: Expression.Grouping): String {
        val moveInformation = context.moveExpressionToX(expression.expression)
        return moveInformation.moveTo(getRegister("ax", TypeUtils.getRawLength(moveInformation.type)))
    }
}