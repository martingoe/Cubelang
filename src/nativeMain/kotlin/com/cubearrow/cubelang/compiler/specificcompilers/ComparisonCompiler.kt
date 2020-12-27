package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.NormalType

class ComparisonCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Comparison> {
    override fun accept(expression: Expression.Comparison): String {
        return if (context.inJmpCondition) {
            """${getBase(expression)}
                        |${CompilerUtils.getComparisonOperation(expression.comparator.substring)} .L${context.lIndex}
                    """.trimMargin()
        } else {
            context.operationResultType = NormalType("char")
            getBase(expression) + "\n" + getMoveComparisonOperator(expression.comparator.substring) + " al"
        }
    }

    private fun getMoveComparisonOperator(comparator: String): String {
        return when (comparator) {
            "==" -> "sete"
            "!=" -> "setne"
            "<" -> "setl"
            "<=" -> "setle"
            ">" -> "setg"
            ">=" -> "setge"
            else -> error("Comparison operator not found")
        }
    }

    private fun getBase(expression: Expression.Comparison): String {
        val right = CompilerUtils.moveExpressionToX(expression.rightExpression, context)
        val left = CompilerUtils.moveExpressionToX(expression.leftExpression, context)
        checkMatchingTypes(right.third, left.third)
        val rightRegister = getRegister("bx", right.third.getRawLength())
        val leftRegister = getRegister("ax", left.third.getRawLength())

        return """push rbx
                    |${left.first}${if (!left.second.endsWith("ax")) "\nmov $leftRegister, " + left.second else ""}
                    |${right.first}
                    |mov $rightRegister, ${right.second}
                    |cmp $leftRegister, $rightRegister
                    |pop rbx
                    """.trimMargin()
    }
}