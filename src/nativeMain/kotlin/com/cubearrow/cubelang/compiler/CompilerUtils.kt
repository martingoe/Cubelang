package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.parser.Expression
import kotlin.math.max


class CompilerUtils {
    companion object {
        fun getOperationDepth(expression: Expression): Int {
            return when (expression) {
                is Expression.Operation -> max(
                    getOperationDepth(expression.expression),
                    getOperationDepth(expression.expression2)
                ) + 1
                is Expression.Comparison -> max(
                    getOperationDepth(expression.expression),
                    getOperationDepth(expression.expression2)
                )
                is Expression.Grouping -> getOperationDepth(expression.expression)
                is Expression.Call -> expression.expressionLst.fold(0) { acc, arg -> max(acc, getOperationDepth(arg)) }
                is Expression.Unary -> getOperationDepth(expression.expression)
                is Expression.Logical -> max(
                    getOperationDepth(expression.expression),
                    getOperationDepth(expression.expression2)
                )
                else -> 0
            }
        }

        fun getVariableFromArrayGet(expression: Expression, context: CompilerContext): Compiler.LocalVariable? {
            if (expression is Expression.ArrayGet) {
                return getVariableFromArrayGet(expression.expression, context)
            } else if (expression is Expression.VarCall) {
                return context.variables.last()[expression.identifier.substring]
            }
            return null
        }

        fun moveAXToVariable(length: Int, context: CompilerContext): String =
            "mov ${getASMPointerLength(length)} [rbp - ${context.stackIndex.last()}], ${getRegister("ax", length)}"

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


        fun checkMatchingTypes(type: Type, type2: Type) {
            if (type != type2) Main.error(-1, -1, null, "The types do not match")
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

        fun moveArrayGetToSth(arrayGet: Expression.ArrayGet, toMoveTo: String, context: CompilerContext): String {
            val (before, pointer) = beforeAndPointerArrayGet(arrayGet, context)
            return "$before\n" +
                    "$toMoveTo $pointer\n"
        }

        fun beforeAndPointerArrayGet(arrayGet: Expression.ArrayGet, context: CompilerContext): Pair<String, String>{
            val before: String
            val pointer: String

            if (arrayGet.expression2 !is Expression.Literal) {
                val string = arrayGet.accept(context.compilerInstance)
                val indexOf = string.indexOf("[", string.indexOf("[") + 1)
                before = string.substring(0 until indexOf)
                pointer = string.substring(indexOf until string.length)
            } else {
                before = ""
                pointer = arrayGet.accept(context.compilerInstance)
            }
            return Pair(before, pointer)
        }


    }
}