package com.cubearrow.cubelang.interpreter

class ClassInstance(val functionStorage: FunctionStorage, val variableStorage: VariableStorage) {
    fun callFunction(callable: Callable, args: List<Any?>): Any? {
        variableStorage.addScope()

        //Add the argument variables to the variable stack
        for (i in callable.args.indices) {
            variableStorage.addVariableToCurrentScope(callable.args[i], args[i])
        }
        val value = callable.call(variableStorage, functionStorage)
        variableStorage.popScope()

        return value
    }
}