package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import kotlin.math.pow

/**
 * The interpreter for the AST, it runs the program. Implements the [Expression.ExpressionVisitor]
 * @param expressions The expressions to be run
 * @param previousVariables Any previous variables if available, the default value is a new instance of [VariableStorage]
 * @param functions Any previously defined functions if available, the default value is a new instance of [FunctionStorage]
 */
class Interpreter(expressions: List<Expression>, previousVariables: VariableStorage = VariableStorage(), functions: FunctionStorage = FunctionStorage()) : Expression.ExpressionVisitor<Any?> {
    private var variableStorage = previousVariables
    private var functionStorage = functions
    var returnedValue: Any? = null

    class Return : RuntimeException()

    override fun visitAssignment(assignment: Expression.Assignment) {
        val value = assignment.expression1.accept(this)
        try {
            variableStorage.updateVariable(assignment.identifier1.substring, value)
        } catch (error: VariableNotFoundException) {
            Main.error(assignment.identifier1.line, assignment.identifier1.index, null, "The variable with the name '${assignment.identifier1.substring}' has not been found")
        }
    }

    override fun visitOperation(operation: Expression.Operation): Any? {
        val right = evaluate(operation.expression2)
        val left = evaluate(operation.expression1)

        if (right is Number && left is Number) {
            val rightDouble = right.toDouble()
            val leftDouble = left.toDouble()
            val value =  when (operation.operator1.substring) {
                "-" -> leftDouble - rightDouble
                "+" -> leftDouble + rightDouble
                "/" -> leftDouble / rightDouble
                "*" -> leftDouble * rightDouble
                "^" -> leftDouble.pow(rightDouble)
                "%" -> leftDouble % rightDouble
                //Unreachable
                else -> null
            }
            return if(left is Int) value?.toInt() else value
        } else if (right is String && left is String && operation.operator1.substring == "+") {
            return left + right
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

        return function.call(call.expressionLst1.map(this::evaluate), variableStorage, functionStorage)
    }

    override fun visitLiteral(literal: Expression.Literal): Any? {
        return literal.any1
    }

    override fun visitVarCall(varCall: Expression.VarCall): Any? {
        return getVariableFromVariableStorage(variableStorage, varCall).value
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        val args = ExpressionUtils.mapArgumentDefinitions(functionDefinition.expressionLst1)
        functionStorage.addFunction(functionDefinition.identifier1, args, functionDefinition.expressionLst2)
    }

    fun evaluate(expression: Expression) = expression.accept(this)

    init {
        try {
            expressions.forEach {
                evaluate(it)
            }
        } catch (returnError: Return) {
        }
    }


    override fun visitComparison(comparison: Expression.Comparison): Boolean {
        val left = evaluate(comparison.expression1)
        val right = evaluate(comparison.expression2)
        try {
            if(left is Double && right is Double) {
                return when (comparison.comparator1.substring) {
                    "==" -> left == right
                    "!=" -> left != right
                    "<" -> left < right
                    "<=" -> left <= right
                    ">" -> left > right
                    ">=" -> left >= right
                    // Unreachable
                    else -> return false
                }
            } else if(left is Int && right is Int){
                return when (comparison.comparator1.substring) {
                    "==" -> left == right
                    "!=" -> left != right
                    "<" -> left < right
                    "<=" -> left <= right
                    ">" -> left > right
                    ">=" -> left >= right
                    // Unreachable
                    else -> return false
                }
            }
        } catch (error: TypeCastException) {
            Main.error(comparison.comparator1.line, comparison.comparator1.index, null, "The comparator \"${comparison.comparator1.substring}\" can only be executed on numbers.")
            return false
        }
        return false
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt) {
        val isTrue = evaluate(ifStmnt.expression1) as Boolean
        if (isTrue) {
            this.returnedValue = Interpreter(ifStmnt.expressionLst1, variableStorage, functionStorage).returnedValue
            if (this.returnedValue != null) throw Return()
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
        } catch (error: TypeCastException) {
            Main.error(-1, -1, null, "The condition of the while statement is not a boolean.")
        } catch (returnError: Return) {
            return interpreter!!.returnedValue
        }
        return null
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt) {
        if (forStmnt.expressionLst1.size == 3) {
            variableStorage.addScope()
            evaluate(forStmnt.expressionLst1[0])
            while (evaluate(forStmnt.expressionLst1[1]) as Boolean) {
                val interpreter = Interpreter(forStmnt.expressionLst2, variableStorage, functionStorage)
                variableStorage = interpreter.variableStorage
                evaluate(forStmnt.expressionLst1[2])
            }
            variableStorage.popScope()
        }
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization) {
        ExpressionUtils.computeVarInitialization(varInitialization, variableStorage, this)
    }

    override fun visitClassDefinition(classDefinition: Expression.ClassDefinition) {
        val klass = Klass(classDefinition.identifier1.substring,
                functionStorage.functions.firstOrNull { it.name == classDefinition.identifier2.substring } as Klass?,
                classDefinition.expressionLst1)
        functionStorage.addFunction(klass)
        klass.initializeVariables(this)
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Any? {
        val instance = evaluate(instanceGet.expression1) as ClassInstance
        val expression = instanceGet.expression2
        if (expression is Expression.VarCall) {
            return getVariableFromVariableStorage(instance.variableStorage, expression).value
        } else if (expression is Expression.Call) {
            val args = expression.expressionLst1.map { evaluate(it) }
            return instance.functionStorage.getFunction(expression.identifier1.substring, expression.expressionLst1.size)?.let { instance.callFunction(it, args) }
        }
        return null
    }

    private fun getVariableFromVariableStorage(variables: VariableStorage, expression: Expression.VarCall): Variable {
        val returnValue = variables.getCurrentVariables()[expression.identifier1.substring]
        if (returnValue == null) {
            Main.error(expression.identifier1.line, expression.identifier1.index, null,
                    "The variable with the name \"${expression.identifier1.substring}\" is not defined or out of scope.")
        }
        return returnValue!!
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet) {
        val instance = evaluate(instanceSet.expression1) as ClassInstance
        val expression = instanceSet.expression2
        if (expression is Expression.Assignment) {
            instance.variableStorage.updateVariable(expression.identifier1.substring, evaluate(expression.expression1))
        }
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition): Any? {
        TODO("Not yet implemented")
    }
}