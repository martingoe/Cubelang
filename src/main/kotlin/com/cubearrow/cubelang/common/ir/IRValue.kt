package com.cubearrow.cubelang.common.ir

import com.cubearrow.cubelang.common.Type

/**
 * A single Intermediate Representation value.
 */
class IRValue(val type: IRType, var arg0: ValueType?, var arg1: ValueType?, val resultType: Type) {
    override fun toString(): String {
        return """$type, $arg0, $arg1 : $resultType
        """.trimMargin()
    }
}

interface ValueType

class TemporaryRegister(val index: Int) : ValueType {
    var allocatedIndex: Int = -1
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

class RegOffset(val temporaryRegister: TemporaryRegister, var offset: String): ValueType {
    override fun toString(): String {
        return "[r$temporaryRegister - $offset]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegOffset

        if (temporaryRegister != other.temporaryRegister) return false

        return true
    }

    override fun hashCode(): Int {
        return temporaryRegister.hashCode()
    }


}


class FramePointerOffset(val literal: String, val temporaryRegister: TemporaryRegister? = null, var offset: String? = null): ValueType {
    override fun toString(): String {
        return "[r$temporaryRegister - $offset]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegOffset

        if (temporaryRegister != other.temporaryRegister) return false

        return true
    }

    override fun hashCode(): Int {
        return temporaryRegister.hashCode()
    }


}
class FramePointer : ValueType {
    override fun toString(): String {
        return "rbp"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
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

class IRLiteral(val value: String) : ValueType {
    override fun toString(): String {
        return value
    }
}