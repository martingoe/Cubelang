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

class NormalType(var typeName: String) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other !is NormalType) return false

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
        if (this is NormalType) {
            return Compiler.LENGTHS_OF_TYPES[this.typeName]!!
        } else if (this is ArrayType) {
            return this.subType.getLength() * this.count
        }
        return -1
    }

    fun getRawLength(): Int {
        if (this is NormalType) {
            return Compiler.LENGTHS_OF_TYPES[this.typeName]!!
        } else if (this is ArrayType) {
            return this.subType.getRawLength()
        }
        return 0
    }
}