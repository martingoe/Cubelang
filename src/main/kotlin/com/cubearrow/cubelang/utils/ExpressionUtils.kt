package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.interpreter.ClassInstance
import com.cubearrow.cubelang.interpreter.Interpreter
import com.cubearrow.cubelang.interpreter.VariableStorage
import com.cubearrow.cubelang.lexer.Token
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

        fun computeVarInitialization(varInitialization: Expression.VarInitialization, variableStorage: VariableStorage, interpreter: Interpreter){
            val type = getType(varInitialization.identifierNull1, varInitialization.expressionNull1)
            if(varInitialization.expressionNull1 != null) {
                variableStorage.addVariableToCurrentScope(varInitialization.identifier1.substring, type, interpreter.evaluate(varInitialization.expressionNull1!!))
            } else{
                variableStorage.addVariableToCurrentScope(varInitialization.identifier1.substring, type, null)
            }
        }

        fun getType(type: Token?, value: Any?): String {
            var valueToCompare = value
            if(value is Expression.Literal) valueToCompare = value.any1
            return type?.substring?.toLowerCase()
                    ?: when (valueToCompare){
                        is Int -> "int"
                        is Double -> "double"
                        is String -> "string"
                        is ClassInstance -> valueToCompare.className
                        null -> "any"
                        else -> "any"
                    }
        }
    }
}