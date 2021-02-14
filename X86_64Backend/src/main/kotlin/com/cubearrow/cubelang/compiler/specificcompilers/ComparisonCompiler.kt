package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.compiler.utils.TypeUtils

/**
 * Compiles comparisons with the cmp command in asm.
 */
class ComparisonCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Comparison> {
    override fun accept(expression: Expression.Comparison): String {
        return if (context.inJmpCondition) {
            if (context.isOR) {
                "${getBase(expression)}\n" +
                        "${getOrComparisonOperation(expression.comparator.substring)} .L${context.lIndex - 1}\n"
            } else "${getBase(expression)}\n" +
                    "${getComparisonOperation(expression.comparator.substring)} .L${context.lIndex}\n"
        } else {
            context.operationResultType = NormalType("short")
            getBase(expression) + "\n" + getMoveComparisonOperator(expression.comparator.substring) + " al"
        }
    }

    private fun getComparisonOperation(comparator: String): String {
        return when (comparator) {
            "==" -> "jne"
            "!=" -> "je"
            "<" -> "jge"
            "<=" -> "jg"
            ">" -> "jle"
            ">=" -> "jl"
            else -> error("Comparison operator not expected")
        }
    }

    private fun getOrComparisonOperation(comparator: String): String {
        return when (comparator) {
            "==" -> "je"
            "!=" -> "jne"
            "<" -> "jl"
            "<=" -> "jle"
            ">" -> "jg"
            ">=" -> "jge"
            else -> error("Comparison operator not expected")
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
        val right = context.moveExpressionToX(expression.rightExpression)
        val left = context.moveExpressionToX(expression.leftExpression)
        checkMatchingTypes(right.type, left.type, -1, -1, context)
        val rightRegister = getRegister("bx", TypeUtils.getLength(right.type))
        val leftRegister = getRegister("ax", TypeUtils.getLength(left.type))

        return """push rbx
            |${left.moveTo(leftRegister)}
            |${right.moveTo(rightRegister)}
            |cmp $leftRegister, $rightRegister
            |pop rbx
            """.trimMargin()
    }
}