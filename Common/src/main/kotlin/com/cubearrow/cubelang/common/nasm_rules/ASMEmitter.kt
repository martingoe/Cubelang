package com.cubearrow.cubelang.common.nasm_rules

class ASMEmitter {
    var finishedString = ""
    fun emit(string: String){
        finishedString += string + "\n"
        println(string)
    }
}
