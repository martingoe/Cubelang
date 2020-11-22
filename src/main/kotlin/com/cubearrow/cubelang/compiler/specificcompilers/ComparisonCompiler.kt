package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression

class ComparisonCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Comparison> {
    override fun accept(expression: Expression.Comparison): String {
        if (expression.expression1 is Expression.VarCall && context.inJmpCondition) {
            if (expression.expression2 is Expression.Literal || expression.expression2 is Expression.VarCall) {
                return "cmp ${expression.expression1.accept(context.compilerInstance)}, " +
                        "${expression.expression2.accept(context.compilerInstance)}\n" +
                        CompilerUtils.getComparisonOperation(expression.comparator1.substring) + " .L${context.lIndex}"
            }
        }
        TODO("Not yet implemented")
    }
}