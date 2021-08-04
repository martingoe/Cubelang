package com.cubearrow.cubelang.ircompiler.utils

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.ircompiler.X86IRCompiler

fun Type.getLength(): Int {
    return when (this){
        is PointerType -> 8
        is NormalType -> X86IRCompiler.lengthsOfTypes[this.type.toString()]!!
        is ArrayType -> this.subType.getLength() * this.count
        is StructType -> X86IRCompiler.lengthsOfTypes[this.typeName]!!
        else -> error("Something went wrong")
    }

}