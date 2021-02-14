package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.MoveInformation
import com.cubearrow.cubelang.compiler.utils.TypeUtils

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
        val (leftTriple, leftSide) = getLeftSide(expression)

        val leftRegister = getRegister("ax", TypeUtils.getRawLength(leftTriple.type))

        checkMatchingTypes(rightTriple.type, leftTriple.type, -1, -1, context)
        context.operationResultType = leftTriple.type
        context.operationIndex--
        val operator = getOperator(expression.operator.substring)
        val result =
            """$rightSide
              |$leftSide
              |$operator ${if (operator != "mul" && operator != "div") "$leftRegister," else ""} ${
                getRegister(
                    register,
                    TypeUtils.getRawLength(rightTriple.type)
                )
            }""".trimMargin()
        return saveUsedRegisters(wasInSub, result, expression)
    }

    private fun getRightSide(
        expression: Expression.Operation,
        register: String
    ): Pair<MoveInformation, String> {
        val rightMoveInformation = context.moveExpressionToX(expression.rightExpression)
        val rightSide = rightMoveInformation.moveTo(getRegister(register, TypeUtils.getRawLength(rightMoveInformation.type)))
        return Pair(rightMoveInformation, rightSide)
    }

    private fun getLeftSide(expression: Expression.Operation): Pair<MoveInformation, String> {
        val leftMoveInfo = context.moveExpressionToX(expression.leftExpression)
        val leftSide = leftMoveInfo.moveTo(getRegister("ax", TypeUtils.getRawLength(leftMoveInfo.type)))
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
    /**
     * Returns the mathematical operation to use from the token string.
     */
    private fun getOperator(operatorString: String): String {
        return when (operatorString) {
            "+" -> "add"
            "-" -> "sub"
            "*" -> "mul"
            "/" -> "div"
            else -> error("Unexpected operator")
        }
    }
}