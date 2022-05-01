package com.martingoe.cubelang.backend

import com.martingoe.cubelang.common.Expression
import com.martingoe.cubelang.common.NormalType
import com.martingoe.cubelang.common.NormalTypes
import com.martingoe.cubelang.common.Type


/**
 * Returns the inverse of the jmp operation requested by the comparison string.
 *
 * e.g: == -> jne
 */
internal fun getInvJumpOperationFromComparator(comparisonString: String): String {
    return when (comparisonString) {
        "==" -> "jne"
        "!=" -> "je"
        "<" -> "jge"
        "<=" -> "jg"
        ">" -> "jle"
        ">=" -> "jl"
        else -> error("Could not find the requested operation")
    }
}

/**
 * Returns the jmp operation appropriate for the comparison string
 *
 * e.g: == -> leq
 */
internal fun getJmpOperationFromComparator(comparisonString: String): String {
    return when (comparisonString) {
        "==" -> "jeq"
        "!=" -> "jne"
        "<" -> "jl"
        "<=" -> "jle"
        ">" -> "jg"
        ">=" -> "jge"
        else -> error("Could not find the requested operation")
    }
}

class Utils {
    companion object {
        fun getIntTypeForLength(length: Int): Type {
            return when (length) {
                1 -> NormalType(NormalTypes.I8)
                2 -> NormalType(NormalTypes.I16)
                4 -> NormalType(NormalTypes.I32)
                8 -> NormalType(NormalTypes.I64)

                else -> error("Unknown size")
            }
        }
        /**
         * Splits the length of a struct in a set of lengths of 2^n where 1 <= n <= 3.
         */
        fun splitStruct(structLength: Int): List<Int> {
            var remainder = structLength
            val result = mutableListOf<Int>()
            for (i in arrayOf(8, 4, 2, 1)) {
                for (j in 0 until (remainder / i)) {
                    result.add(i)
                }
                remainder %= i
            }
            return result
        }


        /**
         * Sets the nnh child of an expression to a new expression.
         */
        fun setNthChild(index: Int, newExpression: Expression, parent: Expression) {
            when (parent) {
                is Expression.Operation -> {
                    if (index == 0)
                        parent.leftExpression = newExpression
                    else if (index == 1)
                        parent.rightExpression = newExpression
                }
                is Expression.Logical -> {
                    if (index == 0)
                        parent.leftExpression = newExpression
                    else if (index == 1)
                        parent.rightExpression = newExpression
                }
                is Expression.Grouping -> {
                    if (index == 0)
                        parent.expression = newExpression
                }
                is Expression.ValueFromPointer -> {
                    if (index == 0)
                        parent.expression = newExpression
                }
                is Expression.Unary -> {
                    if (index == 0)
                        parent.expression = newExpression
                }
                is Expression.Assignment -> {
                    if (index == 0)
                        parent.leftSide = newExpression
                    else if (index == 1)
                        parent.valueExpression = newExpression
                }
                is Expression.Comparison -> {
                    if (index == 0)
                        parent.leftExpression = newExpression
                    else if (index == 1)
                        parent.rightExpression = newExpression
                }
                is Expression.ExtendTo64Bit -> {
                    if(index == 0)
                        parent.expression = newExpression
                }
            }
            // TODO("Children missing?")
        }

        /**
         * Returns a list of children of a given expression.
         */
        fun getChildren(expression: Expression): List<Expression> {
            // TODO: Are children missing?
            return when (expression) {
                is Expression.Operation -> {
                    listOf(expression.leftExpression, expression.rightExpression)
                }
                is Expression.Logical -> {
                    listOf(expression.leftExpression, expression.rightExpression)
                }
                is Expression.Grouping -> {
                    listOf(expression.expression)
                }
                is Expression.ValueFromPointer -> {
                    listOf(expression.expression)
                }
                is Expression.Unary -> {
                    listOf(expression.expression)
                }
                is Expression.Assignment -> {
                    listOf(expression.leftSide, expression.valueExpression)
                }
                is Expression.Comparison -> {
                    listOf(expression.leftExpression, expression.rightExpression)
                }
                is Expression.ExtendTo64Bit -> {
                    listOf(expression.expression)
                }
                else -> listOf()
            }
        }
    }
}