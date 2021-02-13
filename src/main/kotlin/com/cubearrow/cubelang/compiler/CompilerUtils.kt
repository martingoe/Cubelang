package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.Type
import kotlin.math.max
import kotlin.math.pow


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

        /**
         * Returns a [Token] to eg. return an error from in an [Expression.ArrayGet] by getting to the [Expression.VarCall].
         *
         * @param expression The expression to get the [Token] from. Is supposed to be either [Expression.ArrayGet] or [Expression.VarCall]
         * @throws Error Throws an error if the passed expression is not valid.
         */
        fun getTokenFromArrayGet(expression: Expression): Token {
            return when (expression) {
                is Expression.VarCall -> expression.varName
                is Expression.ArrayGet -> getTokenFromArrayGet(expression.expression)
                else -> error("Inappropriate expression.")
            }
        }

        /**
         * Returns the asm code to assign an existing variable to a variable.
         *
         * @param variableToAssign The variable to be assigned to the other one.
         * @param variableToAssignTo The variable to be assigned to.
         *
         * @return Returns the required x86_64 NASM code.
         */
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

        /**
         * Checks if the given types match. If not, an error is thrown on the given line and index.
         */
        fun checkMatchingTypes(expected: Type?, actual: Type?, line: Int = -1, index: Int = -1, context: CompilerContext) {
            if (expected != actual) context.error(line, index, "The types do not match: $expected and $actual")
        }


        /**
         * Returns the ASM pointer size for getting a value from a pointer.
         */
        fun getASMPointerLength(length: Int): String {
            return when (length) {
                1 -> "BYTE"
                2 -> "WORD"
                4 -> "DWORD"
                8 -> "QWORD"
                else -> error("Unknown byte size")
            }
        }

        /**
         * Splits a length into the minimal amount of registers.
         * @param length The length to split up.
         * @return Returns a [List] of [Pair]s where the first element is the size and the second element is the amount of that size.
         */
        fun splitLengthIntoRegisterLengths(length: Int): List<Pair<Int, Int>> {
            var remainder = length
            val resultingMap: MutableList<Pair<Int, Int>> = ArrayList()
            for (i in 3 downTo 0) {
                val size = 2.0.pow(i).toInt()
                resultingMap.add(Pair(size, remainder / size))
                remainder %= size
            }
            return resultingMap
        }


        /**
         * Returns the register for the appropriate name and length.
         */
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