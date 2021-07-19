package com.cubearrow.cubelang.ircompiler.utils

import com.cubearrow.cubelang.common.ArrayType
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.ircompiler.X86IRCompiler

fun Type.getLength(): Int {
    return when (this){
        is PointerType -> 8
        is NormalType -> X86IRCompiler.lengthsOfTypes[this.typeName]!!
        is ArrayType -> this.subType.getLength() * this.count
        else -> error("Something went wrong")
    }

}