package com.cubearrow.cubelang.interpreter

class ClassInstance(val functionStorage: FunctionStorage, val variableStorage: VariableStorage, val className: String) {
    /**
     * Calls a function of the instance with the given arguments
     */
    fun callFunction(callable: Callable, args: List<Any?>): Any? {
        return callable.call(args, variableStorage, functionStorage)
    }
}