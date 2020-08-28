package com.cubearrow.cubelang.interpreter

import java.util.*
import kotlin.collections.HashMap

/**
 * The class storing the variables used. A [Stack] is used to save the variables in [MutableMap] form.
 */
class VariableStorage {
    private var variables = Stack<MutableMap<String, Any?>>()

    /**
     * Returns all of the variables up to a specific scope index
     */
    private fun getVariablesInScope(scope: Int): HashMap<String, Any?> {
        val result = HashMap<String, Any?>()
        variables.subList(0, scope).forEach { result.putAll(it) }
        return result
    }

    /**
     * Returns all of the variables in the current scope
     */
    fun getCurrentVariables() = getVariablesInScope(variables.size)
    fun popScope() {
        variables.pop()
    }

    /**
     * Adds a variable to the current scope, if there is no scope, one is added
     */
    fun addVariableToCurrentScope(name: String, value: Any?) {
        if(this.variables.empty()) addScope()
        variables.peek()[name] = value
    }

    /**
     * Pushes a new empty [HashMap] to the variable [Stack]
     */
    fun addScope() {
        variables.push(HashMap())
    }

    /**
     * Updates a variable to any new value.
     *
     * @throws VariableNotFoundException if the variable name is not found in the current scope
     */
    fun updateVariable(name: String, value: Any?): Any? {
        if (getCurrentVariables().containsKey(name)) {
            for (i in 0 until variables.size) {
                if (variables[i].containsKey(name)) {
                    variables[i][name] = value
                    return value
                }
            }
        }
        throw VariableNotFoundException()
    }

    /**
     * Adds multiple variables to the current scope, if there is no scope, one is added
     */
    fun addVariablesToCurrentScope(map: Map<String, Any?>) {
        if(this.variables.empty()) addScope()
        variables.peek().putAll(map)
    }

}

class VariableNotFoundException : RuntimeException()
