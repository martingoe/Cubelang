package com.cubearrow.cubelang.common.ir

import com.cubearrow.cubelang.common.StructType
import com.cubearrow.cubelang.common.Type

class IRValue(val type: IRType, var arg0: ValueType?, var arg1: ValueType?, val result: ValueType?, val resultType: Type) {
    override fun toString(): String {
        return """$type, $arg0, $arg1, $result : $resultType
        """.trimMargin()
    }
}

interface ValueType

class TemporaryRegister(val index: Int) : ValueType {
    override fun toString(): String {
        return "r$index"
    }


    override fun hashCode(): Int {
        return index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemporaryRegister

        if (index != other.index) return false

        return true
    }
}

class Variable(val name: String, val extraOffset: Int = 0) : ValueType {
    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Variable

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}


class StructSubvalue(val name: String, val structType: StructType) : ValueType {
    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StructSubvalue

        if (name != other.name) return false
        if (structType != other.structType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + structType.hashCode()
        return result
    }


}

class FunctionLabel(val name: String) : ValueType {

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FunctionLabel

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class TemporaryLabel(private val index: Int) : ValueType {

    override fun toString(): String {
        return ".l${index}"
    }
}

class Literal(val value: String) : ValueType {
    override fun toString(): String {
        return value
    }
}
