package com.martingoe.cubelang.common.definitions

import com.martingoe.cubelang.common.*

/**
 * The class used to save function definitions.
 */
data class Function(var name: String, var args: Map<String, Type>, var returnType: Type?)

/**
 * The functions defined in the standard library
 */
class StandardLibraryFunctions {
    companion object {
        val definedFunctions = mutableMapOf(
            "io" to mutableListOf(
                Function("printChar", mapOf("value" to NormalType(NormalTypes.CHAR)), NoneType()),
                Function("printI32", mapOf("value" to NormalType(NormalTypes.I32)), NoneType()),
                Function("printI8", mapOf("value" to NormalType(NormalTypes.I8)), NoneType()),
                Function("printI16", mapOf("value" to NormalType(NormalTypes.I16)), NoneType()),
                Function("printI64", mapOf("value" to NormalType(NormalTypes.I16)), NoneType()),

                Function("printPointer", mapOf("value" to PointerType(NormalType(NormalTypes.ANY))), NoneType())
            ),
            "time" to mutableListOf(
                Function("getUnixTime", mapOf(), NormalType(NormalTypes.I32))
            ),
            "IntMath" to mutableListOf(
                Function("min", mapOf("first" to NormalType(NormalTypes.I32), "sec" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I32)),
                Function("max", mapOf("first" to NormalType(NormalTypes.I32), "sec" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I32))
            ),
            "random" to mutableListOf(
                Function("randomI32", mapOf("previous" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I32))
            ),
            "convert" to mutableListOf(
                Function("i8toi16", mapOf("value" to NormalType(NormalTypes.I8)), NormalType(NormalTypes.I16)),
                Function("i8tochar", mapOf("value" to NormalType(NormalTypes.I8)), NormalType(NormalTypes.CHAR)),
                Function("i8toi32", mapOf("value" to NormalType(NormalTypes.I8)), NormalType(NormalTypes.I32)),
                Function("i8toi64", mapOf("value" to NormalType(NormalTypes.I8)), NormalType(NormalTypes.I64)),


                Function("i16toi8", mapOf("value" to NormalType(NormalTypes.I16)), NormalType(NormalTypes.I8)),
                Function("i16tochar", mapOf("value" to NormalType(NormalTypes.I16)), NormalType(NormalTypes.CHAR)),
                Function("i16toi32", mapOf("value" to NormalType(NormalTypes.I16)), NormalType(NormalTypes.I32)),
                Function("i16toi64", mapOf("value" to NormalType(NormalTypes.I16)), NormalType(NormalTypes.I64)),

                Function("i64toi16", mapOf("value" to NormalType(NormalTypes.I64)), NormalType(NormalTypes.I16)),
                Function("i64tochar", mapOf("value" to NormalType(NormalTypes.I64)), NormalType(NormalTypes.CHAR)),
                Function("i64toi8", mapOf("value" to NormalType(NormalTypes.I64)), NormalType(NormalTypes.I8)),
                Function("i64toi32", mapOf("value" to NormalType(NormalTypes.I64)), NormalType(NormalTypes.I32)),

                Function("i32toi16", mapOf("value" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I16)),
                Function("i32tochar", mapOf("value" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.CHAR)),
                Function("i32toi8", mapOf("value" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I8)),
                Function("i32toi64", mapOf("value" to NormalType(NormalTypes.I32)), NormalType(NormalTypes.I64)),
            )
        )
    }
}
