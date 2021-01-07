package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.isAXRegister
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.Type

class OperationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Operation> {
    override fun accept(expression: Expression.Operation): String {
        val wasInSub = context.isInSubOperation
        if (!context.isInSubOperation) context.isInSubOperation = true
        context.operationIndex++
        val register = Compiler.OPERATION_REGISTERS[context.operationIndex]!!

        val (rightTriple, rightSide) = getLeftSide(expression, register)
        val (leftTriple, leftSide) = getRightSide(expression)

        val leftRegister = getRegister("ax", leftTriple.third.getRawLength())

        checkMatchingTypes(rightTriple.third, leftTriple.third, -1, -1)
        context.operationResultType = leftTriple.third
        context.operationIndex--
        val operator = CompilerUtils.getOperator(expression.operator.substring)
        val result =
            "$rightSide\n$leftSide\n${operator} ${if (operator != "mul" && operator != "div") "$leftRegister," else ""} ${getRegister(register, rightTriple.third.getRawLength())}"
        return saveUsedRegisters(wasInSub, result, expression)
    }

    private fun getLeftSide(
        expression: Expression.Operation,
        register: String
    ): Pair<Triple<String, String, Type>, String> {
        val rightTriple = context.moveExpressionToX(expression.rightExpression)
        val rightSide = "${rightTriple.first}\nmov ${getRegister(register, rightTriple.third.getRawLength())}, ${rightTriple.second}"
        return Pair(rightTriple, rightSide)
    }

    private fun getRightSide(expression: Expression.Operation): Pair<Triple<String, String, Type>, String> {
        val leftTriple = context.moveExpressionToX(expression.leftExpression)
        val leftSide = "${leftTriple.first}${
            if (!isAXRegister(leftTriple.second)) "\nmov ${
                getRegister(
                    "ax",
                    leftTriple.third.getRawLength()
                )
            }, ${leftTriple.second}" else ""
        }"
        return Pair(leftTriple, leftSide)
    }

    private fun saveUsedRegisters(wasInSub: Boolean, result: String, expression: Expression): String {
        var result1 = result
        if (!wasInSub) {
            context.isInSubOperation = false
            for (i in 0 until CompilerUtils.getOperationDepth(expression) - 1) {
                result1 = "push r${Compiler.OPERATION_REGISTERS[i]}\n$result1\npop r${Compiler.OPERATION_REGISTERS[i]}"
            }
        }
        return result1
    }
}