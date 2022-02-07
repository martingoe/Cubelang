package com.cubearrow.cubelang

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType
import java.util.*
import kotlin.collections.ArrayList

class TreeRewriter : Statement.StatementVisitor<Statement>, Expression.ExpressionVisitor<Expression> {
    private var currentVarIndex = 0
    var scope: Stack<Int> = Stack()
    private fun rewrite(expression: Statement): Statement {
        return evaluate(expression)
    }

    init {
        scope.push(-1)
    }

    private fun evaluate(expression: Statement): Statement = expression.accept(this)
    override fun visitAssignment(assignment: Expression.Assignment): Expression {
        assignment.valueExpression = evaluateExpression(assignment.valueExpression)
        assignment.leftSide = evaluateExpression(assignment.leftSide)
        return assignment
    }

    private fun evaluateExpression(valueExpression: Expression): Expression {
        return valueExpression.accept(this)
    }

    override fun visitVarInitialization(varInitialization: Statement.VarInitialization): Statement {
        varInitialization.valueExpression?.let {
            val assignment = Expression.Assignment(evaluateExpression(Expression.VarCall(varInitialization.name)), evaluateExpression(it))
            assignment.resultType = varInitialization.type
            return Statement.ExpressionStatement(assignment)
        }
        return Statement.Empty(null)
    }



    override fun visitOperation(operation: Expression.Operation): Expression {
        operation.leftExpression = evaluateExpression(operation.leftExpression)
        operation.rightExpression = evaluateExpression(operation.rightExpression)
        return operation
    }

    override fun visitCall(call: Expression.Call): Expression {
        call.arguments = call.arguments.map { evaluateExpression(it) }
        return call
    }

    override fun visitLiteral(literal: Expression.Literal): Expression {
        return literal
    }

    override fun visitVarCall(varCall: Expression.VarCall): Expression {
        val index = getVarIndex(varCall.varName.substring)
        val result =  Expression.ValueFromPointer(
            Expression.Operation(
                Expression.FramePointer(),
                Token("-", TokenType.PLUSMINUS),
                Expression.Literal(index)
            )
        )
        result.resultType = varCall.resultType
        return result
    }

    private fun getVarIndex(substring: String): Int {
        return getVariables().first { it.name == substring }.offset
    }

    /**
     * Returns all the currently declared variables.
     */
    private fun getVariables(): List<VarNode> {
        return SymbolTableSingleton.getCurrentSymbolTable().getVariablesInCurrentScope(scope)
    }


    override fun visitFunctionDefinition(functionDefinition: Statement.FunctionDefinition): Statement {
        currentVarIndex = 0

        scope.push(scope.pop() + 1)
        scope.push(-1)

        functionDefinition.body = evaluate(functionDefinition.body)

        scope.pop()
        return functionDefinition
    }

    override fun visitComparison(comparison: Expression.Comparison): Expression {
        comparison.leftExpression = evaluateExpression(comparison.leftExpression)
        comparison.rightExpression = evaluateExpression(comparison.rightExpression)
        return comparison
    }

    override fun visitIfStmnt(ifStmnt: Statement.IfStmnt): Statement {
        ifStmnt.condition = evaluateExpression(ifStmnt.condition)
        ifStmnt.ifBody = evaluate(ifStmnt.ifBody)
        ifStmnt.elseBody = ifStmnt.elseBody?.let { evaluate(it) }
        return ifStmnt
    }

    override fun visitReturnStmnt(returnStmnt: Statement.ReturnStmnt): Statement {
        returnStmnt.returnValue = returnStmnt.returnValue?.let { evaluateExpression(it) }
        return returnStmnt
    }

    override fun visitWhileStmnt(whileStmnt: Statement.WhileStmnt): Statement {
        whileStmnt.body = evaluate(whileStmnt.body)
        whileStmnt.condition = evaluateExpression(whileStmnt.condition)
        return whileStmnt
    }

    override fun visitForStmnt(forStmnt: Statement.ForStmnt): Statement {
        forStmnt.body = evaluate(forStmnt.body)
        forStmnt.inBrackets = forStmnt.inBrackets.map { evaluate(it) }
        return forStmnt
    }

    override fun visitStructDefinition(structDefinition: Statement.StructDefinition): Statement {
        return structDefinition
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Expression {
//        TODO()
        val index = getInstanceGetIndex(
            SymbolTableSingleton.getCurrentSymbolTable().getStruct(instanceGet.identifier.substring)!!,
            instanceGet.expression as Expression.VarCall
        )
        return Expression.ValueFromPointer(
            Expression.Operation(
                Expression.FramePointer(),
                Token("-", TokenType.PLUSMINUS),
                Expression.Literal(index)
            )
        )
    }

    private fun getInstanceGetIndex(struct: Struct, structSubvalue: Expression.VarCall): Int {
        val requestedVar = struct.variables.first { pair -> pair.first == structSubvalue.varName.substring }
        val argumentsBefore = struct.variables.subList(0, struct.variables.indexOf(requestedVar))
        return argumentsBefore.fold(0) { acc, pair -> acc + pair.second.getLength() }
    }

    override fun visitInstanceSet(instanceSet: Statement.InstanceSet): Statement {
        TODO("Create a valuetopointer expression")
    }

    override fun visitArgumentDefinition(argumentDefinition: Statement.ArgumentDefinition): Statement {
        return argumentDefinition
    }

    override fun visitBlockStatement(blockStatement: Statement.BlockStatement): Statement {
        scope.push(scope.pop() + 1)
        scope.push(-1)
        blockStatement.statements = blockStatement.statements.map { evaluate(it) }
        scope.pop()
        return blockStatement
    }

    override fun visitLogical(logical: Expression.Logical): Expression {
        logical.leftExpression = evaluateExpression(logical.leftExpression)
        logical.rightExpression = evaluateExpression(logical.rightExpression)
        return logical
    }

    override fun visitUnary(unary: Expression.Unary): Expression {
        unary.expression = evaluateExpression(unary.expression)
        return unary
    }

    override fun visitGrouping(grouping: Expression.Grouping): Expression {
        // TODO: Is a grouping still needed or can you return the evaluated subexpression?
        grouping.expression = evaluateExpression(grouping.expression)
        return grouping
    }

    private fun getSubtype(type: Type): Type?{
        return when(type){
            is PointerType -> type.subtype
            is ArrayType -> type.subType
            else -> null
        }
    }
    override fun visitArrayGet(arrayGet: Expression.ArrayGet): Expression {
        val arrayGets = getArrayGets(arrayGet)
        if(arrayGets.last().expression is Expression.VarCall){
            val variable = getVariable((arrayGets.last().expression as Expression.VarCall).varName)
            var index = variable.offset

            if(arrayGets.all { it.inBrackets is Expression.Literal }){

                arrayGets.forEach {
                    index -= it.resultType.getLength() * getLiteralValue((it.inBrackets as Expression.Literal).value)
                }
                val result = Expression.ValueFromPointer(
                    Expression.Operation(
                        Expression.FramePointer(),
                        Token("-", TokenType.PLUSMINUS),
                        Expression.Literal(index)
                    )
                )
                result.resultType = arrayGet.resultType
                return result
            }

            var resultAddition = Expression.Operation(evaluateExpression(arrayGets.last().inBrackets), Token("*", TokenType.STAR), Expression.Literal(arrayGets.last().resultType.getLength()))
            arrayGets.dropLast(1).reversed().forEach {
                resultAddition = Expression.Operation(
                    resultAddition,
                    Token("+", TokenType.PLUSMINUS),
                    Expression.Operation(
                        evaluateExpression(it.inBrackets),
                        Token("*", TokenType.STAR),
                        Expression.Literal(it.resultType.getLength())
                    )
                )
            }
            return Expression.ValueFromPointer(
                Expression.Operation(
                    Expression.Operation(
                        Expression.FramePointer(),
                        Token("-", TokenType.PLUSMINUS),
                        Expression.Literal(index)
                    ),
                    Token("+", TokenType.PLUSMINUS),
                    resultAddition
                )
            )
        }

        TODO()
    }

    private fun getArrayGets(arrayGet: Expression.ArrayGet): List<Expression.ArrayGet>{
        val result: MutableList<Expression.ArrayGet> = ArrayList()
        result.add(arrayGet)
        while(result.last().expression is Expression.ArrayGet){
            result.add(result.last().expression as Expression.ArrayGet)
        }
        return result
    }

    private fun getVariable(varName: Token): VarNode {
        return getVariables().first { it.name == varName.substring }
    }


    override fun visitArraySet(arraySet: Statement.ArraySet): Statement {
        TODO("Not yet implemented")
    }

    override fun visitImportStmnt(importStmnt: Statement.ImportStmnt): Statement {
        return importStmnt
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet): Expression {
        return pointerGet
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer): Expression {
        valueFromPointer.expression = evaluateExpression(valueFromPointer.expression)
        return valueFromPointer
    }

    override fun visitEmpty(empty: Statement.Empty): Statement {
        TODO("Not yet implemented")
    }


    fun rewriteMultiple(expressions: List<Statement>) {
        expressions.forEach { rewrite(it) }
    }

    override fun visitValueToPointer(valueToPointer: Expression.ValueToPointer): Expression {
        valueToPointer.value = evaluateExpression(valueToPointer.value)
        valueToPointer.pointer = evaluateExpression(valueToPointer.pointer)
        return valueToPointer
    }

    override fun visitExpressionStatement(expressionStatement: Statement.ExpressionStatement): Statement {
        expressionStatement.expression = evaluateExpression(expressionStatement.expression)
        return expressionStatement
    }

    override fun visitRegister(register: Expression.Register): Expression {
        return register
    }

    override fun acceptFramePointer(framePointer: Expression.FramePointer): Expression {
        return framePointer
    }
}