package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.MoveInformation
import com.cubearrow.cubelang.parser.Expression

/**
 * Compiles mathematical operations while saving the used registers if needed.
 *
 * The result is saved in the 'ax' register and [CompilerContext.operationResultType] is updated to the result type.
 *
 * @param context The needed [CompilerContext].
 */
class OperationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Operation> {
    override fun accept(expression: Expression.Operation): String {
        val wasInSub = context.isInSubOperation
        if (!context.isInSubOperation) context.isInSubOperation = true
        context.operationIndex++
        val register = Compiler.OPERATION_REGISTERS[context.operationIndex]!!

        val (rightTriple, rightSide) = getRightSide(expression, register)
        val (leftTriple, leftSide) = getRightSide(expression)

        val leftRegister = getRegister("ax", leftTriple.type.getRawLength())

        checkMatchingTypes(rightTriple.type, leftTriple.type, -1, -1)
        context.operationResultType = leftTriple.type
        context.operationIndex--
        val operator = CompilerUtils.getOperator(expression.operator.substring)
        val result =
            """$rightSide
              |$leftSide
              |$operator ${if (operator != "mul" && operator != "div") "$leftRegister," else ""} ${
                getRegister(
                    register,
                    rightTriple.type.getRawLength()
                )
            }""".trimMargin()
        return saveUsedRegisters(wasInSub, result, expression)
    }

    private fun getRightSide(
        expression: Expression.Operation,
        register: String
    ): Pair<MoveInformation, String> {
        val rightMoveInformation = context.moveExpressionToX(expression.rightExpression)
        val rightSide = rightMoveInformation.moveTo(getRegister(register, rightMoveInformation.type.getRawLength()))
        return Pair(rightMoveInformation, rightSide)
    }

    private fun getRightSide(expression: Expression.Operation): Pair<MoveInformation, String> {
        val leftMoveInfo = context.moveExpressionToX(expression.leftExpression)
        val leftSide = leftMoveInfo.moveTo(getRegister("ax", leftMoveInfo.type.getRawLength()))
        return Pair(leftMoveInfo, leftSide)
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