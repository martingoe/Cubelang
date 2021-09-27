package com.cubearrow.cubelang.ir.subcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.ir.IRCompiler
import com.cubearrow.cubelang.ir.IRCompilerContext
import com.cubearrow.cubelang.ir.getLength
import com.cubearrow.cubelang.ir.getStructType

class StructManagementCompiler(private val context: IRCompilerContext) {
    fun compileStructDefinition(structDefinition: Expression.StructDefinition) {
        val name = structDefinition.name.substring
        val variables = structDefinition.body.map { it.name.substring to it.type }
        context.structs[name] = Struct(name, variables)
        IRCompiler.lengthsOfTypes[name] = variables.fold(0) { acc, pair -> acc + pair.second.getLength() }
    }

    fun compileInstanceGet(instanceGet: Expression.InstanceGet) {
        getInstanceGetOrSet(instanceGet, IRType.COPY_FROM_STRUCT_GET, TemporaryRegister(context.increaseTempRegisterIndex()))
    }

    fun compileInstanceSet(instanceSet: Expression.InstanceSet) {
        val resultValue = context.getValue(instanceSet.value)
        getInstanceGetOrSet(instanceSet.instanceGet, IRType.COPY_TO_STRUCT_GET, resultValue)
    }

    private fun getInstanceGetOrSet(instanceGet: Expression.InstanceGet, irType: IRType, result: ValueType) {
        if (instanceGet.expression is Expression.VarCall) {
            val varName = (instanceGet.expression as Expression.VarCall).varName.substring
            val variable = context.getVariables()[varName] ?: error("Could not find the variable")
            val structType = getStructType(variable)
            context.pushValue(
                IRValue(
                    irType,
                    Variable(varName),
                    StructSubvalue(instanceGet.identifier.substring, structType),
                    result,
                    context.structs[structType.typeName]!!.variables.first { instanceGet.identifier.substring == it.first }.second
                )
            )
        } else {
            val (value, type) = context.getValueAndType(instanceGet.expression)
            val structType = getStructType(type)
            context.pushValue(
                IRValue(
                    irType,
                    value,
                    StructSubvalue(instanceGet.identifier.substring, structType),
                    result,
                    context.structs[structType.typeName]!!.variables.first { instanceGet.identifier.substring == it.first }.second
                )
            )
        }
    }
}