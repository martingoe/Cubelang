package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.UsualErrorMessages
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
        UsualErrorMessages.onlyNumberError(operation.operator1)
        return null
    }

    override fun visitCall(call: Expression.Call): Any? {
        if(call.expression1 is Expression.VarCall) {
            val varCall = call.expression1 as Expression.VarCall
            val function = functionStorage.getFunction(varCall.identifier1.substring, call.expressionLst1.size)
            if (function == null) {
                Main.error(varCall.identifier1.line, varCall.identifier1.index, null, "The called function is not defined")
                return null
            }

            return function.call(call.expressionLst1.map(this::evaluate), variableStorage, functionStorage)
        }
        TODO("")
    }

    override fun visitLiteral(literal: Expression.Literal): Any? {
        return literal.any1
    }

    override fun visitVarCall(varCall: Expression.VarCall): Any? {
        return getVariableFromVariableStorage(variableStorage, varCall.identifier1).value
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        val args = ExpressionUtils.mapArgumentDefinitions(functionDefinition.expressionLst1)
        functionStorage.addFunction(functionDefinition.identifier1, args, functionDefinition.expression1)
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
            UsualErrorMessages.onlyNumberError(comparison.comparator1)
            return false
        }
        return false
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt) {
        val isTrue = evaluate(ifStmnt.expression1) as Boolean
        if (isTrue) {
            ifStmnt.expression2.accept(this)
            if (this.returnedValue != null) throw Return()
        } else if(ifStmnt.expressionNull1 != null){
            ifStmnt.expressionNull1!!.accept(this)
        }
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): Any? {
        this.returnedValue = returnStmnt.expressionNull1?.let { evaluate(it) }
        throw Return()
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): Any? {
        try {
            while (evaluate(whileStmnt.expression1) as Boolean) {
                variableStorage.addScope()
                whileStmnt.expression2.accept(this)
                variableStorage.popScope()
            }
        } catch (error: TypeCastException) {
            Main.error(-1, -1, null, "The condition of the while statement is not a boolean.")
        } catch (returnError: Return) {
            return returnedValue
        }
        return null
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt) {
        if (forStmnt.expressionLst1.size == 3) {
            variableStorage.addScope()
            evaluate(forStmnt.expressionLst1[0])
            while (evaluate(forStmnt.expressionLst1[1]) as Boolean) {
                forStmnt.expression1.accept(this)
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
                functionStorage.functions.firstOrNull { it.name == classDefinition.identifierNull1?.substring } as Klass?,
                classDefinition.expressionLst1)
        functionStorage.addFunction(klass)
        klass.initializeVariables(this)
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Any? {
        val instance = evaluate(instanceGet.expression1) as ClassInstance
        val expression = instanceGet.identifier1
        return getVariableFromVariableStorage(instance.variableStorage, expression).value
    }

    private fun getVariableFromVariableStorage(variables: VariableStorage, expression: Token): Variable {
        val returnValue = variables.getCurrentVariables()[expression.substring]
        if (returnValue != null) {
            Main.error(expression.line, expression.index, null,
                    "The variable with the name \"${expression.substring}\" is not defined or out of scope.")
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

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement){
        for(expression in blockStatement.expressionLst1){
            expression.accept(this)
        }
    }

    override fun visitLogical(logical: Expression.Logical): Any? {
        TODO("Not yet implemented")
    }

    override fun visitUnary(unary: Expression.Unary): Any? {
        TODO("Not yet implemented")
    }

    override fun visitGrouping(grouping: Expression.Grouping): Any? {
        TODO("Not yet implemented")
    }

    override fun visitEmpty(empty: Expression.Empty){}
}