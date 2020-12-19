package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression

class ComparisonCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Comparison> {
    override fun accept(expression: Expression.Comparison): String {
        if (expression.leftExpression is Expression.VarCall && context.inJmpCondition) {
            if (expression.rightExpression is Expression.Literal || expression.rightExpression is Expression.VarCall) {
                return "cmp ${expression.leftExpression.accept(context.compilerInstance)}, " +
                        "${expression.rightExpression.accept(context.compilerInstance)}\n" +
                        CompilerUtils.getComparisonOperation(expression.comparator.substring) + " .L${context.lIndex}"
            }
        }
        TODO("Not yet implemented")
    }
}