package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.errors.ErrorManager
import com.cubearrow.cubelang.common.ir.IRType
import com.cubearrow.cubelang.common.ir.IRValue
import com.cubearrow.cubelang.common.ir.Variable
import com.cubearrow.cubelang.ir.subcompilers.*


class IRCompiler(
    private val expressions: List<Expression>,
    private val stdlibPath: String,
    private val definedFunctions: Map<String, List<Function>>,
    private val errorManager: ErrorManager
) : Expression.ExpressionVisitor<Unit> {
    val context = IRCompilerContext(compilerInstance = this)
    private fun resultList() = context.resultList
    private fun variables() = context.variables
    private val statementCompiler = StatementCompiler(context)
    private val copyCompiler = CopyCompiler(context)
    private val arrayManagementCompiler = ArrayManagementCompiler(context)
    private val expressionCompiler = ExpressionCompiler(context)
    private val structManagementCompiler = StructManagementCompiler(context)

    companion object {
        private val excludedFromCleaningInstructions =
            listOf(
                IRType.COPY_FROM_REG_OFFSET,
                IRType.PLUS_OP,
                IRType.DIV_OP,
                IRType.MINUS_OP,
                IRType.MUL_OP,
                IRType.COPY_FROM_DEREF,
                IRType.COPY_FROM_ARRAY_ELEM,
                IRType.COPY_TO_ARRAY_ELEM
            )
        val lengthsOfTypes = mutableMapOf("I8" to 1, "I16" to 2, "I32" to 4, "I64" to 8, "CHAR" to 1)
    }

    init {
        context.variables.push(mutableMapOf())
    }


    fun parse(): List<IRValue> {
        TypeChecker(expressions, errorManager, definedFunctions).checkTypes()
        expressions.filterIsInstance<Expression.FunctionDefinition>().forEach { it ->
            context.functions.add(
                Function(
                    it.name.substring,
                    it.args.map { it as Expression.ArgumentDefinition }.associate { it.name.substring to it.type },
                    it.type
                )
            )
        }

        for (expression in expressions) {
            evaluate(expression)
        }
        return cleanUpCopies(resultList())
    }

    private fun cleanUpCopies(list: List<IRValue>): List<IRValue> {
        val result = mutableListOf<IRValue>()
        var i = 0
        while (i < list.size) {
            if (list[i].type == IRType.COPY && list[i].arg0 == list[i].result) {
                i++
                continue
            } else if (list[i].type == IRType.COPY && list.size >= i && list[i + 1].arg0 == list[i].result &&
                !excludedFromCleaningInstructions.contains(list[i + 1].type) && list[i].result !is Variable
            ) {
                val x = list[i + 1]
                x.arg0 = list[i].arg0
                i++
                result.add(x)
            } else if (list[i].type == IRType.COPY && list.size <= i && list[i + 1].arg1 == list[i].result) {
                val x = list[i + 1]
                x.arg1 = list[i].arg0
                i++
                result.add(x)
            } else {
                result.add(list[i])
            }
            i++
        }
        return result.toList()
    }


    private fun pushValue(value: IRValue) {
        resultList().add(value)
    }

    fun evaluate(expression: Expression) {
        expression.accept(this)
    }

    override fun visitAssignment(assignment: Expression.Assignment) {
        copyCompiler.compileAssignment(assignment)
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization) {
        copyCompiler.compileVarInitialization(varInitialization)
    }

    override fun visitOperation(operation: Expression.Operation) {
        expressionCompiler.compileOperation(operation)
    }


    override fun visitCall(call: Expression.Call) {
        expressionCompiler.compileCall(call)
    }

    override fun visitLiteral(literal: Expression.Literal) {
        expressionCompiler.compileLiteral(literal)
    }

    override fun visitVarCall(varCall: Expression.VarCall) {
        expressionCompiler.compileVarCall(varCall)
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        statementCompiler.compileFunctionDefinition(functionDefinition)
    }


    override fun visitComparison(comparison: Expression.Comparison) {
        TODO("Not yet implemented")
    }


    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt) {
        statementCompiler.compileIfStmnt(ifStmnt)
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt) {
        statementCompiler.compileReturnStmnt(returnStmnt)
    }


    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt) {
        statementCompiler.compileWhileStmnt(whileStmnt)
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt) {
        statementCompiler.compileForStmnt(forStmnt)
    }

    override fun visitStructDefinition(structDefinition: Expression.StructDefinition) {
        structManagementCompiler.compileStructDefinition(structDefinition)
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet) {
        structManagementCompiler.compileInstanceGet(instanceGet)
    }


    override fun visitInstanceSet(instanceSet: Expression.InstanceSet) {
        structManagementCompiler.compileInstanceSet(instanceSet)
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition) {
        variables().last()[argumentDefinition.name.substring] = argumentDefinition.type
        pushValue(IRValue(IRType.VAR_DEF, null, null, Variable(argumentDefinition.name.substring), argumentDefinition.type))
        pushValue(IRValue(IRType.POP_ARG, null, null, Variable(argumentDefinition.name.substring), argumentDefinition.type))
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement) {
        statementCompiler.compileBlockStmnt(blockStatement)
    }

    override fun visitLogical(logical: Expression.Logical) {
        TODO("Not yet implemented")
    }

    override fun visitUnary(unary: Expression.Unary) {
        expressionCompiler.compileUnary(unary)
    }

    override fun visitGrouping(grouping: Expression.Grouping) {
        evaluate(grouping.expression)
    }


    override fun visitArrayGet(arrayGet: Expression.ArrayGet) {
        arrayManagementCompiler.compileArrayGet(arrayGet)
    }

    override fun visitArraySet(arraySet: Expression.ArraySet) {
        arrayManagementCompiler.compileArraySet(arraySet)
    }

    override fun visitImportStmnt(importStmnt: Expression.ImportStmnt) {
        statementCompiler.compileImportStmnt(importStmnt, stdlibPath)
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet) {
        expressionCompiler.compilePointerGet(pointerGet)
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer) {
        expressionCompiler.compileValueFromPointer(valueFromPointer)
    }

    override fun visitEmpty(empty: Expression.Empty) {
        return
    }
}