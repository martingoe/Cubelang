package com.cubearrow.cubelang.ir.subcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.StructType
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.ir.IRCompilerContext
import com.cubearrow.cubelang.ir.getIntTypeFromLength
import com.cubearrow.cubelang.ir.splitStruct

class CopyCompiler(private val context: IRCompilerContext) {
    fun compileVarInitialization(varInitialization: Expression.VarInitialization) {
        context.clearUsedRegisters()

        if (varInitialization.valueExpression != null)
            compileCopy(varInitialization.name.substring, varInitialization.valueExpression!!)
    }

    private fun compileCopy(name: String, value: Expression) {
        if (value is Expression.ValueFromPointer) {
            val (valueIRType, type) = context.getValueAndType(value.expression)
            val resultType = (type as PointerType).subtype
            if (resultType is StructType) {
                copyFromStruct(resultType, valueIRType, name)
            }

        } else {
            val (valueType, resultType) = context.getValueAndType(value)
            context.pushValue(IRValue(IRType.COPY, valueType, null, Variable(name), resultType))
        }
    }

    private fun copyFromStruct(
        resultType: StructType,
        valueIRType: ValueType,
        name: String
    ) {
        val splitLength = splitStruct(resultType.getLength())
        var completedOffset = 0
        for (i in splitLength) {
            context.pushValue(
                IRValue(
                    IRType.COPY_FROM_REG_OFFSET,
                    valueIRType,
                    Literal(completedOffset.toString()),
                    TemporaryRegister(context.currentTempRegisterIndex),
                    getIntTypeFromLength(i)
                )
            )
            context.pushValue(IRValue(IRType.COPY, context.resultList.last().result, null, Variable(name, completedOffset), getIntTypeFromLength(i)))
            completedOffset += i
        }
    }

    fun compileAssignment(assignment: Expression.Assignment) {
        compileCopy(assignment.name.substring, assignment.valueExpression)

    }
}