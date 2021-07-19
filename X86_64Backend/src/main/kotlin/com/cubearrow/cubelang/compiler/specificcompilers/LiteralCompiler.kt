package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.common.NormalType

/**
 * Returns the literal value from the parsed [Expression.Literal]. and updates the [CompilerContext.operationResultType]
 *
 * @param context The needed [CompilerContext].
 */
class LiteralCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Literal> {
    override fun accept(expression: Expression.Literal): String {
        if (expression.value is Int) {
            context.operationResultType = NormalType("i32")
            return expression.value.toString()
        } else if (expression.value is Char) {
            context.operationResultType = NormalType("char")
            return (expression.value as Char).toByte().toInt().toString()
        }
        TODO("Not yet implemented")
    }
}