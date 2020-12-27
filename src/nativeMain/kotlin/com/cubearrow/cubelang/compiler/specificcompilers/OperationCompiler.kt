package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.parser.Expression

class OperationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Operation> {
    override fun accept(expression: Expression.Operation): String {
        val wasInSub = context.isInSubOperation
        if (!context.isInSubOperation) context.isInSubOperation = true
        context.operationIndex++
        val register = Compiler.OPERATION_REGISTERS[context.operationIndex]!!
        val rightTriple = CompilerUtils.moveExpressionToX(expression.rightExpression, context)
        val rightSide = "${rightTriple.first}\nmov ${getRegister(register, rightTriple.third.getRawLength())}, ${rightTriple.second}"
        val rightRegister = getRegister(register, rightTriple.third.getRawLength())

        val leftTriple = CompilerUtils.moveExpressionToX(expression.leftExpression, context)
        val leftSide = "${leftTriple.first}${
            if (!leftTriple.second.endsWith("ax")) "\nmov ${
                getRegister(
                    "ax",
                    leftTriple.third.getRawLength()
                )
            }, ${leftTriple.second}" else ""
        }"
        val leftRegister = getRegister("ax", leftTriple.third.getRawLength())
        checkMatchingTypes(rightTriple.third, leftTriple.third)
        context.operationResultType = leftTriple.third
        context.operationIndex--
        val operator = CompilerUtils.getOperator(expression.operator.substring)
        val result =
            "$rightSide\n$leftSide\n${operator} ${if (operator != "mul" && operator != "div") "$leftRegister," else ""} $rightRegister"
        return saveUsedRegisters(wasInSub, result, expression)
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