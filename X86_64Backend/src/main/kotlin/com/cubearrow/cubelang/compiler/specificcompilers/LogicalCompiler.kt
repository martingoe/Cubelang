package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.tokens.TokenType
import com.cubearrow.cubelang.compiler.CompilerContext

class LogicalCompiler(val context: CompilerContext): SpecificCompiler<Expression.Logical> {
    override fun accept(expression: Expression.Logical): String {
        if(!context.inJmpCondition)
            context.error(expression.logical.index, expression.logical.index, "Cannot compile assigning logical expressions yet.")
        if(expression.logical.tokenType == TokenType.AND){
            if(context.inJmpCondition){
                return context.evaluate(expression.leftExpression) + context.evaluate(expression.rightExpression) + "\n"
            }
        }
        if(expression.logical.tokenType == TokenType.OR){
            if(context.inJmpCondition){
                context.isOR = true
                val x =  context.evaluate(expression.leftExpression)
                context.isOR = false
                return x + context.evaluate(expression.rightExpression) + "\n"
            }
        }
        return ""
    }
}