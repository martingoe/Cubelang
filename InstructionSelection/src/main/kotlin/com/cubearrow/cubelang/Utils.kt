package com.cubearrow.cubelang

import com.cubearrow.cubelang.common.Expression

class Utils {
    companion object {
        /**
         * Splits the length of a struct in a set of lenghts of 2^n where 1 <= n <= 3.
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
                is Expression.ValueToPointer -> {
                    if (index == 0)
                        parent.pointer = newExpression
                    else if (index == 1)
                        parent.value = newExpression
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
            }
            // TODO("Children missing?")
        }

        fun getChildren(expression: Expression): List<Expression> {
            when (expression) {
                is Expression.Operation -> {
                    return listOf(expression.leftExpression, expression.rightExpression)
                }
                is Expression.Logical -> {
                    return listOf(expression.leftExpression, expression.rightExpression)
                }
                is Expression.Grouping -> {
                    return listOf(expression.expression)
                }
                is Expression.ValueFromPointer -> {
                    return listOf(expression.expression)
                }
                is Expression.Unary -> {
                    return listOf(expression.expression)
                }
                is Expression.ValueToPointer -> {
                    // TODO: Left side
                    return listOf(expression.pointer, expression.value)
                }
                is Expression.Assignment -> {
                    return listOf(expression.leftSide, expression.valueExpression)
                }
                is Expression.Comparison -> {
                    return listOf(expression.leftExpression, expression.rightExpression)
                }
            }
            // TODO: Are children missing?
            return listOf()
        }
    }
}