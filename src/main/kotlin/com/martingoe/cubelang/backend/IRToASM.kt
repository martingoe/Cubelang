package com.martingoe.cubelang.backend

import com.martingoe.cubelang.common.ASMEmitter
import com.martingoe.cubelang.common.RegisterConfig
import com.martingoe.cubelang.common.RegisterConfig.Companion.ARG_REGISTERS
import com.martingoe.cubelang.common.RegisterConfig.Companion.NORMAL_REGISTER
import com.martingoe.cubelang.common.ir.*

/**
 * Emits ASM values for given intermediate representation values
 */
class IRToASM {
    companion object {
        private var currentPushedCount: Int = 0
        private val NORMAL_REGISTER = listOf("ax", "bx", "dx", "di", "si", "cx")
        private val ARG_REGISTERS = listOf("di", "si", "dx", "cx", "8", "9")
        fun emitASMForIR(emitter: ASMEmitter) {
            var pushArgIndex = 0
            var popArgIndex = 0
            for (irValue in emitter.resultIRValues) {
                val arg0PointerValue = getPointerValue(irValue.arg0, irValue.resultType.getLength())
                val arg1PointerValue = getPointerValue(irValue.arg1, irValue.resultType.getLength())
                val asmPointerSize = getASMPointerSize(irValue.resultType.getLength())
                val res = when (irValue.type) {
                    IRType.COPY -> "mov ${arg0PointerValue}, $arg1PointerValue"
                    IRType.COPY_FROM_REF -> "lea ${arg1PointerValue}, $asmPointerSize $arg0PointerValue"
                    IRType.INC -> "inc $arg0PointerValue"
                    IRType.DEC -> "dec $arg0PointerValue"
                    IRType.SAL -> "sal ${arg0PointerValue}, ${irValue.arg1}"
                    IRType.NEG_UNARY -> "neg $arg0PointerValue"
                    IRType.COPY_FROM_DEREF -> "mov ${arg1PointerValue}, $asmPointerSize [${
                        getPointerValue(
                            irValue.arg0!!,
                            8
                        )
                    }]"
                    IRType.COPY_TO_DEREF -> "mov $asmPointerSize [${
                        getPointerValue(
                            irValue.arg0!!,
                            8
                        )
                    }], $arg1PointerValue"
                    IRType.CALL -> {
                        pushArgIndex = 0

                        var res = if (irValue.arg1 is TemporaryRegister && (irValue.arg1 as TemporaryRegister).allocatedIndex != 0) {
                            "push rax\n" +
                                    "call ${irValue.arg0}\n" +
                                    "mov ${arg1PointerValue}, ${
                                        getRegister(
                                            "ax",
                                            irValue.resultType.getLength()
                                        )
                                    }\n" +
                                    "pop rax"
                        } else {
                            "call ${irValue.arg0}\n"
                        }
                        // Align stack
                        res += if (currentPushedCount != 0) "add rsp, $currentPushedCount" else ""
                        currentPushedCount = 0
                        res
                    }
                    IRType.PLUS_OP -> {
                        "add ${arg0PointerValue}, $arg1PointerValue"
                    }
                    IRType.MINUS_OP -> {
                        "sub ${arg0PointerValue}, $arg1PointerValue"
                    }
                    IRType.MUL_OP -> {
                        "imul ${arg0PointerValue}, $arg1PointerValue"
                    }
                    IRType.DIV_OP -> {
                        val x = if ((irValue.arg0 as TemporaryRegister).allocatedIndex != 0) {
                            "push rax\n" +
                                    "mov ${getRegister("ax", irValue.resultType.getLength())}, ${arg0PointerValue}\n" +
                                    "idiv ${irValue.arg1}\n" +
                                    "mov ${arg0PointerValue}, ${
                                        getRegister(
                                            "ax",
                                            irValue.resultType.getLength()
                                        )
                                    }\n" +
                                    "pop rax"
                        } else {
                            "idiv ${
                                getPointerValue(
                                    irValue.arg1!!,
                                    irValue.resultType.getLength()
                                )
                            }"
                        }
                        "push rdx\nxor rdx, rdx\n" + if ((irValue.arg1 as TemporaryRegister).allocatedIndex == 2) {
                            "push rbx\n" +
                                    "mov ${getRegister("bx", irValue.resultType.getLength())}, ${arg0PointerValue}\n" +
                                    x +
                                    "\npop rbx"
                        } else {
                            x
                        } + "\npop rdx"
                    }
                    IRType.COPY_TO_FP_OFFSET -> {
                        "mov $asmPointerSize ${arg0PointerValue}, $arg1PointerValue"
                    }
                    IRType.COPY_FROM_FP_OFFSET -> {
                        "mov ${arg0PointerValue}, $asmPointerSize $arg1PointerValue"
                    }
                    IRType.POP_ARG -> {
                        if (popArgIndex < ARG_REGISTERS.size) {
                            if (irValue.resultType.getLength() <= 2) {
                                "mov ${getRegister("ax", 4)}, ${getRegister(ARG_REGISTERS[popArgIndex], 4)}\n" +
                                        "mov $asmPointerSize ${arg0PointerValue}, ${
                                    getRegister(
                                        "ax",
                                        irValue.resultType.getLength()
                                    )
                                }"
                            } else {
                                "mov $asmPointerSize ${arg0PointerValue}, ${
                                    getRegister(
                                        ARG_REGISTERS[popArgIndex++],
                                        irValue.resultType.getLength()
                                    )
                                }"
                            }
                        } else
                            ""
                    }
                    IRType.PUSH_ARG -> {
                        if (pushArgIndex < RegisterConfig.REGISTER_ARG_COUNT) {
                            if (irValue.resultType.getLength() <= 2) {
                                "movsx ${
                                    getRegister(
                                        ARG_REGISTERS[pushArgIndex++],
                                        4
                                    )
                                }, $arg0PointerValue"
                            } else {
                                "mov ${
                                    getRegister(
                                        ARG_REGISTERS[pushArgIndex++],
                                        irValue.resultType.getLength()
                                    )
                                }, $arg0PointerValue"
                            }
                        } else{
                            this.currentPushedCount += irValue.resultType.getLength()
                            "push $arg0PointerValue"

                        }
                    }
                    IRType.CMP -> {
                        "cmp ${arg0PointerValue}, $arg1PointerValue"
                    }

                    IRType.EXTEND_TO_64BITS -> {
                        if ((irValue.arg0 as TemporaryRegister).allocatedIndex != 0) {
                            "push rax\n" +
                                    "mov ${getRegister("ax", irValue.resultType.getLength())}, ${arg0PointerValue}\n" +
                                    "${extendTo64Bits(irValue.resultType.getLength())}\n" +
                                    "mov ${
                                        getPointerValue(
                                            irValue.arg0!!,
                                            8
                                        )
                                    }, ${getRegister("ax", 8)}\n" +
                                    "pop rax"
                        } else {
                            extendTo64Bits(irValue.resultType.getLength())
                        }
                    }

                    IRType.COPY_FROM_REG_OFFSET -> {
                        "mov ${arg1PointerValue}, $asmPointerSize $arg0PointerValue"
                    }

                    IRType.COPY_STRING_REF -> {
                        "lea ${arg0PointerValue}, [rel .str_$arg1PointerValue]"
                    }

                    else -> ""
                }
                emitter.emit(res)
            }
            emitter.resultIRValues.clear()
        }

        private fun extendTo64Bits(length: Int): String {
            val result = when (length) {
                1 -> "cbw\ncwde\ncdqe"
                2 -> "cwde\n" +
                        "cdqe"
                4 -> "cdqe"
                8 -> ""
                else -> error("Unknown byte size")
            }
            return result
        }

        private fun getASMPointerSize(length: Int): String? {
            return when (length) {
                1 -> "BYTE"
                2 -> "WORD"
                4 -> "DWORD"
                8 -> "QWORD"
                else -> null
            }
        }

        private fun getPointerValue(value: ValueType?, resultType: Int): String? {
            return when (value) {
                is TemporaryRegister -> getRegister(
                    NORMAL_REGISTER[value.allocatedIndex],
                    resultType
                )
                is IRLiteral -> value.value
                is FramePointer -> value.toString()
                is RegOffset -> "[${getPointerValue(value.temporaryRegister, 8)} - ${value.offset}]"
                is FramePointerOffset -> "[rbp - ${value.literal}" + if (value.temporaryRegister != null) " + ${
                    getPointerValue(
                        value.temporaryRegister,
                        8
                    )
                } * ${value.offset!!}]" else "]"
                else -> null
            }
        }

        /**
         * Returns the register for the appropriate name and length.
         */
        private fun getRegister(baseName: String, length: Int): String {
            return try {
                baseName.toInt()
                when (length) {
                    8 -> "r$baseName"
                    4 -> "r${baseName}d"
                    2 -> "r${baseName}w"
                    1 -> "r${baseName}b"
                    else -> error("")
                }
            } catch (e: NumberFormatException) {
                when (length) {
                    8 -> "r${baseName}"
                    4 -> "e${baseName}"
                    2 -> baseName
                    1 -> "${baseName[0]}l"
                    else -> error("")
                }
            }
        }

    }
}