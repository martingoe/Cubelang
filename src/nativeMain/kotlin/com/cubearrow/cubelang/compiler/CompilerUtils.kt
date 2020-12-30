package com.cubearrow.cubelang.compiler

import Main
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ArrayType
import com.cubearrow.cubelang.utils.ExpressionUtils.Companion.getType
import com.cubearrow.cubelang.utils.NormalType
import com.cubearrow.cubelang.utils.Type
import com.cubearrow.cubelang.utils.CommonErrorMessages
import kotlin.math.max


class CompilerUtils {
    companion object {
        private val axRegisters = listOf("rax", "eax", "al", "ah")
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

        fun getVariableFromArrayGet(expression: Expression, context: CompilerContext): Compiler.LocalVariable? {
            if (expression is Expression.ArrayGet) {
                return getVariableFromArrayGet(expression.expression, context)
            } else if (expression is Expression.VarCall) {
                return getVariable(expression.varName.substring, context)
            }
            return null
        }

        fun getTokenFromArrayGet(expression: Expression): Token {
            return when(expression){
                is Expression.VarCall -> expression.varName
                is Expression.ArrayGet -> getTokenFromArrayGet(expression.expression)
                else -> error("unreachable")
            }
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


        fun isAXRegister(string: String): Boolean{
            return axRegisters.contains(string)
        }
        fun checkMatchingTypes(type: Type?, type2: Type?, line: Int = -1, index: Int = -1) {
            if (type != type2) Main.error(line, index, "The types do not match: $type and $type2")
        }
        fun getVariable(name: String, context: CompilerContext): Compiler.LocalVariable? {
            return context.variables.reduce { acc, mutableMap ->
                acc.putAll(mutableMap)
                acc
            }[name]
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

        private fun beforeAndPointerArrayGet(arrayGet: Expression.ArrayGet, context: CompilerContext): Triple<String, String, Type> {
            val before: String
            val pointer: String

            if (arrayGet.inBrackets !is Expression.Literal) {
                val string = arrayGet.accept(context.compilerInstance)
                val indexOf = string.indexOf("[", string.indexOf("[") + 1)
                before = string.substring(0 until indexOf)
                pointer = string.substring(indexOf until string.length)
            } else {
                before = ""
                pointer = arrayGet.accept(context.compilerInstance)
            }
            return Triple(before, pointer, context.operationResultType!!)
        }

        private fun getNormalType(type: Type): NormalType {
            if(type is ArrayType)
                return getNormalType(type.subType)
            return type as NormalType
        }

        fun moveExpressionToX(expression: Expression, context: CompilerContext): Triple<String, String, Type> {
            return when (expression) {
                is Expression.VarCall -> {
                    val localVariable = getVariable(expression.varName.substring, context)
                    if (localVariable == null) {
                        CommonErrorMessages.xNotFound("variable", expression.varName)
                        error("")
                    }
                    Triple("", expression.accept(context.compilerInstance), localVariable.type)
                }
                is Expression.ArrayGet -> {
                    beforeAndPointerArrayGet(expression, context)
                }
                is Expression.Call -> {
                    moveCallToX(expression, context)
                }
                is Expression.Grouping, is Expression.Operation, is Expression.Comparison -> {
                    val first = expression.accept(context.compilerInstance)
                    if(context.operationResultType == null){
                        Main.error(-1, -1, "The expression does not return a type.")
                    }
                    Triple(first, getRegister("ax", context.operationResultType!!.getRawLength()), context.operationResultType!!)
                }
                is Expression.Literal -> {
                    val type = getType(null, expression.value)
                    Triple("", expression.accept(context.compilerInstance), type)
                }
                else -> Triple("", "", NormalType("any"))
            }
        }

        private fun moveCallToX(call: Expression.Call, context: CompilerContext, ): Triple<String, String, Type> {
            if (call.callee is Expression.VarCall) {
                val function = context.functions[call.callee.varName.substring]
                if (function == null) {
                    CommonErrorMessages.xNotFound("called function", call.callee.varName)
                    error("Could not find the function")
                }
                if (function.returnType == null) {
                    Main.error(
                        call.callee.varName.line,
                        call.callee.varName.index,
                        "The called function does not return a value."
                    )
                    error("")
                }
                return Triple(
                    call.accept(context.compilerInstance),
                    getRegister("ax", function.returnType!!.getRawLength()),
                    function.returnType!!
                )
            }
            error("Cannot yet compile the requested call")
        }


    }
}