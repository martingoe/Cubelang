package com.cubearrow.cubelang.common.definitions

import com.cubearrow.cubelang.common.NoneType
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.Type

data class Function(var name: String, var args: Map<String, Type>, var returnType: Type?)

class DefinedFunctions {
    companion object {
        val definedFunctions = mutableMapOf(
            "io" to mutableListOf(
                Function("printChar", mapOf("value" to NormalType("char")), NoneType()),
                Function("printInt", mapOf("value" to NormalType("i32")), NoneType()),
                Function("printShort", mapOf("value" to NormalType("i8")), NoneType()),
                Function("printPointer", mapOf("value" to PointerType(NormalType("any"))), NoneType())
            ),
            "time" to mutableListOf(
                Function("getCurrentTime", mapOf(), NormalType("i32"))
            ),
            "IntMath" to mutableListOf(
                Function("min", mapOf("first" to NormalType("i32"), "sec" to NormalType("i32")), NormalType("i32")),
                Function("max", mapOf("first" to NormalType("i32"), "sec" to NormalType("i32")), NormalType("i32"))
            )
        )
    }
}
