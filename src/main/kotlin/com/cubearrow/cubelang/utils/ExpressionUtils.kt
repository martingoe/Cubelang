package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.parser.Expression

class ExpressionUtils {
    companion object {
        /**
         * Maps a [List] of [Expression] which may only contain [Expression.VarCall] to their substrings
         *
         * @throws TypeCastException Throws this exception when one of the elements of the expressions are not a [Expression.VarCall]
         * @param expressions The expressions whose names are to be returned
         * @return Returns a [List] of [String]s with the substrings of the identifier of the [Expression.VarCall]
         */
        fun mapVarCallsToStrings(expressions: List<Expression>): List<String> {
            return expressions.map { (it as Expression.VarCall).identifier1.substring }
        }
    }
}