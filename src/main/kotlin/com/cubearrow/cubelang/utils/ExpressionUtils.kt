package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.parser.Expression

class ExpressionUtils {
    companion object {
        /**
         * Maps a [List] of [Expression] which may only contain [Expression.ArgumentDefinition] to their substrings
         *
         * @throws TypeCastException Throws this exception when one of the elements of the expressions are not a [Expression.VarCall]
         * @param expressions The expressions whose names are to be returned
         * @return Returns a [Map] of [String]s mapped to [String]s with the substrings of the identifier of the [Expression.ArgumentDefinition]
         */
        fun mapArgumentDefinitions(expressions: List<Expression>): Map<String, Type> {
            return expressions.associate { Pair((it as Expression.ArgumentDefinition).name.substring, it.type) }
        }


    }
}