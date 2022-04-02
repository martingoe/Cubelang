package com.martingoe.cubelang.backend.instructionselection

import com.martingoe.cubelang.common.Expression

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