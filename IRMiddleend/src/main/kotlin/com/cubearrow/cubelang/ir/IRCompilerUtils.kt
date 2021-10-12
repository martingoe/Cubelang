package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.ir.IRType

fun getIntTypeFromLength(length: Int): NormalType {
    return when (length) {
        1 -> NormalType(NormalTypes.I8)
        2 -> NormalType(NormalTypes.I16)
        4 -> NormalType(NormalTypes.I32)
        8 -> NormalType(NormalTypes.I64)
        else -> error("Could not find an int type for length $length")
    }
}

fun splitStruct(structLength: Int): List<Int> {
    var remainder = structLength
    val result = mutableListOf<Int>()
    for (i in arrayOf(8, 4, 2, 1)) {
        for (j in 0 until (remainder / i)) {
            result.add(i)
        }
        remainder %= i
    }
    return result
}

fun getValueOfLiteral(literal: Expression.Literal): String {
    return if (literal.value is Char)
        (literal.value as Char).code.toString()
    else literal.value.toString()
}

fun getOperationFromString(string: String): IRType {
    return when (string) {
        "+" -> IRType.PLUS_OP
        "-" -> IRType.MINUS_OP
        "*" -> IRType.MUL_OP
        "/" -> IRType.DIV_OP

        else -> error("Could not find the requested operation")
    }
}

fun getInvJumpOperationFromComparator(comparisonString: String): IRType {
    return when (comparisonString) {
        "==" -> IRType.JMP_NE
        "!=" -> IRType.JMP_EQ
        "<" -> IRType.JMP_GE
        "<=" -> IRType.JMP_G
        ">" -> IRType.JMP_LE
        ">=" -> IRType.JMP_L
        else -> error("Could not find the requested operation")
    }
}

fun getJmpOperationFromComparator(comparisonString: String): IRType {
    return when (comparisonString) {
        "==" -> IRType.JMP_EQ
        "!=" -> IRType.JMP_NE
        "<" -> IRType.JMP_L
        "<=" -> IRType.JMP_LE
        ">" -> IRType.JMP_G
        ">=" -> IRType.JMP_GE
        else -> error("Could not find the requested operation")
    }
}

fun getStructType(variable: Type) =
    if (variable is PointerType) variable.subtype as StructType else variable as StructType


fun getAllNestedArrayGets(arrayGet: Expression.ArrayGet): MutableList<Expression.ArrayGet> {
    val arrayGets = mutableListOf(arrayGet)
    while (arrayGets.last().expression is Expression.ArrayGet) {
        arrayGets.add(arrayGets.last().expression as Expression.ArrayGet)
    }
    return arrayGets
}

fun getTypeOfLiteral(value: Any?): Type {
    return when (value) {
        is Int -> NormalType(NormalTypes.I32)
        is Char -> NormalType(NormalTypes.CHAR)
        else -> error("Could not find the literal type.")
    }
}

fun getTypeOfExpression(expression: Expression, context: IRCompilerContext): Type? {
    return when (expression) {
        is Expression.Literal -> getTypeOfLiteral(expression.value)
        is Expression.VarCall -> context.getVariables()[expression.varName.substring]!!
        is Expression.ArrayGet -> (getTypeOfExpression(expression.expression, context) as ArrayType).subType
        is Expression.Call -> context.functions.first { it.name == (expression.callee as Expression.VarCall).varName.substring }.returnType
        is Expression.Grouping -> getTypeOfExpression(expression.expression, context)
        is Expression.Unary -> getTypeOfExpression(expression.expression, context)

        is Expression.Operation -> getTypeOfExpression(expression.leftExpression, context)
        else -> error("Unknown type.")
    }
}
