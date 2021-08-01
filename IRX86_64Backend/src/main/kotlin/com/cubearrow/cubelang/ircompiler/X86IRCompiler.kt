package com.cubearrow.cubelang.ircompiler

import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.ircompiler.utils.getLength
import java.util.*

class X86IRCompiler(private val instructions: List<IRValue>, private val structs: MutableMap<String, Struct>) {
    var currentVarIndex = 0
    var currentArgumentIndex = 0
    var positiveArgIndex = 16

    init {
        for (struct in structs)
            lengthsOfTypes[struct.key] = struct.value.variables.fold(0) { acc, pair -> acc + pair.second.getLength() }
    }

    companion object {
        val TEMPORARY_REGISTERS = listOf("ax", "dx", "bx", "di", "si", "cx")
        val ARGUMENT_REGISTERS = listOf("di", "si", "dx", "cx", "8", "9")

        val lengthsOfTypes = mutableMapOf("i8" to 1, "i16" to 2, "i32" to 4, "i64" to 8, "char" to 1)

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
                else -> error("")
            }
            if (register.index != 0)
                result = "\npush rax\nmov ${getRegister("ax", length)},${getRegister(TEMPORARY_REGISTERS[register.index], length)}\n$result\nmov ${
                    getRegister(
                        TEMPORARY_REGISTERS[register.index], 8
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
    }

    class X86Variable(var index: Int, val type: Type)

    private val variables: Stack<MutableMap<String, X86Variable>> = Stack()
    fun compile(): String {
        variables.push(mutableMapOf())
        var result = "section .text\n    global main\n"
        for (instruction in instructions) {
            result += compileSingle(instruction)
        }
        return result
    }

    private fun compileSingle(instruction: IRValue): String {
        return when (instruction.type) {
            IRType.COPY -> compileCopy(instruction)
            IRType.COPY_FROM_REF -> copyFromRef(instruction)
            IRType.COPY_FROM_DEREF -> copyFromDeref(instruction)
            IRType.COPY_TO_DEREF -> TODO()
            IRType.PLUS_OP -> compilePlusOperation(instruction)
            IRType.MINUS_OP -> compileMinusOperation(instruction)
            IRType.MUL_OP -> compileMulOperation(instruction)
            IRType.DIV_OP -> compileDivOperation(instruction)
            IRType.COPY_FROM_ARRAY_ELEM -> compileCopyFromArrayElem(instruction)
            IRType.COPY_TO_ARRAY_ELEM -> compileCopyToArrayElem(instruction)
            IRType.PUSH_ARG -> compilePushArgument(instruction)
            IRType.CALL -> compileCall(instruction)
            IRType.FUNC_DEF -> compileFuncDef(instruction)
            IRType.POP_ARG -> compilePopArg(instruction)
            IRType.RET -> compileRet(instruction)
            IRType.NEG_UNARY -> compileNegUnary(instruction)
            IRType.JMP_EQ -> compileJumpCmp(instruction, "jeq")
            IRType.JMP_LE -> compileJumpCmp(instruction, "jle")
            IRType.JMP_L -> compileJumpCmp(instruction, "jl")
            IRType.JMP_GE -> compileJumpCmp(instruction, "jge")
            IRType.JMP_G -> compileJumpCmp(instruction, "jg")
            IRType.JMP_NE -> compileJmpNE(instruction)
            IRType.LABEL -> compileLabel(instruction)
            IRType.JMP -> compileJmp(instruction)
            IRType.VAR_DEF -> compileVarDefinition(instruction)
            IRType.COPY_TO_STRUCT_GET -> compileCopyToStructGet(instruction)
            IRType.COPY_FROM_STRUCT_GET -> compileCopyFromStructGet(instruction)
            IRType.INCLUDE -> "%include \"${instruction.arg0}\"\n"
            IRType.PUSH_REG -> if(instruction.arg0 is TemporaryRegister) "push ${getRegister(TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8)}\n" else TEMPORARY_REGISTERS.fold("") { acc, s -> acc +  "push ${getRegister(s, 8)}\n"}
            IRType.POP_REG -> if(instruction.arg0 is TemporaryRegister) "pop ${getRegister(TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8)}\n" else TEMPORARY_REGISTERS.fold("") { acc, s -> "pop ${getRegister(s, 8)}\n" + acc}
        }
    }

    private fun compileRet(instruction: IRValue): String {
        if(instruction.arg0 is TemporaryRegister) {
            val temporaryRegister = instruction.arg0 as TemporaryRegister
            return if (temporaryRegister.index == 0) "leave\nret\n"
            else "mov ${getRegister(TEMPORARY_REGISTERS[temporaryRegister.index], instruction.resultType.getLength())}, ${
                getRegister(
                    "ax",
                    instruction.resultType.getLength()
                )
            }\nleave\nret\n"
        } else if(instruction.arg0 is Variable){
            return "mov ${getRegister("ax", instruction.resultType.getLength())}, [${
                variableIndex(getVariables()[(instruction.arg0 as Variable).name]!!.index)
            }]\nleave\nret\n"
        }
        TODO()
    }

    private fun compileDivOperation(instruction: IRValue): String {
        var result = "div ${getPointerValue(instruction.arg1!!, instruction.resultType)}\n"
        if(instruction.result !is TemporaryRegister && (instruction.result!! as TemporaryRegister).index != 0)
            result += "mov ${getPointerValue(instruction.result!!, instruction.resultType)}, ${getRegister("ax", instruction.resultType.getLength())}"
        if(instruction.arg0!! !is TemporaryRegister && (instruction.arg0 as TemporaryRegister).index != 0){
            result = "push rax\n$result\npop rax"
        }
        return result
    }

    private fun compileNegUnary(instruction: IRValue): String {
        return "mov ${getPointerValue(instruction.result!!, instruction.resultType)}, ${getPointerValue(instruction.arg0!!, instruction.resultType)}\n"
    }

    private fun compileCopyFromStructGet(instruction: IRValue): String {
        if (instruction.arg0 is Variable) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            val structSubvalue = instruction.arg1 as StructSubvalue

            val struct = structs[structSubvalue.structType.typeName]!!
            val index = getStructIndex(struct, structSubvalue, variable.index)
            return "mov ${
                getPointerValue(
                    instruction.result!!,
                    instruction.resultType
                )
            }, ${getASMPointerLength(instruction.resultType.getLength())}[${variableIndex(index)}]\n"
        }
        TODO()
    }

    private fun getStructIndex(
        struct: Struct,
        structSubvalue: StructSubvalue,
        index: Int = 0
    ): Int {
        val requestedVar = struct.variables.first { pair -> pair.first == structSubvalue.name }
        val argumentsBefore = struct.variables.subList(0, struct.variables.indexOf(requestedVar))
        return index + argumentsBefore.fold(0) { acc, pair -> acc + pair.second.getLength() }
    }

    private fun compileCopyToStructGet(instruction: IRValue): String {
        if (instruction.arg0 is Variable) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            val structSubvalue = instruction.arg1 as StructSubvalue

            val struct = structs[structSubvalue.structType.typeName]!!
            val index = getStructIndex(struct, structSubvalue, variable.index)
            return "mov ${getASMPointerLength(instruction.resultType.getLength())}[${variableIndex(index)}], ${
                getPointerValue(
                    instruction.result!!,
                    instruction.resultType
                )
            }\n"
        }
        TODO()
    }

    private fun compileCopyToArrayElem(instruction: IRValue): String {
        if (instruction.arg0 is TemporaryRegister && instruction.arg1 is TemporaryRegister) {
            var result = extendTo64Bit(instruction.arg0 as TemporaryRegister, instruction.resultType.getLength())
            result += extendTo64Bit(instruction.arg1 as TemporaryRegister, instruction.resultType.getLength())
            return "$result\nmov ${
                getArrayElemPointer(
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8),
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8), instruction.resultType.getLength()
                )
            }, " +
                    "${getPointerValue(instruction.result!!, instruction.resultType)}\n"
        } else if (instruction.arg0 is Variable && instruction.arg1 is TemporaryRegister) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            val result = extendTo64Bit(instruction.arg1 as TemporaryRegister, instruction.resultType.getLength())
            return "$result\nmov ${
                getArrayElemPointer(
                    variableIndex(variable.index),
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8), instruction.resultType.getLength()
                )
            }, " +
                    "${getPointerValue(instruction.result!!, instruction.resultType)}\n"
        }
        TODO("Not yet implemented")
    }

    private fun compileCopyFromArrayElem(instruction: IRValue): String {
        if (instruction.arg0 is TemporaryRegister && instruction.arg1 is TemporaryRegister) {
            var result = extendTo64Bit(instruction.arg0 as TemporaryRegister, instruction.resultType.getLength())
            result += extendTo64Bit(instruction.arg1 as TemporaryRegister, instruction.resultType.getLength())
            return "$result\nmov ${
                getPointerValue(
                    instruction.result!!,
                    instruction.resultType
                )
            }, ${
                getArrayElemPointer(
                    getRegister(
                        TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8
                    ), getRegister(
                        TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8
                    ), instruction.resultType.getLength()
                )
            }\n"
        } else if (instruction.arg0 is Variable && instruction.arg1 is TemporaryRegister) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            val result = extendTo64Bit(instruction.arg1 as TemporaryRegister, instruction.resultType.getLength())
            return "$result\nmov ${
                getPointerValue(
                    instruction.result!!,
                    instruction.resultType
                )
            }, ${
                getArrayElemPointer(
                    variableIndex(variable.index),
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8),
                    instruction.resultType.getLength()
                )
            }\n"
        }
        TODO()
    }

    private fun getArrayElemPointer(
        string0: String,
        string1: String,
        length: Int
    ) = "${getASMPointerLength(length)} [$string0 + $string1 * $length]"

    private fun compilePushArgument(instruction: IRValue): String {
        if (ARGUMENT_REGISTERS.size > currentArgumentIndex) {
            if (instruction.resultType.getLength() <= 2) {
                return "movsx ${
                    getRegister(
                        ARGUMENT_REGISTERS[currentArgumentIndex],
                        4
                    )
                }, ${getPointerValue(instruction.arg0!!, instruction.resultType)}\n"
            }
            return "mov ${
                getRegister(
                    ARGUMENT_REGISTERS[currentArgumentIndex],
                    instruction.resultType.getLength()
                )
            }, ${getPointerValue(instruction.arg0!!, instruction.resultType)}\n"
        }
        return "push ${getPointerValue(instruction.arg0!!, instruction.resultType)}\n"
    }

    private fun compilePopArg(instruction: IRValue): String {
        if (ARGUMENT_REGISTERS.size > currentArgumentIndex) {
            if (instruction.resultType.getLength() <= 2) {
                return "mov eax, ${getRegister(ARGUMENT_REGISTERS[currentArgumentIndex], 4)}\n" +
                        "mov ${getPointerValue(instruction.result!!, instruction.resultType)}, ${
                            getRegister(
                                "ax",
                                instruction.resultType.getLength()
                            )
                        }\n"


            }
            return "mov ${getPointerValue(instruction.result!!, instruction.resultType)}, ${
                getRegister(
                    ARGUMENT_REGISTERS[currentArgumentIndex],
                    instruction.resultType.getLength()
                )
            }\n"
        }
        currentVarIndex += instruction.resultType.getLength()
        variables.last()[(instruction.result as Variable).name]!!.index = positiveArgIndex
        positiveArgIndex += 8
        return ""
    }

    private fun compileCall(instruction: IRValue): String {
        currentArgumentIndex = 0
        return "call ${instruction.arg0 as FunctionLabel}\n"
    }

    private fun compileJmpNE(instruction: IRValue): String {
        return compileJumpCmp(instruction, "jne")
    }

    private fun compileJumpCmp(instruction: IRValue, jumpOperation: String): String {
        return "cmp ${getPointerValue(instruction.arg0!!, instruction.resultType)}, ${
            getPointerValue(
                instruction.arg1!!,
                instruction.resultType
            )
        }\n" +
                "$jumpOperation ${instruction.result}\n"
    }

    private fun compileFuncDef(instruction: IRValue): String {
        currentVarIndex = 0
        currentArgumentIndex = 0
        positiveArgIndex = 16
        val instructionIndex = instructions.indexOf(instruction)
        var nextIndex = instructions.indexOfFirst { irValue -> irValue.type == IRType.FUNC_DEF && instructions.indexOf(irValue) > instructionIndex }
        if (nextIndex == -1)
            nextIndex = instructions.size - 1
        val subRsp = instructions.subList(instructionIndex, nextIndex).filter { irValue -> irValue.type == IRType.VAR_DEF }
            .fold(0) { acc, irValue -> acc + irValue.resultType.getLength() }
        return """${instruction.arg0 as FunctionLabel}:
            |push rbp
            |mov rbp, rsp
            |sub rsp, $subRsp
            |""".trimMargin()
    }

    private fun copyFromDeref(instruction: IRValue): String {
        if (instruction.arg0 is TemporaryRegister)
            return "mov ${
                getPointerValue(
                    instruction.result!!,
                    instruction.resultType
                )
            }, ${getASMPointerLength(instruction.resultType.getLength())} [${
                getRegister(
                    TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8
                )
            }]\n"
        TODO()
    }

    private fun copyFromRef(instruction: IRValue): String {
        val index = getVariables()[(instruction.arg0 as Variable).name]!!.index
        return "lea ${getPointerValue(instruction.result!!, instruction.resultType)}, [rbp${if (index < 0) index.toString() else "+$index"}]\n"
    }

    private fun compileLabel(instruction: IRValue): String = "${instruction.result}:\n"

    private fun compileJmp(instruction: IRValue): String {
        return when (instruction.arg0) {
            is TemporaryLabel, is FunctionLabel -> "jmp ${instruction.arg0}\n"
            else -> error("Could not jump to this type")
        }
    }

    private fun compileMinusOperation(instruction: IRValue): String {
        return compileOperationTwoArgs("sub", instruction)
    }

    private fun compileMulOperation(instruction: IRValue): String {
        // TODO: Unsigned
        return compileOperationTwoArgs("imul", instruction)
    }

    private fun compileVarDefinition(instruction: IRValue): String {
        currentVarIndex -= instruction.resultType.getLength()
        variables.last()[(instruction.result as Variable).name] = X86Variable(currentVarIndex, instruction.resultType)
        return ""
    }

    private fun compilePlusOperation(instruction: IRValue): String {
        return if (instruction.arg0 is TemporaryRegister) {
            compileOperationTwoArgs("add", instruction)
        } else TODO()
    }

    private fun compileOperationTwoArgs(op: String, instruction: IRValue) =
        "$op ${getPointerValue(instruction.arg0!!, instruction.resultType)}, ${getPointerValue(instruction.arg1!!, instruction.resultType)}\n" +
                compileCopy(IRValue(IRType.COPY, instruction.arg0, null, instruction.result, instruction.resultType))

    private fun compileCopy(instruction: IRValue): String {
        if (instruction.arg0 == instruction.result) return ""
        return "mov ${getPointerValue(instruction.result!!, instruction.resultType)}, ${
            getPointerValue(
                instruction.arg0!!,
                instruction.resultType
            )
        }\n"
    }

    private fun variableIndex(index: Int): String {
        return if (index < 0)
            "rbp${index}"
        else "rbp+$index"
    }

    private fun getPointerValue(result: ValueType, resultType: Type): String {
        return when (result) {
            is Variable -> {
                val variable = getVariables()[result.name] ?: throw VariableNotFoundException()
                "${getASMPointerLength(resultType.getLength())} [${variableIndex(variable.index)}]"
            }
            is TemporaryLabel, is FunctionLabel -> result.toString()

            is TemporaryRegister -> getRegister(TEMPORARY_REGISTERS[result.index], resultType.getLength())

            is Literal -> result.value
            else -> TODO()
        }
    }

    private fun getVariables(): MutableMap<String, X86Variable> {
        return variables.fold(mutableMapOf()) { acc, x -> acc.putAll(x); acc }
    }

    class VariableNotFoundException : RuntimeException()
}