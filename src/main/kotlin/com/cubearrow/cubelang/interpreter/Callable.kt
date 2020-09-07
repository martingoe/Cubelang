package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.utils.ExpressionUtils

interface Callable {
    val name: String
    val args: List<String>
    fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage): Any?

    /**
     * Calls a function of the instance with the given arguments while adding the arguments to the function storage
     * @param args The argument values
     */
    fun call(args: List<Any?>, variableStorage: VariableStorage, functionStorage: FunctionStorage): Any? {
        variableStorage.addScope()

        //Add the argument variables to the variable stack
        for (i in this.args.indices) {
            variableStorage.addVariableToCurrentScope(this.args[i], ExpressionUtils.getType(null, args[i]), args[i])
        }
        val value = this.call(variableStorage, functionStorage)
        variableStorage.popScope()

        return value
    }
}