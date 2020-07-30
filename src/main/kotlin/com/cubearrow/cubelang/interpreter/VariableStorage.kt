package com.cubearrow.cubelang.interpreter

import java.util.*
import kotlin.collections.HashMap

class VariableStorage {
    private var variables = Stack<HashMap<String, Any?>>()

    private fun getVariablesInScope(scope: Int): HashMap<String, Any?> {
        val result = HashMap<String, Any?>()
        variables.subList(0, scope).forEach { result.putAll(it) }
        return result
    }

    fun getCurrentVariables() = getVariablesInScope(variables.size)
    fun popScope() {
        variables.pop()
    }

    fun addVariableToCurrentScope(name: String, value: Any?) {
        if (getCurrentVariables().containsKey(name)) {
            variables.stream().filter { it.containsKey(name) }.findFirst().get()[name] = value
        } else {
            variables.peek()[name] = value
        }
    }

    fun addScope() {
        variables.push(HashMap())
    }

}