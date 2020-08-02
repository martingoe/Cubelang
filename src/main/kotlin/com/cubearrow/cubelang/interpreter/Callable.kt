package com.cubearrow.cubelang.interpreter

interface Callable {
    val name: String
    val args: List<String>
    fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage): Any?
}