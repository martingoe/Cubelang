package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.parser.Expression

class ExpressionUtils {
    companion object {
        fun mapVarCallsToStrings(expressions: List<Expression>): List<String> {
            return expressions.map { (it as Expression.VarCall).identifier1.substring }
        }
    }
}