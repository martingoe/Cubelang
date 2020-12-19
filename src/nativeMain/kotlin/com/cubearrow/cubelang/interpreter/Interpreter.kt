package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.compiler.NormalType
import com.cubearrow.cubelang.lexer.Token
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
        val value = assignment.expression.accept(this)
        try {
            variableStorage.updateVariable(assignment.identifier.substring, value)
        } catch (error: VariableNotFoundException) {
            Main.error(assignment.identifier.line, assignment.identifier.index, null, "The variable with the name '${assignment.identifier.substring}' has not been found")
        }
    }

    override fun visitOperation(operation: Expression.Operation): Any? {
        val right = evaluate(operation.expression2)
        val left = evaluate(operation.expression)

        if (right is Number && left is Number) {
            val rightDouble = right.toDouble()
            val leftDouble = left.toDouble()
            val value =  when (operation.operator.substring) {
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
        } else if (right is String && left is String && operation.operator.substring == "+") {
            return left + right
        }
        UsualErrorMessages.onlyNumberError(operation.operator)
        return null
    }

    override fun visitCall(call: Expression.Call): Any? {
        if(call.expression is Expression.VarCall) {
            val function = functionStorage.getFunction(call.expression.identifier.substring, call.expressionLst.size)
            if (function == null) {
                Main.error(call.expression.identifier.line, call.expression.identifier.index, null, "The called function is not defined")
                return null
            }

            return function.call(call.expressionLst.map(this::evaluate), variableStorage, functionStorage)
        }
        TODO("")
    }

    override fun visitLiteral(literal: Expression.Literal): Any? {
        return literal.any
    }

    override fun visitVarCall(varCall: Expression.VarCall): Any? {
        return getVariableFromVariableStorage(variableStorage, varCall.identifier).value
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        val args = ExpressionUtils.mapArgumentDefinitions(functionDefinition.expressionLst)
        functionStorage.addFunction(functionDefinition.identifier, args, functionDefinition.expression)
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
        val left = evaluate(comparison.expression)
        val right = evaluate(comparison.expression2)
        try {
            if(left is Double && right is Double) {
                return when (comparison.comparator.substring) {
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
                return when (comparison.comparator.substring) {
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
            UsualErrorMessages.onlyNumberError(comparison.comparator)
            return false
        }
        return false
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt) {
        val isTrue = evaluate(ifStmnt.expression) as Boolean
        if (isTrue) {
            ifStmnt.expression2.accept(this)
            if (this.returnedValue != null) throw Return()
        } else if(ifStmnt.expressionNull != null){
            ifStmnt.expressionNull.accept(this)
        }
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): Any? {
        this.returnedValue = returnStmnt.expressionNull?.let { evaluate(it) }
        throw Return()
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): Any? {
        try {
            while (evaluate(whileStmnt.expression) as Boolean) {
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
        if (forStmnt.expressionLst.size == 3) {
            variableStorage.addScope()
            evaluate(forStmnt.expressionLst[0])
            while (evaluate(forStmnt.expressionLst[1]) as Boolean) {
                forStmnt.expression.accept(this)
                evaluate(forStmnt.expressionLst[2])
            }
            variableStorage.popScope()
        }
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization) {
        ExpressionUtils.computeVarInitialization(varInitialization, variableStorage, this)
    }

    override fun visitClassDefinition(classDefinition: Expression.ClassDefinition) {
        val klass = Klass(classDefinition.identifier.substring,
                functionStorage.functions.firstOrNull { it.name == (classDefinition.typeNull as NormalType).typeName } as Klass?,
                classDefinition.expressionLst)
        functionStorage.addFunction(klass)
        klass.initializeVariables(this)
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Any? {
        val instance = evaluate(instanceGet.expression) as ClassInstance
        val expression = instanceGet.identifier
        return getVariableFromVariableStorage(instance.variableStorage, expression).value
    }

    private fun getVariableFromVariableStorage(variables: VariableStorage, expression: Token): Variable {
        val returnValue = variables.getCurrentVariables()[expression.substring]
        if (returnValue == null) {
            Main.error(expression.line, expression.index, null,
                    "The variable with the name \"${expression.substring}\" is not defined or out of scope.")
        }
        return returnValue!!
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet) {
        val instance = evaluate(instanceSet.expression) as ClassInstance
        val expression = instanceSet.expression2
        if (expression is Expression.Assignment) {
            instance.variableStorage.updateVariable(expression.identifier.substring, evaluate(expression.expression))
        }
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition): Any? {
        TODO("Not yet implemented")
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement){
        for(expression in blockStatement.expressionLst){
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
        return evaluate(grouping.expression)
    }

    override fun visitEmpty(empty: Expression.Empty){}


    override fun visitArrayGet(arrayGet: Expression.ArrayGet): Any? {
        TODO("Not yet implemented")
    }

    override fun visitArraySet(arraySet: Expression.ArraySet): Any? {
        TODO("Not yet implemented")
    }
}