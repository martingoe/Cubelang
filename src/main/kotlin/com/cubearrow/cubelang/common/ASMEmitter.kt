package com.cubearrow.cubelang.common

import com.cubearrow.cubelang.common.ir.IRValue

/**
 * A class to save finished ASM and IR values.
 */
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
