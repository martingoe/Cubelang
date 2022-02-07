package com.cubearrow.cubelang

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.Statement
import com.cubearrow.cubelang.common.SymbolTableSingleton
import com.cubearrow.cubelang.common.definitions.DefinedFunctions
import com.cubearrow.cubelang.common.nasm_rules.ASMEmitter
import com.cubearrow.cubelang.common.tokens.TokenType
import java.io.File
import java.util.*

class StatementCompiler(private val emitter: ASMEmitter, private val trie: Trie, private val stdlibPath: String) : Statement.StatementVisitor<Any?> {
    var scope: Stack<Int> = Stack()
    var lIndex = 2

    init {
        scope.push(-1)
    }

    private fun evaluate(statement: Statement) {
        currentRegister = 0
        statement.accept(this)
    }

    override fun visitVarInitialization(varInitialization: Statement.VarInitialization) {

        TODO("Not yet implemented")
    }

    override fun visitFunctionDefinition(functionDefinition: Statement.FunctionDefinition) {
        scope.push(scope.pop() + 1)
        scope.push(-1)
        lIndex = 2
        emitter.emit("${functionDefinition.name.substring}:")
        emitter.emit("push rbp")
        emitter.emit("mov rbp, rsp")
        emitter.emit("sub rsp, ${getFunctionOffset()}")
        evaluate(functionDefinition.body)
        emitter.emit(".l1:")
        emitter.emit("leave\nret")
        scope.pop()
    }

    private fun getFunctionOffset(): Int {
        return SymbolTableSingleton.getCurrentSymbolTable().getVariablesOffsetDefinedAtScope(scope)
    }


    override fun visitIfStmnt(ifStmnt: Statement.IfStmnt) {
        val labelIndex = lIndex
        lIndex += if (ifStmnt.elseBody != null) 2 else 1

        getJmpLogicalOrComparison(ifStmnt.condition, labelIndex)

        evaluate(ifStmnt.ifBody)

        if (ifStmnt.elseBody != null)
            emitter.emit("jmp .l${labelIndex + 1}")
        emitter.emit(".l${labelIndex}:")
        if (ifStmnt.elseBody != null) {
            evaluate(ifStmnt.elseBody!!)
            emitter.emit(".l${labelIndex + 1}")
        }
    }

    private fun getJmpLogicalOrComparison(condition: Expression, labelIndex: Int, useInverted: Boolean = false) {
        if (condition is Expression.Comparison) getJmpComparison(condition, labelIndex, useInverted)
        else if (condition is Expression.Logical && condition.logical.tokenType == TokenType.AND) {
            getJmpLogicalOrComparison(condition.leftExpression, labelIndex)
            getJmpLogicalOrComparison(condition.rightExpression, labelIndex)
        } else if (condition is Expression.Logical && condition.logical.tokenType == TokenType.OR) {
            val afterLabelIndex = lIndex++
            getJmpLogicalOrComparison(condition.leftExpression, afterLabelIndex, false)
            getJmpLogicalOrComparison(condition.rightExpression, labelIndex, true)
            emitter.emit(".l${afterLabelIndex}:")
        } else {
            val value = evaluateExpression(condition)
            emitter.emit("cmp ${value}, 1\\jne .l${labelIndex}")
        }
    }

    private fun getJmpComparison(comparison: Expression.Comparison, tempLabelIndex: Int, useInverted: Boolean = true) {
        evaluateExpression(comparison)
        val operation =                 if (useInverted) getInvJumpOperationFromComparator(comparison.comparator.substring) else
            getJmpOperationFromComparator(comparison.comparator.substring)

        emitter.emit("$operation .l${tempLabelIndex}")
    }

    private fun getInvJumpOperationFromComparator(comparisonString: String): String {
        return when (comparisonString) {
            "==" -> "jne"
            "!=" -> "je"
            "<" -> "jge"
            "<=" -> "jg"
            ">" -> "jle"
            ">=" -> "jl"
            else -> error("Could not find the requested operation")
        }
    }

    private fun getJmpOperationFromComparator(comparisonString: String): String {
        return when (comparisonString) {
            "==" -> "leq"
            "!=" -> "jne"
            "<" -> "jl"
            "<=" -> "jle"
            ">" -> "jg"
            ">=" -> "jge"
            else -> error("Could not find the requested operation")
        }
    }
    override fun visitReturnStmnt(returnStmnt: Statement.ReturnStmnt) {
        returnStmnt.returnValue?.let { evaluateExpression(it) }
        emitter.emit("jmp .l1")
    }

    private fun evaluateExpression(returnValue: Expression) {
        trie.emitCodeForExpression(returnValue)
    }

    override fun visitWhileStmnt(whileStmnt: Statement.WhileStmnt) {
        val resetLIndex = lIndex++
        val exitIndex = lIndex++
        emitter.emit(".l$resetLIndex")
        evaluate(whileStmnt.body)
        emitter.emit("jmp .l$resetLIndex")
        emitter.emit(".l${exitIndex}")
    }

    override fun visitForStmnt(forStmnt: Statement.ForStmnt) {
        evaluate(forStmnt.inBrackets[0])

        val resetLIndex = lIndex++
        val exitIndex = lIndex++
        emitter.emit(".l$resetLIndex")
        getJmpLogicalOrComparison((forStmnt.inBrackets[1] as Statement.ExpressionStatement).expression, resetLIndex)
        evaluate(forStmnt.body)
        evaluate(forStmnt.inBrackets[2])
        emitter.emit("jmp .l$resetLIndex")

        emitter.emit(".l${exitIndex}")
    }

    override fun visitStructDefinition(structDefinition: Statement.StructDefinition) {
        return
    }

    override fun visitInstanceSet(instanceSet: Statement.InstanceSet): Any? {
        TODO("Not yet implemented")
    }

    override fun visitArgumentDefinition(argumentDefinition: Statement.ArgumentDefinition): Any? {
        TODO("Not yet implemented")
    }

    override fun visitBlockStatement(blockStatement: Statement.BlockStatement) {
        scope.push(scope.pop() + 1)
        scope.push(-1)
        for (stmnt in blockStatement.statements) {
            evaluate(stmnt)
        }
        scope.pop()
    }

    override fun visitArraySet(arraySet: Statement.ArraySet): Any? {
        TODO("Not yet implemented")
    }

    override fun visitImportStmnt(importStmnt: Statement.ImportStmnt) {
        SymbolTableSingleton.getCurrentSymbolTable().functions.addAll(DefinedFunctions.definedFunctions[importStmnt.identifier.substring]!!)
        val name =
            (if (importStmnt.identifier.tokenType == TokenType.IDENTIFIER) File(stdlibPath).absolutePath + "/" + importStmnt.identifier.substring else File(
                importStmnt.identifier.substring.substringBeforeLast(".")
            ).absolutePath) + ".asm"

        emitter.emit("%include $name")
    }

    override fun visitEmpty(empty: Statement.Empty) {
    return
    }

    override fun visitExpressionStatement(expressionStatement: Statement.ExpressionStatement) {
        evaluateExpression(expressionStatement.expression)
    }

    fun evaluateList(expressions: List<Statement>) {
        expressions.forEach { evaluate(it) }

    }

}