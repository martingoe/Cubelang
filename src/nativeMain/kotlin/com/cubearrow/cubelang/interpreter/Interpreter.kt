package com.cubearrow.cubelang.interpreter

import Main
import com.cubearrow.cubelang.compiler.NormalType
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.UsualErrorMessages

/**
 * The interpreter for the AST, it runs the program. Implements the [Expression.ExpressionVisitor]
 * @param expressions The expressions to be run
 * @param previousVariables Any previous variables if available, the default value is a new instance of [VariableStorage]
 * @param functions Any previously defined functions if available, the default value is a new instance of [FunctionStorage]
 */
class Interpreter(
    expressions: List<Expression>,
    previousVariables: VariableStorage = VariableStorage(),
    functions: FunctionStorage = FunctionStorage()
) : Expression.ExpressionVisitor<Any?> {
    private var variableStorage = previousVariables
    private var functionStorage = functions
    var returnedValue: Any? = null

    class Return : RuntimeException()

    override fun visitAssignment(assignment: Expression.Assignment) {
        val value = assignment.valueExpression.accept(this)
        try {
            variableStorage.updateVariable(assignment.name.substring, value)
        } catch (error: VariableNotFoundException) {
            Main.error(
                assignment.name.line,
                assignment.name.index,
                null,
                "The variable with the name '${assignment.name.substring}' has not been found"
            )
        }
    }

    override fun visitOperation(operation: Expression.Operation): Any? {
        val right = evaluate(operation.rightExpression)
        val left = evaluate(operation.leftExpression)

        if (right is Number && left is Number) {
            val rightDouble = right.toDouble()
            val leftDouble = left.toDouble()
            val value = when (operation.operator.substring) {
                "-" -> leftDouble - rightDouble
                "+" -> leftDouble + rightDouble
                "/" -> leftDouble / rightDouble
                "*" -> leftDouble * rightDouble
                "%" -> leftDouble % rightDouble
                //Unreachable
                else -> null
            }
            return if (left is Int) value?.toInt() else value
        } else if (right is String && left is String && operation.operator.substring == "+") {
            return left + right
        }
        UsualErrorMessages.onlyNumberError(operation.operator)
        return null
    }

    override fun visitCall(call: Expression.Call): Any? {
        if (call.callee is Expression.VarCall) {
            val function = functionStorage.getFunction(call.callee.varName.substring, call.arguments.size)
            if (function == null) {
                Main.error(
                    call.callee.varName.line,
                    call.callee.varName.index,
                    null,
                    "The called function is not defined"
                )
                return null
            }

            return function.call(call.arguments.map(this::evaluate), variableStorage, functionStorage)
        }
        TODO("")
    }

    override fun visitLiteral(literal: Expression.Literal): Any? {
        return literal.value
    }

    override fun visitVarCall(varCall: Expression.VarCall): Any? {
        return getVariableFromVariableStorage(variableStorage, varCall.varName).value
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        val args = ExpressionUtils.mapArgumentDefinitions(functionDefinition.args)
        functionStorage.addFunction(functionDefinition.name, args, functionDefinition.body)
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
        val left = evaluate(comparison.leftExpression)
        val right = evaluate(comparison.rightExpression)
        try {
            if (left is Double && right is Double) {
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
            } else if (left is Int && right is Int) {
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
        val isTrue = evaluate(ifStmnt.condition) as Boolean
        if (isTrue) {
            ifStmnt.ifBody.accept(this)
            if (this.returnedValue != null) throw Return()
        } else if (ifStmnt.elseBody != null) {
            ifStmnt.elseBody.accept(this)
        }
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): Any? {
        this.returnedValue = returnStmnt.returnValue?.let { evaluate(it) }
        throw Return()
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): Any? {
        try {
            while (evaluate(whileStmnt.condition) as Boolean) {
                variableStorage.addScope()
                whileStmnt.body.accept(this)
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
        if (forStmnt.inBrackets.size == 3) {
            variableStorage.addScope()
            evaluate(forStmnt.inBrackets[0])
            while (evaluate(forStmnt.inBrackets[1]) as Boolean) {
                forStmnt.body.accept(this)
                evaluate(forStmnt.inBrackets[2])
            }
            variableStorage.popScope()
        }
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization) {
        ExpressionUtils.computeVarInitialization(varInitialization, variableStorage, this)
    }

    override fun visitClassDefinition(classDefinition: Expression.ClassDefinition) {
        val klass = Klass(classDefinition.name.substring,
            functionStorage.functions.firstOrNull { it.name == (classDefinition.type as NormalType).typeName } as Klass?,
            classDefinition.body)
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
            Main.error(
                expression.line, expression.index, null,
                "The variable with the name \"${expression.substring}\" is not defined or out of scope."
            )
        }
        return returnValue!!
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet) {
        val instance = evaluate(instanceSet.expression) as ClassInstance
        val expression = instanceSet.value
        if (expression is Expression.Assignment) {
            instance.variableStorage.updateVariable(expression.name.substring, evaluate(expression.valueExpression))
        }
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition): Any? {
        TODO("Not yet implemented")
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement) {
        for (expression in blockStatement.statements) {
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

    override fun visitEmpty(empty: Expression.Empty) {}


    override fun visitArrayGet(arrayGet: Expression.ArrayGet): Any? {
        TODO("Not yet implemented")
    }

    override fun visitArraySet(arraySet: Expression.ArraySet): Any? {
        TODO("Not yet implemented")
    }
}