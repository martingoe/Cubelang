package com.cubearrow.cubelang.instructionselection

import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.ASMEmitter

class IRToASM {
    companion object{
        private val NORMAL_REGISTER = listOf("ax", "dx", "bx", "di", "si", "cx")
        private val ARG_REGISTERS = listOf("di", "si", "dx", "cx", "8", "9")
        fun emitASMForIR(emitter: ASMEmitter) {
            var pushArgIndex = 0
            var popArgIndex = 0
            for (irValue in emitter.resultIRValues) {
                val res = when (irValue.type) {
                    IRType.COPY -> "mov ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}"
                    IRType.COPY_FROM_REF -> "lea ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}, ${getASMPointerSize(irValue.resultType.getLength())} [rbp - ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}]"
                    IRType.INC -> "inc ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}"
                    IRType.DEC -> "dec ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}"
                    IRType.SAL -> "sal ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${irValue.arg1}"
                    IRType.NEG_UNARY -> "neg ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}"
                    IRType.COPY_FROM_DEREF -> "mov ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}, ${getASMPointerSize(irValue.resultType.getLength())} [${getPointerValue(irValue.arg0!!, 8)}]"
                    IRType.COPY_TO_DEREF -> "mov ${getASMPointerSize(irValue.resultType.getLength())} [${getPointerValue(irValue.arg0!!, 8)}], ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}"
                    IRType.CALL -> {
                        pushArgIndex = 0

                        if(irValue.arg1 is TemporaryRegister && (irValue.arg1 as TemporaryRegister).allocatedIndex != 0){
                            "push rax\n" +
                                    "call ${irValue.arg0}\n" +
                                    "mov ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}, ${getRegister("ax", irValue.resultType.getLength())}\n" +
                                    "pop rax"
                        } else{
                            "call ${irValue.arg0}\n"
                        }
                    }
                    IRType.PLUS_OP -> {
                        "add ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}"
                    }
                    IRType.MINUS_OP -> {
                        "sub ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}"
                    }
                    IRType.MUL_OP -> {
                        "imul ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}"
                    }
                    IRType.DIV_OP -> {
                        if((irValue.arg0 as TemporaryRegister).allocatedIndex != 0){
                            "push rax\n" +
                                    "mov ${getRegister("ax", irValue.resultType.getLength())}, ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}" +
                                    "idiv ${irValue.arg1}\n" +
                                    "mov ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${getRegister("ax", irValue.resultType.getLength())}\n" +
                                    "pop rax"
                        } else {
                            "idiv ${
                                getPointerValue(
                                    irValue.arg1!!,
                                    irValue.resultType.getLength()
                                )
                            }"
                        }
                    }
                    IRType.COPY_TO_FP_OFFSET -> {
                        "mov ${getASMPointerSize(irValue.resultType.getLength())} [rbp - ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}], ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}"
                    }
                    IRType.COPY_FROM_FP_OFFSET -> {
                        "mov ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${getASMPointerSize(irValue.resultType.getLength())} [rbp - ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}]"
                    }
                    IRType.POP_ARG -> {
                        if(popArgIndex < ARG_REGISTERS.size)
                            "mov ${getASMPointerSize(irValue.resultType.getLength())} [rbp - ${irValue.arg0}], ${getRegister(ARG_REGISTERS[popArgIndex++], irValue.resultType.getLength())}"
                        else
                            ""
                    }
                    IRType.PUSH_ARG -> {
                        if(pushArgIndex < ARG_REGISTERS.size)
                            "mov ${getRegister(ARG_REGISTERS[pushArgIndex++], irValue.resultType.getLength())}, ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}"
                        else
                            "push ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}"
                    }
                    IRType.CMP -> {
                        "cmp ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}, ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}"
                    }

                    IRType.EXTEND_TO_64BITS -> {
                        if((irValue.arg0 as TemporaryRegister).allocatedIndex != 0){
                            "push rax\n" +
                                    "mov ${getRegister("ax", irValue.resultType.getLength())}, ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}\n" +
                                    "${extendTo64Bits(irValue.resultType.getLength())}\n" +
                                    "mov ${getPointerValue(irValue.arg0!!, 8)}, ${getRegister("ax", 8)}\n" +
                                    "pop rax"
                        } else {
                            extendTo64Bits(irValue.resultType.getLength())
                        }
                    }

                    IRType.COPY_FROM_REG_OFFSET -> {
                        "mov ${getPointerValue(irValue.arg1!!, irValue.resultType.getLength())}, ${getASMPointerSize(irValue.resultType.getLength())} ${getPointerValue(irValue.arg0!!, irValue.resultType.getLength())}"
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

        private fun getASMPointerSize(length: Int): String {
            return when(length){
                1 -> "BYTE"
                2 -> "WORD"
                4 -> "DWORD"
                8 -> "QWORD"
                else -> error("")
            }
        }

        private fun getPointerValue(value: ValueType, resultType: Int): String{
            return when(value) {
                is TemporaryRegister -> getRegister(NORMAL_REGISTER[value.allocatedIndex], resultType)
                is Literal -> value.value
                is FramePointer -> value.toString()
                is RegOffset -> "[${getPointerValue(value.temporaryRegister, 8)} - ${value.offset}]"
                is FramePointerOffset -> "${value.literal} " + if(value.temporaryRegister != null) " + ${getPointerValue(value.temporaryRegister, 8)} * ${value.offset!!}" else ""
                else -> error("")
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
                    2 -> "${baseName[0]}h"
                    1 -> "${baseName[0]}l"
                    else -> error("")
                }
            }
        }

    }
}