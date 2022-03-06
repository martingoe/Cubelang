package com.cubearrow.cubelang.common

import com.cubearrow.cubelang.common.ir.IRValue

class ASMEmitter {
    var finishedString = ""
    var resultIRValues = ArrayList<IRValue>()
    fun emit(string: String) {
        finishedString += string + "\n"
    }

    fun emit(irValue: IRValue) {
        resultIRValues.add(irValue)
    }
}
