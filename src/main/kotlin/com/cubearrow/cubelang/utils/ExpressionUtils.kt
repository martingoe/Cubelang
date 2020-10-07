package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.interpreter.ClassInstance
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
        fun mapArgumentDefinitions(expressions: List<Expression>): Map<String, String> {
            return expressions.associate { Pair((it as Expression.ArgumentDefinition).identifier1.substring, it.identifier2.substring) }
        }

        fun computeVarInitialization(varInitialization: Expression.VarInitialization, variableStorage: VariableStorage, interpreter: Interpreter) {
            val value = varInitialization.expressionNull1?.let { interpreter.evaluate(it) }
            val type = getType(varInitialization.identifierNull1?.substring, value)
            if (varInitialization.expressionNull1 != null) {
                variableStorage.addVariableToCurrentScope(varInitialization.identifier1.substring, type, value!!)
            } else {
                variableStorage.addVariableToCurrentScope(varInitialization.identifier1.substring, type, null)
            }
        }

        fun getType(type: String?, value: Any?): String {
            var valueToCompare = value
            if (value is Expression.Literal) valueToCompare = value.any1
            return type?.toLowerCase()
                    ?: when (valueToCompare) {
                        is Int -> "int"
                        is Double -> "double"
                        is String -> "string"
                        is Char -> "char"
                        is ClassInstance -> valueToCompare.className
                        null -> "any"
                        else -> "any"
                    }
        }
    }
}