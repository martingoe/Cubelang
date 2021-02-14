package com.cubearrow.cubelang.common.definitions

import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.Type

data class Function(var name: String, var args: Map<String, Type>, var returnType: Type?)

class DefinedFunctions {
    companion object {
        val definedFunctions = mutableMapOf(
            "io" to mutableListOf(
                Function("printChar", mapOf("value" to NormalType("char")), null),
                Function("printInt", mapOf("value" to NormalType("i32")), null),
                Function("printShort", mapOf("value" to NormalType("i8")), null),
                Function("printPointer", mapOf("value" to PointerType(NormalType("any"))), null)
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
