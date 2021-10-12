package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.ir.IRValue
import com.cubearrow.cubelang.common.ir.ValueType
import java.util.*

data class IRCompilerContext(
    var inSubOperation: Boolean = false,
    var currentTempRegisterIndex: Int = 0,
    var currentTempLabelIndex: Int = 0,
    var currentRegistersToSkip: MutableList<Int> = mutableListOf(),
    var currentReturnLabelIndex: Int = 0,

    var currentRegistersToSave: Int = 0,

    var resultList: MutableList<IRValue> = mutableListOf(),
    var variables: Stack<MutableMap<String, Type>> = Stack(),
    val functions: MutableList<Function> = mutableListOf(),
    val structs: MutableMap<String, Struct> = mutableMapOf(),
    val compilerInstance: IRCompiler
) {
    fun getVariables(): MutableMap<String, Type> {
        return variables.fold(mutableMapOf()) { acc, x -> acc.putAll(x); acc }
    }

    fun pushValue(value: IRValue) = resultList.add(value)
    fun clearUsedRegisters() {
        currentTempRegisterIndex = 0
        currentRegistersToSkip.clear()
    }

    fun getValue(value: Expression): ValueType {
        compilerInstance.evaluate(value)
        return resultList.last().result!!
    }

    fun getValueAndType(value: Expression): Pair<ValueType, Type> {
        compilerInstance.evaluate(value)
        return Pair(resultList.last().result!!, resultList.last().resultType)
    }

    fun increaseTempRegisterIndex(): Int {
        val beforeIncrease = currentTempRegisterIndex
        currentRegistersToSave = Integer.max(currentRegistersToSave, beforeIncrease)
        while (currentRegistersToSkip.contains(currentTempRegisterIndex + 1))
            currentTempRegisterIndex++
        currentTempRegisterIndex++
        return beforeIncrease
    }
}