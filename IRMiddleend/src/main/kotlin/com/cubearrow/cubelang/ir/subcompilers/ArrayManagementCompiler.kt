package com.cubearrow.cubelang.ir.subcompilers

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.ir.Variable
import com.cubearrow.cubelang.ir.IRCompilerContext
import com.cubearrow.cubelang.ir.getAllNestedArrayGets
import com.cubearrow.cubelang.ir.getTypeOfExpression

class ArrayManagementCompiler(private val context: IRCompilerContext) {
    fun compileArraySet(arraySet: Expression.ArraySet){
        val valueExpression = context.getValue(arraySet.value)

        compileArrayGet(arraySet.arrayGet)
        val arrayValue = context.resultList.last().arg0
        val inBracketValue = context.resultList.last().arg1
        val resultType = context.resultList.last().resultType
        context.resultList.removeLast()

        context.pushValue(
            IRValue(
                IRType.COPY_TO_ARRAY_ELEM,
                arrayValue,
                inBracketValue,
                valueExpression,
                resultType
            )
        )
    }
    fun compileArrayGet(arrayGet: Expression.ArrayGet){
        val previousTemporaryRegisterIndex = context.currentTempRegisterIndex
        val arrayGets = getAllNestedArrayGets(arrayGet)

        var currentType = getTypeOfExpression(arrayGets.last().expression, context)
        val expressionValue = getExpressionValue(currentType, arrayGets)

        val resultOffsetRegister = TemporaryRegister(context.increaseTempRegisterIndex())
        context.pushValue(IRValue(IRType.COPY, Literal("0"), null, resultOffsetRegister, NormalType(NormalTypes.I64)))

        for (arrayGetToAddToCount in arrayGets.reversed()) {
            val lengthToBeAdded = context.getValue(arrayGetToAddToCount.inBrackets)
            currentType = when (currentType) {
                is PointerType -> currentType.subtype
                is ArrayType -> currentType.subType
                else -> NoneType()
            }

            multiplyValueByTypeLength(lengthToBeAdded, currentType)
            addValueToRegister(resultOffsetRegister, lengthToBeAdded)
        }
        if (currentType is ArrayType) {
            currentType = PointerType(currentType.subType)
        }

        context.currentRegistersToSkip.add(resultOffsetRegister.index)
        context.currentTempRegisterIndex = previousTemporaryRegisterIndex
        context.pushValue(IRValue(IRType.COPY_FROM_ARRAY_ELEM, expressionValue, resultOffsetRegister, resultOffsetRegister, currentType!!))
    }

    private fun getExpressionValue(
        currentType: Type?,
        arrayGets: MutableList<Expression.ArrayGet>
    ) = if (currentType is ArrayType) {
        Variable((arrayGets.last().expression as Expression.VarCall).varName.substring)
    } else if (currentType is PointerType && arrayGets.size == 1) {
        context.getValue(arrayGets[0].expression)
    } else {
        // Should not get to this since the TypeChecker should have printed an error here
        error("The compiler does not currently support the given configuration of array get expressions.")
    }

    private fun addValueToRegister(
        resultRegister: TemporaryRegister,
        value: ValueType
    ) {
        context.pushValue(
            IRValue(
                IRType.PLUS_OP,
                resultRegister,
                value,
                resultRegister,
                NormalType(
                    NormalTypes.I32
                )
            )
        )
    }

    private fun multiplyValueByTypeLength(res: ValueType, currentType: Type) {
        context.pushValue(
            IRValue(
                IRType.MUL_OP,
                res,
                Literal(
                    currentType.getLength().toString()
                ),
                res,
                NormalType(
                    NormalTypes.I32
                )
            )
        )
    }
}