package com.cubearrow.cubelang.library

import com.cubearrow.cubelang.interpreter.Callable
import com.cubearrow.cubelang.interpreter.FunctionStorage
import com.cubearrow.cubelang.interpreter.VariableStorage

class PrintingLibrary {
    class Println : Callable {
        override val name = "println"
        override val args = listOf("value")

        override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage) {
            println(variableStorage.getCurrentVariables()["value"])
        }
    }
}