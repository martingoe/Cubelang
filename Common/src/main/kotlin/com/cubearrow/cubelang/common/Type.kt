package com.cubearrow.cubelang.common

enum class NormalTypes{
    I8, I16, I32, I64, CHAR, ANY
}
/**
 * The Type used to define arrays.
 *
 * @param subType The type of which an array is defined.
 * @param count The amount of arrays elements.
 */
class ArrayType(var subType: Type, var count: Int) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true


        if (other !is ArrayType) return false

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

/**
 * The [Type] used to define Pointers to other types.
 *
 * @param subtype The type that is being pointed to.
 */
class PointerType(var subtype: Type) : Type {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointerType || subtype != other.subtype) return false
        return true
    }

    override fun hashCode(): Int {
        return subtype.hashCode()
    }

    override fun toString(): String {
        return "$subtype*"
    }
}

class NormalType(var type: NormalTypes) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NormalType) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return type.toString()
    }
}

class StructType(var typeName: String) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StructType) return false

        if (typeName == "any" || other.typeName == "any") return true
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

class NoneType : Type{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "null"
    }
}
interface Type
