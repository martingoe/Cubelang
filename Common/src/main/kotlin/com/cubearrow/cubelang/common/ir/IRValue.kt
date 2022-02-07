package com.cubearrow.cubelang.common.ir

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