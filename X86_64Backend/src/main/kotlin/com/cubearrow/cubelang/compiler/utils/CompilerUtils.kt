package com.cubearrow.cubelang.compiler.utils

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
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
         * Checks if the given types match. If not, an error is thrown on the given line and index.
         */
        fun checkMatchingTypes(expected: Type?, actual: Type?, line: Int = -1, index: Int = -1, context: CompilerContext) {
            if (expected != actual) context.error(line, index, "The types do not match: $expected and $actual")
        }

        private fun pushPopRegisters(baseString: String, registers: List<String>): String {
            var result = baseString
            for (i in registers) {
                result = "push r${i}\n${result}pop r${i}\n"
            }
            return result
        }
        fun setVariableToStructFromPointer(
            pointerType: PointerType,
            localVariable: Compiler.LocalVariable
        ): String {
            val splitLengths = splitLengthIntoRegisterLengths(TypeUtils.getLength(pointerType.subtype))
            var adder = 0
            var result = ""
            var registersUsed = 0
            for (length in splitLengths) {
                for (times in 0 until length.second) {
                    val register = getRegister(
                        Compiler.GENERAL_PURPOSE_REGISTERS[Compiler.GENERAL_PURPOSE_REGISTERS.size - registersUsed - 1],
                        length.first
                    )

                    result +=  moveLocationToLocation("${getASMPointerLength(length.first)} [rbp-${localVariable.index - adder}]",
                        "${getASMPointerLength(length.first)} [rax + $adder]", register)
                    registersUsed++
                    adder += length.first
                }
            }
            return pushPopRegisters(result, Compiler.GENERAL_PURPOSE_REGISTERS.subList(Compiler.GENERAL_PURPOSE_REGISTERS.size - registersUsed - 1, Compiler.GENERAL_PURPOSE_REGISTERS.size - 1))
        }
        fun getPointerTypeFromValueFromPointer(valueFromPointer: Expression.ValueFromPointer, context: CompilerContext): PointerType {
            val innerMoveInformation = context.moveExpressionToX(valueFromPointer.expression)
            if (innerMoveInformation.type !is PointerType) {
                context.error(-1, -1, "Expected a pointer type when getting the value from a type.")
            }
            return innerMoveInformation.type as PointerType
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

        fun moveLocationToLocation(locationToMoveTo: String, locationToMove: String, register: String): String{
            return "mov $register, $locationToMove \n" +
            "mov ${locationToMoveTo}, $register \n"
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