package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.compiler.Compiler

class ArrayType(var subType: Type, var count: Int) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true


        if(other !is ArrayType) return false

        if (subType != other.subType) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = subType.hashCode()
        result = 31 * result + count.hashCode()
        return result
    }

    override fun toString(): String {
        return "[$subType : $count]"
    }
}

class PointerType(var normalType: Type): Type{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other !is PointerType || normalType != other.normalType) return false
        return true
    }

    override fun hashCode(): Int {
        return normalType.hashCode()
    }

    override fun toString(): String {
        return "$normalType*"
    }
}

class NormalType(var typeName: String) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other !is NormalType) return false

        if(typeName == "any" || other.typeName == "any") return true
        if (typeName != other.typeName) return false

        return true
    }

    override fun hashCode(): Int {
        return typeName.hashCode()
    }

    override fun toString(): String {
        return typeName
    }
}
interface Type {
    fun getLength(): Int {
        return when (this) {
            is NormalType -> {
                Compiler.LENGTHS_OF_TYPES[this.typeName]!!
            }
            is PointerType -> 8
            is ArrayType -> {
                this.subType.getRawLength() * this.count
            }
            else -> 0
        }
    }

    fun getRawLength(): Int {
        return when (this) {
            is NormalType -> Compiler.LENGTHS_OF_TYPES[this.typeName]!!
            is PointerType -> 8
            is ArrayType -> {
                this.subType.getRawLength()
            }
            else -> 0
        }
    }
}