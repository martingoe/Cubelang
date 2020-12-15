package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.lexer.Token
import Main
import com.cubearrow.cubelang.parser.Expression
import kotlin.math.max


class CompilerUtils {
    companion object {
        fun getOperationDepth(expression: Expression): Int{
            return when (expression){
                is Expression.Operation -> max(getOperationDepth(expression.expression1), getOperationDepth(expression.expression2)) + 1
                is Expression.Comparison -> max(getOperationDepth(expression.expression1), getOperationDepth(expression.expression2))
                is Expression.Grouping -> getOperationDepth(expression.expression1)
                is Expression.Call -> expression.expressionLst1.fold(0) {acc, arg -> max(acc, getOperationDepth(arg)) }
                is Expression.Unary -> getOperationDepth(expression.expression1)
                is Expression.Logical -> max(getOperationDepth(expression.expression1), getOperationDepth(expression.expression2))
                else -> 0
            }
        }

        fun moveAXToVariable(length: Int, context: CompilerContext): String =
                "mov ${getASMPointerLength(length)} [rbp - ${context.stackIndex.last()}], ${getRegister("ax", length)}"

        fun assignVariableToVariable(variableToAssignTo: Compiler.LocalVariable, variableToAssign: Compiler.LocalVariable): String {
            val length = Compiler.LENGTHS_OF_TYPES[variableToAssignTo.type]!!
            val register = getRegister("ax", length)
            return """
                |mov $register, ${getASMPointerLength(length)} [rbp - ${variableToAssign.index}]
                |mov ${getASMPointerLength(length)} [rbp - ${variableToAssignTo.index}], $register
            """.trimMargin()
        }

        fun checkMatchingTypes(type1: Token, type2: String) {
            if (type1.substring != type2) Main.error(type1.line, type1.index, null, "The types do not match")
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

        fun getComparisonOperation(comparator: String): String {
            return when (comparator) {
                "==" -> "jne"
                "!=" -> "je"
                "<" -> "jge"
                "<=" -> "jg"
                ">" -> "jle"
                ">=" -> "jl"
                else -> error("Comparison operator not expected")
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