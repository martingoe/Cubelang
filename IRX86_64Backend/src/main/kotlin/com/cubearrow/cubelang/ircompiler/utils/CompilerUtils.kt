package com.cubearrow.cubelang.ircompiler.utils

import com.cubearrow.cubelang.common.ir.TemporaryRegister
import com.cubearrow.cubelang.ircompiler.X86IRCompiler

/**
 * Returns the register for the appropriate name and length.
 */
fun getRegister(baseName: String, length: Int): String {
    return try {
        baseName.toInt()
        when (length) {
            8 -> "r$baseName"
            4 -> "r${baseName}d"
            2 -> "r${baseName}w"
            1 -> "r${baseName}b"
            else -> ""
        }
    } catch (e: NumberFormatException) {
        when (length) {
            8 -> "r${baseName}"
            4 -> "e${baseName}"
            2 -> "${baseName[0]}h"
            1 -> "${baseName[0]}l"
            else -> ""
        }
    }
}

fun extendTo64Bit(register: TemporaryRegister, length: Int): String {
    var result = when (length) {
        1 -> "cbw\ncwde\ncdqe"
        2 -> "cwde\n" +
                "cdqe"
        4 -> "cdqe"
        8 -> ""
        else -> error("Unknown byte size")
    }
    if (register.index != 0)
        result = "\npush rax\nmov ${getRegister("ax", length)},${getRegister(X86IRCompiler.TEMPORARY_REGISTERS[register.index], length)}\n$result\nmov ${
            getRegister(
                X86IRCompiler.TEMPORARY_REGISTERS[register.index], 8
            )
        }, rax\npop rax"
    return result + "\n"
}

/**
 * Returns the ASM pointer size for getting a value from a pointer.
 */
fun getASMPointerLength(length: Int): String {
    return when (length) {
        1 -> "BYTE"
        2 -> "WORD"
        4 -> "DWORD"
        8 -> "QWORD"
        else -> error("Unknown byte size")
    }
}