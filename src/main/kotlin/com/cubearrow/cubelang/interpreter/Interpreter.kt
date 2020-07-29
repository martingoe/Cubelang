package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class Interpreter : Expression.ExpressionVisitor<Any?> {
    override fun visitAssignment(assignment: Expression.Assignment): Any? {
        TODO("Not yet implemented")
    }

    override fun visitOperation(operation: Expression.Operation): Any? {
        val right = evaluate(operation.expression2)
        val left = evaluate(operation.expression1)

        if (right is Double && left is Double) {
            return when (operation.operator1.substring) {
                "-" -> left - right
                "+" -> left + right
                "/" -> left / right
                "*" -> left * right
                "^" -> Math.pow(left, right)
                "%" -> left % right
                //Unreachable
                else -> null
            }
        }
        Main.error(operation.operator1.line, operation.operator1.index, null, "Mathematical operations can only be performed on numbers")
        return null
    }

    override fun visitCall(call: Expression.Call): Any? {
        TODO("Not yet implemented")
    }

    override fun visitLiteral(literal: Expression.Literal): Any? {
        return literal.number1.substring.toDouble()
    }

    override fun visitVarCall(varCall: Expression.VarCall): Any? {
        TODO("Not yet implemented")
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition): Any? {
        TODO("Not yet implemented")
    }

    private fun evaluate(expression: Expression) = expression.accept(this)
}