package com.cubearrow.cubelang.compiler.utils

import com.cubearrow.cubelang.common.ArrayType
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.compiler.Compiler.Companion.lengthsOfTypes

class TypeUtils {
    companion object{
        fun getLength(type: Type): Int{
            return when (type){
                is NormalType -> lengthsOfTypes.getOrDefault(type.typeName, 8)
                is PointerType -> 8
                is ArrayType -> getRawLength(type.subType) * type.count
                else -> 0
            }
        }

        fun getRawLength(type: Type): Int {
            return when(type){
                is NormalType -> lengthsOfTypes.getOrDefault(type.typeName, 8)
                is PointerType -> 8
                is ArrayType -> getRawLength(type.subType)
                else -> 0
            }
        }
    }
}