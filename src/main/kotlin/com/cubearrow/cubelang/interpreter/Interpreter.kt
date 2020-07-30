package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class Interpreter(expressions: List<Expression>, previousVariables: VariableStorage? = null) : Expression.ExpressionVisitor<Any?> {
    private var variableStorage = VariableStorage()
    private var functionStorage = FunctionStorage()
    override fun visitAssignment(assignment: Expression.Assignment): Any? {
        val value = assignment.expression1.accept(this)
        variableStorage.addVariableToCurrentScope(assignment.identifier1.substring, value)
        return value
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
        val function = functionStorage.getFunction(call.identifier1.substring, call.expressionLst1.size)
        if (function != null) {
            variableStorage.addScope()

            //Add the argument variables to the variable stack
            for (i in 0 until call.expressionLst1.size) {
                val value = evaluate(call.expressionLst1[i]) as Double
                variableStorage.addVariableToCurrentScope(function.args[i], value)
            }
            Interpreter(function.body, variableStorage)
            variableStorage.popScope()
        } else {
            Main.error(call.identifier1.line, call.identifier1.index, null, "The called function is not defined")
        }
        return null
    }

    override fun visitLiteral(literal: Expression.Literal): Any? {
        return literal.number1.substring.toDouble()
    }

    override fun visitVarCall(varCall: Expression.VarCall): Any? {
        return variableStorage.getCurrentVariables()[varCall.identifier1.substring]
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition): Any? {
        val args = functionDefinition.expressionLst1.map { (it as Expression.VarCall).identifier1.substring }
        functionStorage.addFunction(functionDefinition.identifier1, args, functionDefinition.expressionLst2)
        return null
    }

    fun evaluate(expression: Expression) = expression.accept(this)

    init {
        variableStorage.addScope()
        previousVariables?.let { this.variableStorage = it }
        expressions.forEach { println(it.accept(this)) }
    }

}