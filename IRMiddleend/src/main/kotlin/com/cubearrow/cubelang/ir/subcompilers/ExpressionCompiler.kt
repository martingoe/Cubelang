package com.cubearrow.cubelang.ir.subcompilers

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.ir.Variable
import com.cubearrow.cubelang.common.SymbolTableSingleton
import com.cubearrow.cubelang.ir.IRCompilerContext
import com.cubearrow.cubelang.ir.getOperationFromString
import com.cubearrow.cubelang.ir.getTypeOfLiteral
import com.cubearrow.cubelang.ir.getValueOfLiteral

class ExpressionCompiler(private val context: IRCompilerContext) {
    fun compileValueFromPointer(valueFromPointer: Expression.ValueFromPointer){
        val value = context.getValue(valueFromPointer.expression)
        context.pushValue(
            IRValue(
                IRType.COPY_FROM_DEREF,
                value,
                null,
                TemporaryRegister(context.increaseTempRegisterIndex()),
                (context.resultList.last().resultType as PointerType).subtype
            )
        )
    }
    fun argumentDefinition(argumentDefinition: Expression.ArgumentDefinition){
        context.variables.last()[argumentDefinition.name.substring] = argumentDefinition.type
        context.pushValue(IRValue(IRType.VAR_DEF, null, null, Variable(argumentDefinition.name.substring), argumentDefinition.type))
        context.pushValue(IRValue(IRType.POP_ARG, null, null, Variable(argumentDefinition.name.substring), argumentDefinition.type))
    }

    fun compilePointerGet(pointerGet: Expression.PointerGet){
        var type = context.getVariables()[pointerGet.varCall.varName.substring]!!
        if (type is ArrayType)
            type = type.subType
        context.pushValue(
            IRValue(
                IRType.COPY_FROM_REF,
                Variable(pointerGet.varCall.varName.substring),
                null,
                TemporaryRegister(context.increaseTempRegisterIndex()),
                PointerType(type)
            )
        )
    }

    fun compileLiteral(literal: Expression.Literal){
        val valueOfLiteral = getValueOfLiteral(literal)
        context.pushValue(
            IRValue(
                IRType.COPY,
                Literal(valueOfLiteral),
                null,
                TemporaryRegister(context.increaseTempRegisterIndex()),
                getTypeOfLiteral(literal.value)
            )
        )
    }

    fun compileCall(call: Expression.Call){
        val previousTempRegisterIndex = context.currentTempRegisterIndex
        for (expression in call.arguments) {
            val (valueType, resultType) = context.getValueAndType(expression)
            context.currentTempRegisterIndex = previousTempRegisterIndex
            context.pushValue(IRValue(IRType.PUSH_ARG, valueType, null, null, resultType))
        }
        val functionName = call.callee.varName.substring
        val type = SymbolTableSingleton.getCurrentSymbolTable().functions.first { it.name == functionName }
        context.pushValue(IRValue(IRType.CALL, FunctionLabel(functionName), null, TemporaryRegister(0), type.returnType!!))
    }

    fun compileVarCall(varCall: Expression.VarCall){
        context.pushValue(
            IRValue(
                IRType.COPY,
                Variable(varCall.varName.substring),
                null,
                TemporaryRegister(context.increaseTempRegisterIndex()),
                context.getVariables()[varCall.varName.substring]!!
            )
        )
    }

    fun compileOperation(operation: Expression.Operation){
        val wasInSub = context.inSubOperation
        context.inSubOperation = true
        val previousTempRegisterIndex = context.currentTempRegisterIndex

        var (rhs, rhsType) = context.getValueAndType(operation.rightExpression)
        if (!wasInSub) {
            context.currentTempRegisterIndex = previousTempRegisterIndex
            context.increaseTempRegisterIndex()
            context.pushValue(IRValue(IRType.COPY, rhs, null, TemporaryRegister(context.currentTempRegisterIndex), rhsType))
            context.currentRegistersToSkip.add(context.currentTempRegisterIndex)
            rhs = TemporaryRegister(context.currentTempRegisterIndex)
            context.currentTempRegisterIndex = previousTempRegisterIndex
        }

        val lhs = context.getValue(operation.leftExpression)
        val result =
            if (lhs is TemporaryRegister) lhs else TemporaryRegister(context.increaseTempRegisterIndex())

        context.pushValue(
            IRValue(
                getOperationFromString(operation.operator.substring),
                lhs,
                rhs,
                result,
                rhsType
            )
        )
        if (!wasInSub) {
            context.inSubOperation = false
        }
        context.currentTempRegisterIndex = previousTempRegisterIndex
        context.increaseTempRegisterIndex()
    }

    fun compileUnary(unary: Expression.Unary) {
        val (value, type) = context.getValueAndType(unary.expression)
        if (unary.identifier.substring == "-") {
            context.pushValue(IRValue(IRType.NEG_UNARY, value, null, TemporaryRegister(context.increaseTempRegisterIndex()), type))
            return
        }
        TODO("Not yet implemented")
    }
}