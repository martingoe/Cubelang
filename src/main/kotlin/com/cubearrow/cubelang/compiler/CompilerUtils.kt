package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.Type
import kotlin.math.max


class CompilerUtils {
    companion object {

        /**
         * Returns the maximum mathematical operation depth in an expression.
         * This recursively accounts for arguments in calls etc. and is used to evaluate how many registers need to be pushed onto the stack.
         *
         * @param expression The expression to be evaluated
         */
        fun getOperationDepth(expression: Expression): Int {
            return when (expression) {
                is Expression.Operation -> max(
                    getOperationDepth(expression.leftExpression),
                    getOperationDepth(expression.rightExpression)
                ) + 1
                is Expression.Comparison -> max(
                    getOperationDepth(expression.leftExpression),
                    getOperationDepth(expression.rightExpression)
                )
                is Expression.Grouping -> getOperationDepth(expression.expression)
                is Expression.Call -> expression.arguments.fold(0) { acc, arg -> max(acc, getOperationDepth(arg)) }
                is Expression.Unary -> getOperationDepth(expression.expression)
                is Expression.Logical -> max(
                    getOperationDepth(expression.leftExpression),
                    getOperationDepth(expression.rightExpression)
                )
                else -> 0
            }
        }

        fun getTokenFromArrayGet(expression: Expression): Token {
            return when(expression){
                is Expression.VarCall -> expression.varName
                is Expression.ArrayGet -> getTokenFromArrayGet(expression.expression)
                else -> error("unreachable")
            }
        }

        fun assignVariableToVariable(
            variableToAssignTo: Compiler.LocalVariable,
            variableToAssign: Compiler.LocalVariable
        ): String {
            val length = variableToAssign.type.getLength()
            val register = getRegister("ax", length)
            return """
                |mov $register, ${getASMPointerLength(length)} [rbp - ${variableToAssign.index}]
                |mov ${getASMPointerLength(length)} [rbp - ${variableToAssignTo.index}], $register
            """.trimMargin()
        }

        fun checkMatchingTypes(type: Type?, type2: Type?, line: Int = -1, index: Int = -1) {
            if (type != type2) Main.error(line, index, "The types do not match: $type and $type2")
        }


        fun getASMPointerLength(length: Int): String {
            return when (length) {
                1 -> "BYTE"
                2 -> "WORD"
                4 -> "DWORD"
                8 -> "QWORD"
                else -> error("Unknown byte size")
            }
        }



        fun getOperator(operatorString: String): String {
            return when (operatorString) {
                "+" -> "add"
                "-" -> "sub"
                "*" -> "mul"
                "/" -> "div"
                else -> error("Unexpected operator")
            }
        }

        fun getRegister(baseName: String, length: Int): String {
            return try {
                baseName.toInt()
                when (length) {
                    8 -> "r$baseName"
                    4 -> "r${baseName}d"
                    2 -> "r${baseName}w"
                    1 -> "r${baseName}b"
                    else -> ""
                }
            } catch (e: NumberFormatException) {
                when (length) {
                    8 -> "r${baseName}"
                    4 -> "e${baseName}"
                    2 -> "${baseName[0]}h"
                    1 -> "${baseName[0]}l"
                    else -> ""
                }
            }
        }

    }
}