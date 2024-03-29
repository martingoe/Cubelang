package com.martingoe.cubelang.backend.instructionselection

import com.martingoe.cubelang.common.Expression

/**
 * A mapping from the different expressions to a specific symbol (char). Used in the [[ExpressionMatchingTrie]].
 */
class ASTGetSymbol: Expression.ExpressionVisitor<Char> {
    override fun visitOperation(operation: Expression.Operation): Char {
        return operation.operator.substring[0]
    }

    fun evaluate(expression: Expression): Char {
        return expression.accept(this)
    }

    override fun visitCall(call: Expression.Call): Char {
        return 'c'
    }

    override fun visitLiteral(literal: Expression.Literal): Char {
        return 'l'
    }

    override fun visitVarCall(varCall: Expression.VarCall): Char {
        TODO("Not yet implemented")
    }

    override fun visitComparison(comparison: Expression.Comparison): Char {
        return 'x'
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Char {
        TODO()
    }


    override fun visitLogical(logical: Expression.Logical): Char {
        TODO("Not yet implemented")
    }

    override fun visitUnary(unary: Expression.Unary): Char {
        return 'r'
    }

    override fun visitGrouping(grouping: Expression.Grouping): Char {
        return evaluate(grouping.expression)
    }

    override fun visitArrayGet(arrayGet: Expression.ArrayGet): Char {
        TODO("Not yet implemented")
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet): Char {
        return 'P'
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer): Char {
        return 'L'
    }

    override fun visitRegister(register: Expression.Register): Char {
        return 'r'
    }

    override fun visitAssignment(assignment: Expression.Assignment): Char {
        return '='
    }

    override fun acceptFramePointer(framePointer: Expression.FramePointer): Char {
        return 'f'
    }

    override fun acceptExtendTo64Bits(extendTo64Bit: Expression.ExtendTo64Bit): Char {
        return 'e'
    }

    override fun visitStringLiteral(stringLiteral: Expression.StringLiteral): Char {
        return 's'
    }

}