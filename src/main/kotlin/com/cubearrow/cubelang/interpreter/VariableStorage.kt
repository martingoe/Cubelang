package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.main.Main
import java.util.*
import kotlin.collections.HashMap

/**
 * The class storing the variables used. A [Stack] is used to save the variables in [MutableMap] form.
 */
class VariableStorage {
    private var variables = Stack<MutableMap<String, Variable>>()

    /**
     * Returns all of the variables up to a specific scope index
     */
    private fun getVariablesInScope(scope: Int): HashMap<String, Variable> {
        val result = HashMap<String, Variable>()
        variables.subList(0, scope).forEach { result.putAll(it) }
        return result
    }

    /**
     * Returns all of the variables in the current scope
     */
    fun getCurrentVariables() = getVariablesInScope(variables.size)

    /**
     * Pops a scope off of the variable [Stack]
     */
    fun popScope() {
        variables.pop()
    }

    /**
     * Adds a variable to the current scope, if there is no scope, one is added
     */
    fun addVariableToCurrentScope(name: String, type: String, value: Any?) {
        if (this.variables.empty()) addScope()
        setValue(value, name, type, variables.size - 1)
    }

    private fun setValue(value: Any?, name: String, type: String, index: Int) {
        if (value == null) {
            variables[index][name] = Variable(name, null, type, VariableState.UNDEFINED)
        } else {
            val castedValue = castValue(value, type)
            variables[index][name] = Variable(name, castedValue, type, VariableState.DEFINED)
        }
    }

    private fun castValue(value: Any?, type: String): Any? {
        return try {
            when (type) {
                "int" -> value as Int
                "double" -> value as Double
                "string" -> value as String
                else -> {
                    if (value is ClassInstance) value else throw ClassCastException()
                }
            }
        } catch (e: ClassCastException) {
            Main.error(-1, -1, null, "Could not cast $value to the type $type")
            null
        }
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
                    setValue(value, name, variables[i][name]!!.type, i)
                    return value
                }
            }
        }
        throw VariableNotFoundException()
    }

    /**
     * Adds multiple variables to the current scope, if there is no scope, one is added
     */
    fun addVariablesToCurrentScope(map: Map<String, Variable>) {
        if (this.variables.empty()) addScope()
        variables.peek().putAll(map)
    }

}

class VariableNotFoundException : RuntimeException()
