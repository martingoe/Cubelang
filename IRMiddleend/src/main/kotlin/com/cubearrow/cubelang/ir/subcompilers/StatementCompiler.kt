package com.cubearrow.cubelang.ir.subcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.NoneType
import com.cubearrow.cubelang.common.definitions.DefinedFunctions.Companion.definedFunctions
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.tokens.TokenType
import com.cubearrow.cubelang.ir.IRCompilerContext
import com.cubearrow.cubelang.ir.getInvJumpOperationFromComparator
import com.cubearrow.cubelang.ir.getJmpOperationFromComparator
import java.io.File

class StatementCompiler(private val context: IRCompilerContext) {
    private fun pushLabel(labelIndex: Int) = pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(labelIndex), NoneType()))
    private fun jmpToLabel(labelIndex: Int) = pushValue(IRValue(IRType.JMP, TemporaryLabel(labelIndex), null, null, NoneType()))
    private fun jmpToReturnLabel() = jmpToLabel(0)

    private fun pushValue(value: IRValue) =
        context.resultList.add(value)

    private fun evaluate(expression: Expression) =
        expression.accept(context.compilerInstance)

    fun compileBlockStmnt(blockStatement: Expression.BlockStatement) {
        for (statement in blockStatement.statements) {
            context.clearUsedRegisters()
            context.compilerInstance.evaluate(statement)
            if (statement is Expression.ReturnStmnt)
                break
        }
    }

    private fun resetIndexesForNewFunction() {
        context.currentRegistersToSave = 0
        context.currentTempLabelIndex = 1
    }

    fun compileFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        context.clearUsedRegisters()
        resetIndexesForNewFunction()
        context.functions.add(Function(functionDefinition.name.substring, functionDefinition.args.map { it as Expression.ArgumentDefinition }
            .associate { it.name.substring to it.type }, functionDefinition.type))
        context.variables.push(mutableMapOf())
        pushValue(IRValue(IRType.FUNC_DEF, FunctionLabel(functionDefinition.name.substring), null, null, functionDefinition.type))

        for (arg in functionDefinition.args)
            evaluate(arg)

        val pushIndex = context.resultList.size
        evaluate(functionDefinition.body)

        pushLabel(context.currentReturnLabelIndex)
        saveRegisters(context.currentRegistersToSave, pushIndex)
        pushValue(IRValue(IRType.RET, null, null, null, NoneType()))
        context.variables.pop()
    }


    private fun saveRegisters(previousTempRegisterIndex: Int, index: Int) {
        for (i in 1..previousTempRegisterIndex + 1)
            context.resultList.add(index, IRValue(IRType.PUSH_REG, TemporaryRegister(i), null, null, NoneType()))

        for (i in 1..previousTempRegisterIndex + 1)
            pushValue(IRValue(IRType.POP_REG, TemporaryRegister(i), null, null, NoneType()))
    }

    fun compileIfStmnt(ifStmnt: Expression.IfStmnt) {
        val labelIndex = context.currentTempLabelIndex
        context.currentTempLabelIndex += if (ifStmnt.elseBody != null) 2 else 1

        getJmpLogicalOrComparison(ifStmnt.condition, labelIndex)

        context.compilerInstance.evaluate(ifStmnt.ifBody)

        if (ifStmnt.elseBody != null)
            jmpToLabel(labelIndex + 1)
        pushLabel(labelIndex)
        if (ifStmnt.elseBody != null) {
            evaluate(ifStmnt.elseBody!!)
            pushLabel(labelIndex + 1)
        }
    }

    fun compileWhileStmnt(whileStmnt: Expression.WhileStmnt) {
        val firstLabel = context.currentTempLabelIndex++
        val secondLabel = context.currentTempLabelIndex++
        pushLabel(firstLabel)
        getJmpLogicalOrComparison(whileStmnt.condition, secondLabel)
        evaluate(whileStmnt.body)

        jmpToLabel(firstLabel)
        pushLabel(secondLabel)
    }

    fun compileForStmnt(forStmnt: Expression.ForStmnt) {
        evaluate(forStmnt.inBrackets[0])
        val resetLabel = context.currentTempLabelIndex++
        val exitLabel = context.currentTempLabelIndex++
        pushLabel(resetLabel)
        getJmpLogicalOrComparison(forStmnt.inBrackets[1], exitLabel)
        context.clearUsedRegisters()
        evaluate(forStmnt.body)
        context.clearUsedRegisters()

        evaluate(forStmnt.inBrackets[2])
        jmpToLabel(resetLabel)
        pushLabel(exitLabel)
    }

    private fun getJmpLogicalOrComparison(expression: Expression, tempLabelIndex: Int, useInverted: Boolean = true) {
        if (expression is Expression.Comparison) getJmpComparison(expression, tempLabelIndex, useInverted)
        else if (expression is Expression.Logical && expression.logical.tokenType == TokenType.AND) {
            getJmpLogicalOrComparison(expression.leftExpression, tempLabelIndex)
            getJmpLogicalOrComparison(expression.rightExpression, tempLabelIndex)
        } else if (expression is Expression.Logical && expression.logical.tokenType == TokenType.OR) {
            val afterLabelIndex = context.increaseTempRegisterIndex()
            getJmpLogicalOrComparison(expression.leftExpression, afterLabelIndex, false)
            getJmpLogicalOrComparison(expression.rightExpression, tempLabelIndex, true)
            pushLabel(afterLabelIndex)
        } else {
            val value = context.getValue(expression)
            pushValue(IRValue(IRType.JMP_NE, value, Literal("1"), TemporaryLabel(tempLabelIndex), NoneType()))
        }
    }

    private fun getJmpComparison(comparison: Expression.Comparison, tempLabelIndex: Int, useInverted: Boolean = true) {
        val left = context.getValue(comparison.leftExpression)
        val (right, resultType) = context.getValueAndType(comparison.rightExpression)

        pushValue(
            IRValue(
                IRType.CMP,
                left,
                right,
                null,
                resultType
            )
        )

        pushValue(
            IRValue(
                if (useInverted) getInvJumpOperationFromComparator(comparison.comparator.substring) else
                    getJmpOperationFromComparator(comparison.comparator.substring),
                null,
                null,
                TemporaryLabel(tempLabelIndex),
                resultType
            )
        )
    }

    fun compileReturnStmnt(returnStmnt: Expression.ReturnStmnt) {
        if (returnStmnt.returnValue != null) {
            val (value, type) = context.getValueAndType(returnStmnt.returnValue!!)
            pushValue(IRValue(IRType.COPY, value, null, TemporaryRegister(0), type))
        }
        jmpToReturnLabel()
    }

    fun compileImportStmnt(importStmnt: Expression.ImportStmnt, stdlibPath: String) {
        context.functions.addAll(definedFunctions[importStmnt.identifier.substring]!!)
        val name =
            (if (importStmnt.identifier.tokenType == TokenType.IDENTIFIER) File(stdlibPath).absolutePath + "/" + importStmnt.identifier.substring else File(
                importStmnt.identifier.substring.substringBeforeLast(".")
            ).absolutePath) + ".asm"
        pushValue(IRValue(IRType.INCLUDE, Literal(name), null, null, NoneType()))
    }

}