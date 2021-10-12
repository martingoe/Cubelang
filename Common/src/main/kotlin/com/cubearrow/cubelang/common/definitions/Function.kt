package com.cubearrow.cubelang.common.definitions

import com.cubearrow.cubelang.common.*

data class Function(var name: String, var args: Map<String, Type>, var returnType: Type?)

class DefinedFunctions {
    companion object {
        val definedFunctions = mutableMapOf(
            "io" to mutableListOf(
                Function("printChar", mapOf("value" to NormalType(NormalTypes.CHAR)), NoneType()),
                Function("printInt", mapOf("value" to NormalType(NormalTypes.I32)), NoneType()),
                Function("printShort", mapOf("value" to NormalType(NormalTypes.I8)), NoneType()),
                Function("printPointer", mapOf("value" to PointerType(NormalType(NormalTypes.ANY))), NoneType())
            ),
            "time" to mutableListOf(
                Function("getCurrentTime", mapOf(), NormalType(NormalTypes.I32))
            ),
            "IntMath" to mutableListOf(
                Function("min", mapOf("first" to NormalType(NormalTypes.I32), "sec" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I32)),
                Function("max", mapOf("first" to NormalType(NormalTypes.I32), "sec" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I32))
            )
        )
    }
}
