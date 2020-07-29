package com.cubearrow.cubelang.interpreter

import java.util.*
import kotlin.collections.HashMap

class VariableStorage {
    var variables = Stack<HashMap<String, Any?>>()

    fun getVariablesInScope(scope: Int) :HashMap<String, Any?>{
        val result = HashMap<String, Any?>()
        variables.subList(0, scope - 1).forEach {result.putAll(it)}
        return result
    }
    fun getCurrentVariables() = getVariablesInScope(variables.size)
    fun popScope() = variables.pop()
    fun addVariableToCurrentScope(name: String, value: Any?) = variables.peek().put(name, value)
}