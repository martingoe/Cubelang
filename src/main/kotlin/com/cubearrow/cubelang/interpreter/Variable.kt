package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.utils.Type
import com.cubearrow.cubelang.utils.NullValue

enum class VariableState{
    UNDEFINED, DEFINED
}
data class Variable(val name: String, val value: Any? = NullValue(), val type: Type, val VariableState: VariableState)
