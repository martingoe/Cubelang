package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.parser.Expression

/**
 * The Type used to define arrays.
 *
 * @param subType The type of which an array is defined.
 * @param count The amount of arrays elements.
 */
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

    override fun getLength(): Int = this.subType.getRawLength() * this.count
    override fun getRawLength(): Int = this.subType.getRawLength()
}

/**
 * The [Type] used to define Pointers to other types.
 *
 * @param subtype The type that is being pointed to.
 */
class PointerType(var subtype: Type): Type{
    override fun getLength(): Int = 8

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other !is PointerType || subtype != other.subtype) return false
        return true
    }

    override fun hashCode(): Int {
        return subtype.hashCode()
    }

    override fun toString(): String {
        return "$subtype*"
    }

    override fun getRawLength(): Int = 8


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
    override fun getLength(): Int =
        Compiler.lengthsOfTypes.getOrDefault(this.typeName, 8)

    override fun getRawLength(): Int =
        Compiler.lengthsOfTypes.getOrDefault(this.typeName, 8)

}
interface Type {
    fun getLength(): Int
    fun getRawLength(): Int
    companion object {
        fun getType(type: Type?, value: Any?): Type {
            var valueToCompare = value
            if (value is Expression.Literal) valueToCompare = value.value
            return type ?: when (valueToCompare) {
                is Int -> NormalType("i32")
                is Double -> NormalType("double")
                is String -> NormalType("string")
                is Char -> NormalType("char")
                //is ClassInstance -> valueToCompare.className
                null -> NormalType("any")
                else -> NormalType("any")
            }
        }
    }
}