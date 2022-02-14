package com.cubearrow.cubelang

import com.cubearrow.cubelang.IRToASM.Companion.emitASMForIR
import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.Statement
import com.cubearrow.cubelang.common.SymbolTableSingleton
import com.cubearrow.cubelang.common.definitions.DefinedFunctions
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.nasm_rules.ASMEmitter
import com.cubearrow.cubelang.common.tokens.TokenType
import java.io.File
import java.util.*

class StatementCompiler(private val emitter: ASMEmitter, private val trie: Trie, private val stdlibPath: String) : Statement.StatementVisitor<Any?> {
    private val REGISTER_COUNT: Int = 6
    private var scope: Stack<Int> = Stack()
    var lIndex = 2

    init {
        scope.push(-1)
        emitter.emit("section .text\nglobal main")
    }

    private fun evaluate(statement: Statement) {
        currentRegister = 0
        statement.accept(this)
    }

    data class ActiveInterval(
        val regIndex: Int,
        val start: Int,
        val end: Int
    )

    data class LiveInterval(
        val virtualRegIndex: Int,
        val start: Int,
        var end: Int
    )


    private fun linearScanRegisterAllocation() {
        val intervals = getLiveIntervals(emitter.resultIRValues)
        val freeRegisters = (0 until REGISTER_COUNT).reversed().toMutableList()
        val active = mutableListOf<ActiveInterval>()
        for (i in intervals) {
            expireOldIntervals(i, active, freeRegisters)
            if (active.size == REGISTER_COUNT)
                spillAtInterval(i)
            else {
                val regIndex = freeRegisters.removeLast()
                setAllocatedRegister(regIndex, i)
                active.add(ActiveInterval(regIndex, i.start, i.end))
            }
        }
    }

    private fun setAllocatedRegister(regIndex: Int, interval: LiveInterval) {
        // TODO
        for (i in interval.start..interval.end) {
            if (emitter.resultIRValues[i].arg0 is TemporaryRegister) {
                if ((emitter.resultIRValues[i].arg0 as TemporaryRegister).index == interval.virtualRegIndex)
                    (emitter.resultIRValues[i].arg0 as TemporaryRegister).allocatedIndex = regIndex
            }
            if (emitter.resultIRValues[i].arg1 is TemporaryRegister) {
                if ((emitter.resultIRValues[i].arg1 as TemporaryRegister).index == interval.virtualRegIndex)
                    (emitter.resultIRValues[i].arg1 as TemporaryRegister).allocatedIndex = regIndex
            }
            if (emitter.resultIRValues[i].arg0 is RegOffset) {
                if ((emitter.resultIRValues[i].arg0 as RegOffset).temporaryRegister.index == interval.virtualRegIndex)
                    (emitter.resultIRValues[i].arg0 as RegOffset).temporaryRegister.allocatedIndex = regIndex
            }
            if (emitter.resultIRValues[i].arg1 is RegOffset) {
                if ((emitter.resultIRValues[i].arg1 as RegOffset).temporaryRegister.index == interval.virtualRegIndex)
                    (emitter.resultIRValues[i].arg1 as RegOffset).temporaryRegister.allocatedIndex = regIndex
            }
        }
    }

    private fun spillAtInterval(i: LiveInterval) {
        TODO("Not yet implemented")
    }

    private fun expireOldIntervals(i: LiveInterval, active: MutableList<ActiveInterval>, freeRegisters: MutableList<Int>) {
        for (j in active.sortedBy { it.end }) {
            if (j.end >= i.start)
                return
            active.remove(j)
            freeRegisters.add(j.regIndex)
        }
    }

    private fun getLiveIntervals(resultIRValues: ArrayList<IRValue>): List<LiveInterval> {
        val resultList = mutableListOf<LiveInterval>()
        for (i in resultIRValues.indices) {
            addLiveInterval(resultIRValues[i].arg0, resultList, i)
            addLiveInterval(resultIRValues[i].arg1, resultList, i)
        }
        resultList.sortBy { it.start }
        return resultList
    }

    private fun addLiveInterval(arg0: ValueType?, resultList: MutableList<LiveInterval>, index: Int) {
        arg0?.let {
            if (arg0 is TemporaryRegister) {
                // Register not yet accounted for
                if (resultList.none { it.virtualRegIndex == arg0.index }) {
                    resultList.add(LiveInterval(arg0.index, index, index))
                } else {
                    val indexOfFirst = resultList.indexOfFirst { it.virtualRegIndex == arg0.index }
                    resultList[indexOfFirst].end = index
                }
            }
            if(arg0 is RegOffset){
                // Register not yet accounted for
                if (resultList.none { it.virtualRegIndex == arg0.temporaryRegister.index }) {
                    resultList.add(LiveInterval(arg0.temporaryRegister.index, index, index))
                } else {
                    val indexOfFirst = resultList.indexOfFirst { it.virtualRegIndex == arg0.temporaryRegister.index }
                    resultList[indexOfFirst].end = index
                }
            }
        }
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
        functionDefinition.args.map { it as Statement.ArgumentDefinition }.forEach {
            // TODO: Structs
            val res = Expression.VarCall(it.name).accept(TreeRewriter(scope))
            res as Expression.ValueFromPointer
            val lit = (res.expression as Expression.Operation).rightExpression as Expression.Literal
            emitter.emit(IRValue(IRType.POP_ARG, Literal(lit.value.toString()), null, it.type))
        }
        emitASMForIR(emitter)

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
        linearScanRegisterAllocation()
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

    }

}