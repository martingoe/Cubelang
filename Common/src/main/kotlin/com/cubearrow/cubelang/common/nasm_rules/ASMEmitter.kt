package com.cubearrow.cubelang.common.nasm_rules

import com.cubearrow.cubelang.common.ir.IRValue

class ASMEmitter {
    var finishedString = ""
    var resultIRValues = ArrayList<IRValue>()
    fun emit(string: String) {
        finishedString += string + "\n"
        println(string)
    }

    fun emit(irValue: IRValue) {
        resultIRValues.add(irValue)
    }
}
