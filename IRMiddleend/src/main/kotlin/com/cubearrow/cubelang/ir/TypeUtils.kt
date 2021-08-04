package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.*

fun Type.getLength(): Int {
    return when (this){
        is PointerType -> 8
        is NormalType -> IRCompiler.lengthsOfTypes[this.type.toString()]!!
        is ArrayType -> this.subType.getLength() * this.count
        is StructType -> IRCompiler.lengthsOfTypes[this.typeName]!!
        else -> error("Something went wrong")
    }

}