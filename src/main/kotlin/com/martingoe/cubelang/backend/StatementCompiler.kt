package com.martingoe.cubelang.backend

import com.martingoe.cubelang.backend.IRToASM.Companion.emitASMForIR
import com.martingoe.cubelang.backend.Utils.Companion.getIntTypeForLength
import com.martingoe.cubelang.backend.Utils.Companion.splitStruct
import com.martingoe.cubelang.backend.instructionselection.ASTToIRService
import com.martingoe.cubelang.backend.instructionselection.RegisterAllocation
import com.martingoe.cubelang.backend.instructionselection.currentRegister
import com.martingoe.cubelang.common.Expression
import com.martingoe.cubelang.common.Statement
import com.martingoe.cubelang.common.SymbolTableSingleton
import com.martingoe.cubelang.common.definitions.StandardLibraryFunctions
import com.martingoe.cubelang.common.ir.*
import com.martingoe.cubelang.common.ASMEmitter
import com.martingoe.cubelang.common.errors.ErrorManager
import com.martingoe.cubelang.common.tokens.TokenType
import com.martingoe.cubelang.middleend.treemodification.TreeRewriter
import java.io.File
import java.util.*

internal const val REGISTER_COUNT: Int = 6

/**
 * Compiles statements to NASM. This class also uses the [[ASTToIRService]] to compile the given expressions.
 */
class StatementCompiler(
    private val emitter: ASMEmitter,
    private var astToIRService: ASTToIRService,
    private val stdlibPath: String,
    private val errorManager: ErrorManager
) : Statement.StatementVisitor<Any?> {
    private var scope: Stack<Int> = Stack()
    private val registerAllocation = RegisterAllocation(emitter)

    private var lIndex = 2

    init {
        scope.push(-1)
        emitter.emit("section .text\nglobal main")
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
        functionDefinition.args.forEach {
            // TODO: Structs
            val res = Expression.VarCall(it.name).accept(TreeRewriter(scope, errorManager))
            res as Expression.ValueFromPointer
            val lit = (res.expression as Expression.Operation).rightExpression as Expression.Literal
            if(it.type.getLength() <= 8)
                emitter.emit(IRValue(IRType.POP_ARG, FramePointerOffset(lit.value.toString()), null, it.type))
            else{
                val splitLengths = splitStruct(it.type.getLength())
                var addedOffset = 0
                for(split in splitLengths){
                    emitter.emit(IRValue(IRType.POP_ARG, FramePointerOffset((lit.value as Int - addedOffset).toString()), null, getIntTypeForLength(split)))
                    addedOffset += split

                }

            }
        }
        emitASMForIR(emitter)

        evaluate(functionDefinition.body)
        emitter.emit(".l1:")
        emitter.emit("leave\nret")
        scope.pop()
    }

    private fun getFunctionOffset(): Int {
        // Align the offset to be 16n + 8
        var offset = 8

        val x = SymbolTableSingleton.getCurrentSymbolTable().getVariablesOffsetDefinedAtScope(scope)
        while(offset <= x) {
            offset += 16
        }
        return offset
    }


    override fun visitIfStmnt(ifStmnt: Statement.IfStmnt) {
        val labelIndex = lIndex
        lIndex += if (ifStmnt.elseBody != null) 2 else 1

        getJmpLogicalOrComparison(ifStmnt.condition, labelIndex, true)
        evaluate(ifStmnt.ifBody)

        if (ifStmnt.elseBody != null)
            emitter.emit("jmp .l${labelIndex + 1}")
        emitter.emit(".l${labelIndex}:")
        if (ifStmnt.elseBody != null) {
            evaluate(ifStmnt.elseBody!!)
            emitter.emit(".l${labelIndex + 1}:")
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
        val operation = if (useInverted) getInvJumpOperationFromComparator(comparison.comparator.substring) else
            getJmpOperationFromComparator(comparison.comparator.substring)

        emitter.emit("$operation .l${tempLabelIndex}")
    }

    override fun visitReturnStmnt(returnStmnt: Statement.ReturnStmnt) {
        returnStmnt.returnValue?.let { evaluateExpression(it) }
        emitter.emit("jmp .l1")
    }

    private fun evaluateExpression(returnValue: Expression) {
        astToIRService.emitCodeForExpression(returnValue)
        registerAllocation.linearScanRegisterAllocation()
        emitASMForIR(emitter)
    }


    override fun visitWhileStmnt(whileStmnt: Statement.WhileStmnt) {
        val resetLIndex = lIndex++
        val exitIndex = lIndex++
        emitter.emit("jmp .l$resetLIndex")
        emitter.emit(".l${exitIndex}:")
        evaluate(whileStmnt.body)
        emitter.emit(".l$resetLIndex:")

        getJmpLogicalOrComparison(whileStmnt.condition, exitIndex)

    }

    override fun visitForStmnt(forStmnt: Statement.ForStmnt) {
        evaluate(forStmnt.inBrackets[0])

        val resetLIndex = lIndex++
        val exitIndex = lIndex++

        emitter.emit("jmp .l$resetLIndex")
        emitter.emit(".l${exitIndex}:")
        evaluate(forStmnt.body)
        evaluate(forStmnt.inBrackets[2])

        emitter.emit(".l$resetLIndex:")

        getJmpLogicalOrComparison((forStmnt.inBrackets[1] as Statement.ExpressionStatement).expression, exitIndex)
    }

    override fun visitStructDefinition(structDefinition: Statement.StructDefinition) {
        return
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

    override fun visitImportStmnt(importStmnt: Statement.ImportStmnt) {
        SymbolTableSingleton.getCurrentSymbolTable().functions.addAll(StandardLibraryFunctions.definedFunctions[importStmnt.identifier.substring]!!)
        val name =
            (if (importStmnt.identifier.tokenType == TokenType.IDENTIFIER) File(stdlibPath).absolutePath + "/" + importStmnt.identifier.substring else File(
                importStmnt.identifier.substring.substringBeforeLast(".")
            ).absolutePath) + ".asm"

        emitter.emit("%include \"$name\"")
    }

    override fun visitEmpty(empty: Statement.Empty) {
        return
    }

    override fun visitExpressionStatement(expressionStatement: Statement.ExpressionStatement) {
        evaluateExpression(expressionStatement.expression)
    }

    fun evaluateList(expressions: List<Statement>) {
        expressions.forEach { evaluate(it) }
        SymbolTableSingleton.getCurrentSymbolTable().stringLiterals.forEach { (string, index) -> emitter.emit(".str_$index: db \"$string\", 0") }

    }

    override fun visitExternFunctionDefinition(externFunctionDefinition: Statement.ExternFunctionDefinition) {
        emitter.emit("extern ${externFunctionDefinition.name.substring}")
    }

}