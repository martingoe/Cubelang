package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.interpreter.Interpreter
import com.cubearrow.cubelang.interpreter.VariableStorage
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

        fun computeVarInitialization(varInitialization: Expression.VarInitialization, variableStorage: VariableStorage, interpreter: Interpreter) {
            val value = varInitialization.valueExpression?.let { interpreter.evaluate(it) }
            val type = getType(varInitialization.type, value)
            if (varInitialization.valueExpression != null) {
                variableStorage.addVariableToCurrentScope(varInitialization.name.substring, type, value!!)
            } else {
                variableStorage.addVariableToCurrentScope(varInitialization.name.substring, type, null)
            }
        }

        fun getType(type: Type?, value: Any?): Type {
            var valueToCompare = value
            if (value is Expression.Literal) valueToCompare = value.value
            return type ?: when (valueToCompare) {
                        is Int -> NormalType("int")
                        is Double -> NormalType("double")
                        is String -> NormalType("string")
                        is Char -> NormalType("char")
                        //is ClassInstance -> valueToCompare.className
                        null -> NormalType("any")
                        else -> NormalType("any")
                    }
        }
    }
}