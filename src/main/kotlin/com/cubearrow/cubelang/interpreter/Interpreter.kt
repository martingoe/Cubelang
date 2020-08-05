package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class Interpreter(expressions: List<Expression>, previousVariables: VariableStorage?, functions: FunctionStorage = FunctionStorage()) : Expression.ExpressionVisitor<Any?> {
    private lateinit var variableStorage: VariableStorage
    private var functionStorage = functions
    var returnedValue: Any? = null
    class Return : RuntimeException()
    override fun visitAssignment(assignment: Expression.Assignment) {
        val value = assignment.expression1.accept(this)
        variableStorage.addVariableToCurrentScope(assignment.identifier1.substring, value)
    }

    override fun visitOperation(operation: Expression.Operation): Double? {
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
        if (function == null) {
            Main.error(call.identifier1.line, call.identifier1.index, null, "The called function is not defined")
            return null
        }

        variableStorage.addScope()

        //Add the argument variables to the variable stack
        for (i in 0 until call.expressionLst1.size) {
            val value = evaluate(call.expressionLst1[i])
            variableStorage.addVariableToCurrentScope(function.args[i], value)
        }
        val value = function.call(variableStorage, functionStorage)
        variableStorage.popScope()

        return value
    }

    override fun visitLiteral(literal: Expression.Literal): Any? {
        return literal.any1
    }

    override fun visitVarCall(varCall: Expression.VarCall): Any? {
        return variableStorage.getCurrentVariables()[varCall.identifier1.substring]
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        val args = functionDefinition.expressionLst1.map { (it as Expression.VarCall).identifier1.substring }
        functionStorage.addFunction(functionDefinition.identifier1, args, functionDefinition.expressionLst2)
    }

    private fun evaluate(expression: Expression) = expression.accept(this)

    init {
        initializeVariableStorage(previousVariables)
        try {
            expressions.forEach {
                evaluate(it)
            }
        } catch (returnError: Return) {}
    }

    private fun initializeVariableStorage(previousVariables: VariableStorage?) {
        if (previousVariables != null) {
            variableStorage = previousVariables
        } else {
            variableStorage = VariableStorage()
            variableStorage.addScope()
        }
    }

    override fun visitComparison(comparison: Expression.Comparison): Boolean {
        val left = evaluate(comparison.expression1)
        val right = evaluate(comparison.expression2)
        return when (comparison.comparator1.substring) {
            "==" -> left == right
            "!=" -> left != right
            "<" -> (left as Double) < right as Double
            "<=" -> left as Double <= right as Double
            ">" -> left as Double > right as Double
            ">=" -> left as Double >= right as Double
            // Unreachable
            else -> return false
        }
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt) {
        val isTrue = evaluate(ifStmnt.expression1) as Boolean
        if (isTrue) {
            Interpreter(ifStmnt.expressionLst1, variableStorage, functionStorage)
        } else {
            Interpreter(ifStmnt.expressionLst2, variableStorage, functionStorage)
        }
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): Any? {
        this.returnedValue = evaluate(returnStmnt.expression1)
        throw Return()
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): Any? {
        var interpreter: Interpreter? = null
        try {
            while (evaluate(whileStmnt.expression1) as Boolean) {
                variableStorage.addScope()
                interpreter = Interpreter(whileStmnt.expressionLst1, variableStorage, functionStorage)
                interpreter.variableStorage.popScope()
                this.variableStorage = interpreter.variableStorage
                this.functionStorage = interpreter.functionStorage
            }
        }
        catch (error: TypeCastException){
            Main.error(-1, -1, null, "The condition of the while statement is not a boolean.")
        } catch(returnError: Return){
            return interpreter!!.returnedValue
        }
        return null
    }
}