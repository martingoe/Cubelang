package com.cubearrow.cubelang.interpreter.library

import com.cubearrow.cubelang.utils.NormalType
import com.cubearrow.cubelang.interpreter.Callable
import com.cubearrow.cubelang.interpreter.FunctionStorage
import com.cubearrow.cubelang.interpreter.VariableStorage

class PrintingLibrary {
    class printString : Callable {
        override val name = "printString"
        override val args = mapOf("value" to NormalType("string"))

        override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage) {
            println(variableStorage.getCurrentVariables()["value"]?.value)
        }
    }
    class printInt : Callable {
        override val name = "printInt"
        override val args = mapOf("value" to NormalType("int"))

        override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage) {
            println(variableStorage.getCurrentVariables()["value"]?.value)
        }
    }
    class printDouble : Callable {
        override val name = "printDouble"
        override val args = mapOf("value" to NormalType("double"))

        override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage) {
            println(variableStorage.getCurrentVariables()["value"]?.value)
        }
    }
}