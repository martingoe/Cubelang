package com.cubearrow.cubelang.instructionselection

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.Expression.*
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType
import java.util.*
import kotlin.collections.ArrayList

class TreeRewriter : Statement.StatementVisitor<Statement>, ExpressionVisitor<Expression> {
    constructor(scope: Stack<Int>) {
        this.scope = scope
    }

    private var currentVarIndex = 0
    var scope: Stack<Int> = Stack()
    private fun rewrite(expression: Statement): Statement {
        return evaluate(expression)
    }

    constructor() {
        scope.push(-1)
    }

    private fun evaluate(expression: Statement): Statement = expression.accept(this)
    override fun visitAssignment(assignment: Assignment): Expression {
        assignment.valueExpression = evaluateExpression(assignment.valueExpression)
        assignment.leftSide = evaluateExpression(assignment.leftSide)
        return assignment
    }

    private fun evaluateExpression(valueExpression: Expression): Expression {
        return valueExpression.accept(this)
    }

    override fun visitVarInitialization(varInitialization: Statement.VarInitialization): Statement {
        varInitialization.valueExpression?.let {
            val assignment = Assignment(evaluateExpression(VarCall(varInitialization.name)), evaluateExpression(it),Token("=", TokenType.EQUALS))
            assignment.resultType = varInitialization.type
            return Statement.ExpressionStatement(assignment)
        }
        return Statement.Empty(null)
    }


    override fun visitOperation(operation: Operation): Expression {
        operation.leftExpression = evaluateExpression(operation.leftExpression)
        operation.rightExpression = evaluateExpression(operation.rightExpression)
        return operation
    }

    override fun visitCall(call: Call): Expression {
        call.arguments = call.arguments.map { evaluateExpression(it) }
        return call
    }

    override fun visitLiteral(literal: Literal): Expression {
        return literal
    }

    override fun visitVarCall(varCall: VarCall): Expression {
        val index = getVarIndex(varCall.varName.substring)
        val result = ValueFromPointer(
            Operation(
                FramePointer(),
                Token("-", TokenType.PLUSMINUS),
                Literal(index)
            ), Token("*", TokenType.STAR)
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

    override fun visitComparison(comparison: Comparison): Expression {
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

    override fun visitInstanceGet(instanceGet: InstanceGet): Expression {
        val expression = evaluateExpression(instanceGet.expression)
        val index = getInstanceGetIndex(
            SymbolTableSingleton.getCurrentSymbolTable().getStruct((instanceGet.expression.resultType as StructType).typeName)!!,
            instanceGet.identifier.substring
        )
        if (expression is Expression.ValueFromPointer &&
            expression.expression is Operation &&
            (expression.expression as Operation).leftExpression is FramePointer &&
            ((expression.expression as Operation).rightExpression is Literal)
        ) {
            val temp = ((expression.expression as Operation).rightExpression as Literal).value as Int - index
            ((expression.expression as Operation).rightExpression as Literal).value = temp
            expression.resultType = instanceGet.resultType
            return expression
        }
        TODO()
    }

    private fun getInstanceGetIndex(struct: Struct, structSubvalue: String): Int {
        val requestedVar = struct.variables.first { pair -> pair.first == structSubvalue }
        val argumentsBefore = struct.variables.subList(0, struct.variables.indexOf(requestedVar))
        return argumentsBefore.fold(0) { acc, pair -> acc + pair.second.getLength() }
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

    override fun visitLogical(logical: Logical): Expression {
        logical.leftExpression = evaluateExpression(logical.leftExpression)
        logical.rightExpression = evaluateExpression(logical.rightExpression)
        return logical
    }

    override fun visitUnary(unary: Unary): Expression {
        unary.expression = evaluateExpression(unary.expression)
        return unary
    }

    override fun visitGrouping(grouping: Grouping): Expression {
        // TODO: Is a grouping still needed or can you return the evaluated subexpression?
        grouping.expression = evaluateExpression(grouping.expression)
        return grouping
    }

    private fun getSubtype(type: Type): Type? {
        return when (type) {
            is PointerType -> type.subtype
            is ArrayType -> type.subType
            else -> null
        }
    }

    override fun visitArrayGet(arrayGet: ArrayGet): Expression {
        val arrayGets = getArrayGets(arrayGet)
        if (arrayGets.last().expression is VarCall) {
            val index = getVariable((arrayGets.last().expression as VarCall).varName).offset
            var expression = getArrayGetOfVariable(arrayGet, arrayGets, index)
            if (arrayGet.resultType is PointerType) {
                expression = PointerGet(expression)
                expression.resultType = arrayGet.resultType
            }
            return expression
        }

        TODO()
    }

    private fun getArrayGetOfVariable(
        arrayGet: ArrayGet,
        arrayGets: List<ArrayGet>,
        index: Int
    ): Expression {
        var index1 = index
        if (arrayGet.expression.resultType is PointerType) {
            if (arrayGets.size != 1)
                error("Cannot have nested array gets with pointer types")

            if (arrayGet.inBrackets is Literal) {
                val firstVal = ValueFromPointer(
                    Operation(
                        FramePointer(),
                        Token("-", TokenType.PLUSMINUS),
                        Literal(index1)
                    ), Token("*", TokenType.STAR)
                )
                firstVal.resultType = arrayGet.expression.resultType
                val value =
                    ValueFromPointer(
                        Operation(
                            firstVal,
                            Token("-", TokenType.PLUSMINUS),
                            Literal(-getLiteralValue((arrayGet.inBrackets as Literal).value) * getSubtype(arrayGet.expression.resultType)!!.getLength())
                        ), Token("*", TokenType.STAR)

                    )
                value.resultType = arrayGet.resultType
                return value
            }
        }
        if (arrayGets.all { it.inBrackets is Literal }) {

            arrayGets.forEach {
                index1 -= it.resultType.getLength() * getLiteralValue((it.inBrackets as Literal).value)
            }
            val result = ValueFromPointer(
                Operation(
                    FramePointer(),
                    Token("-", TokenType.PLUSMINUS),
                    Literal(index1)
                ), Token("*", TokenType.STAR)
            )
            result.resultType = arrayGet.resultType
            return result
        }

        var resultAddition = Operation(
            evaluateExpression(arrayGets.last().inBrackets),
            Token("*", TokenType.STAR),
            Literal(arrayGets.last().resultType.getLength())
        )
        resultAddition.resultType = arrayGets.last().inBrackets.resultType
        arrayGets.dropLast(1).reversed().forEach {
            val mult = Operation(
                evaluateExpression(it.inBrackets),
                Token("*", TokenType.STAR),
                Literal(it.resultType.getLength())
            )
            mult.resultType = it.inBrackets.resultType
            resultAddition = Operation(
                resultAddition,
                Token("+", TokenType.PLUSMINUS),
                mult
            )
            resultAddition.resultType = mult.resultType
        }

        val extendTo64Bit = ExtendTo64Bit(resultAddition)
        extendTo64Bit.resultType = resultAddition.resultType

        val value = Operation(
            FramePointer(),
            Token("-", TokenType.PLUSMINUS),
            Literal(index1)
        )
        value.resultType = NormalType(NormalTypes.I64)
        val result = ValueFromPointer(
            Operation(
                value,
                Token("+", TokenType.PLUSMINUS),
                extendTo64Bit
            ), Token("*", TokenType.STAR)
        )
        result.resultType = arrayGet.resultType
        result.expression.resultType = NormalType(NormalTypes.I64)

        return result
    }

    private fun getArrayGets(arrayGet: ArrayGet): List<ArrayGet> {
        val result: MutableList<ArrayGet> = ArrayList()
        result.add(arrayGet)
        while (result.last().expression is ArrayGet) {
            result.add(result.last().expression as ArrayGet)
        }
        return result
    }

    private fun getVariable(varName: Token): VarNode {
        return getVariables().first { it.name == varName.substring }
    }




    override fun visitImportStmnt(importStmnt: Statement.ImportStmnt): Statement {
        return importStmnt
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet): Expression {
        pointerGet.expression = evaluateExpression(pointerGet.expression)
        return pointerGet
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer): Expression {
        valueFromPointer.expression = evaluateExpression(valueFromPointer.expression)
//        result.resultType = valueFromPointer.resultType
//        return result
        return valueFromPointer
    }

    override fun visitEmpty(empty: Statement.Empty): Statement {
        TODO("Not yet implemented")
    }


    fun rewriteMultiple(expressions: List<Statement>) {
        expressions.forEach { rewrite(it) }
    }

    override fun visitValueToPointer(valueToPointer: ValueToPointer): Expression {
        valueToPointer.value = evaluateExpression(valueToPointer.value)
        valueToPointer.pointer = evaluateExpression(valueToPointer.pointer)
        return valueToPointer
    }

    override fun visitExpressionStatement(expressionStatement: Statement.ExpressionStatement): Statement {
        expressionStatement.expression = evaluateExpression(expressionStatement.expression)
        return expressionStatement
    }

    override fun visitRegister(register: Register): Expression {
        return register
    }

    override fun acceptFramePointer(framePointer: FramePointer): Expression {
        return framePointer
    }

    override fun acceptExtendTo64Bits(extendTo64Bit: ExtendTo64Bit): Expression {
        extendTo64Bit.expression = evaluateExpression(extendTo64Bit.expression)

        return extendTo64Bit
    }
}