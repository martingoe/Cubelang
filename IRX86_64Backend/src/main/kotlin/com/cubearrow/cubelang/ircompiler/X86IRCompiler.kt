package com.cubearrow.cubelang.ircompiler

import com.cubearrow.cubelang.common.ArrayType
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.ircompiler.utils.extendTo64Bit
import com.cubearrow.cubelang.ircompiler.utils.getASMPointerLength
import com.cubearrow.cubelang.ircompiler.utils.getLength
import com.cubearrow.cubelang.ircompiler.utils.getRegister
import java.util.*

class X86IRCompiler(private val instructions: List<IRValue>, private val structs: MutableMap<String, Struct>) {
    private var currentVarIndex = 0
    private var currentPushArgumentIndex = 0
    private var currentPopArgumentIndex = 0
    private var positiveArgIndex = 16

    init {
        for (struct in structs)
            lengthsOfTypes[struct.key] = struct.value.variables.fold(0) { acc, pair -> acc + pair.second.getLength() }
    }

    companion object {
        val TEMPORARY_REGISTERS = listOf("ax", "dx", "bx", "di", "si", "cx")
        val ARGUMENT_REGISTERS = listOf("di", "si", "dx", "cx", "8", "9")

        val lengthsOfTypes = mutableMapOf("I8" to 1, "I16" to 2, "I32" to 4, "I64" to 8, "CHAR" to 1)
    }

    class X86Variable(var index: Int)

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
            IRType.JMP_EQ -> compileJumpCmp(instruction, "je")
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
            IRType.PUSH_REG -> pushOrPopInstruction(instruction, "push")
            IRType.POP_REG -> pushOrPopInstruction(instruction, "pop")
            IRType.COPY_FROM_REG_OFFSET -> compileCopyFromRegOffset(instruction)
            IRType.CMP -> compileCmp(instruction)
        }
    }

    private fun pushOrPopInstruction(instruction: IRValue, instructionString: String) =
        if (instruction.arg0 is TemporaryRegister) "$instructionString ${
            getRegister(
                TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index],
                8
            )
        }\n" else TEMPORARY_REGISTERS.fold("") { acc, s -> acc + "$instructionString ${getRegister(s, 8)}\n" }

    /**
     * Compiles a single compare instruction.
     * @param instruction The instruction to compile
     * @return Returns a string with the following format: 'cmp leftArg, rightArg\n'
     */
    private fun compileCmp(instruction: IRValue): String {
        return "cmp ${getPointerValue(instruction.arg0!!, instruction.resultType.getLength())}, ${
            getPointerValue(
                instruction.arg1!!,
                instruction.resultType.getLength()
            )
        }\n"
    }

    /**
     * Moves a value from a register pointer with an offset.
     * @param instruction The instruction to compile.
     * @return Returns a string with the following format: 'mov resultArg, [leftArgReg + rightArg]'
     */
    private fun compileCopyFromRegOffset(instruction: IRValue): String {
        return "mov ${
            getPointerValue(
                instruction.result!!,
                instruction.resultType.getLength()
            )
        }, ${getASMPointerLength(instruction.resultType.getLength())} [${
            getRegister(
                TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8
            )
        } + ${instruction.arg1}]\n"
    }

    /**
     * Compiles a single RET instruction by moving to ax and calling 'leave\nret\n'
     * @param instruction The instruction to compile.
     *
     * @return Returns a string with the following format: '
     */
    private fun compileRet(instruction: IRValue): String {
        if (instruction.arg0 != null) {
            if (instruction.arg0 is TemporaryRegister && (instruction.arg0 as TemporaryRegister).index == 0)
                return "leave\nret\n"
            return "mov ${getRegister("ax", instruction.resultType.getLength())}, ${
                getPointerValue(
                    instruction.arg0!!,
                    instruction.resultType.getLength()
                )
            }\nleave\nret\n"
        }
        return "leave\nret\n"
    }

    /**
     * Compiles a DIV instruction while preserving the ax register.
     *
     */
    private fun compileDivOperation(instruction: IRValue): String {
        var result = "div ${getPointerValue(instruction.arg1!!, instruction.resultType.getLength())}\n"
        if (instruction.result !is TemporaryRegister && (instruction.result!! as TemporaryRegister).index != 0)
            result += "mov ${getRegister("ax", instruction.resultType.getLength())}, ${
                getPointerValue(
                    instruction.result!!,
                    instruction.resultType.getLength()
                )
            }\n"
        if (instruction.arg0!! !is TemporaryRegister && (instruction.arg0 as TemporaryRegister).index != 0) {
            result = "push rax\n$result\npop rax"
        }
        return result
    }

    /**
     * Moves the value to the requested register and negates it.
     *
     * @return Returs a string with the following format: 'mov arg0, result\nneg result\n'
     */
    private fun compileNegUnary(instruction: IRValue): String {
        return "mov ${getPointerValue(instruction.result!!, instruction.resultType.getLength())}, ${
            getPointerValue(
                instruction.arg0!!,
                instruction.resultType.getLength()
            )
        }\nneg ${getPointerValue(instruction.result!!, instruction.resultType.getLength())}"
    }

    /**
     * Compiles a single COPY_FROM_STRUCT_GET instruction by calculating the index of the value.
     * This currently only supports structs stored as variables on the stack.
     *
     * @param instruction The instruction to compile.
     * @return Returns a string with the following format: 'mov result, [rbp +/- index]
     */
    private fun compileCopyFromStructGet(instruction: IRValue): String {
        if (instruction.arg0 is Variable) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            val structSubvalue = instruction.arg1 as StructSubvalue

            val struct = structs[structSubvalue.structType.typeName]!!
            val index = getStructIndex(struct, structSubvalue, variable.index)
            return "mov ${
                getPointerValue(
                    instruction.result!!,
                    instruction.resultType.getLength()
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
                    instruction.resultType.getLength()
                )
            }\n"
        }
        TODO()
    }

    private fun compileCopyToArrayElem(instruction: IRValue): String {
        if (instruction.arg0 is TemporaryRegister && instruction.arg1 is TemporaryRegister) {
            val result = extendTo64Bit(instruction.arg1 as TemporaryRegister, instruction.resultType.getLength())
            return "$result\nmov ${
                getArrayElemPointer(
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8),
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8), instruction.resultType.getLength()
                )
            }, " +
                    "${getPointerValue(instruction.result!!, instruction.resultType.getLength())}\n"
        } else if (instruction.arg0 is Variable && instruction.arg1 is TemporaryRegister) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            val result = extendTo64Bit(instruction.arg1 as TemporaryRegister, instruction.resultType.getLength())
            return "$result\nmov ${
                getArrayElemPointer(
                    variableIndex(variable.index),
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8), instruction.resultType.getLength()
                )
            }, " +
                    "${getPointerValue(instruction.result!!, instruction.resultType.getLength())}\n"
        }
        TODO("Not yet implemented")
    }

    private fun compileCopyFromArrayElem(instruction: IRValue): String {
        val len = if (instruction.resultType is ArrayType) 8 else instruction.resultType.getLength()
        val instructionStr = if (instruction.resultType is PointerType) "lea" else "mov"
        if (instruction.arg0 is TemporaryRegister && instruction.arg1 is TemporaryRegister) {
            val result = extendTo64Bit(instruction.arg1 as TemporaryRegister, len)
            return "$result\n$instructionStr ${
                getPointerValue(
                    instruction.result!!,
                    len
                )
            }, ${
                getArrayElemPointer(
                    getRegister(
                        TEMPORARY_REGISTERS[(instruction.arg0 as TemporaryRegister).index], 8
                    ), getRegister(
                        TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8
                    ), len
                )
            }\n"
        } else if (instruction.arg0 is Variable && instruction.arg1 is TemporaryRegister) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            val result = extendTo64Bit(instruction.arg1 as TemporaryRegister, len)
            return "$result\n$instructionStr ${
                getPointerValue(
                    instruction.result!!,
                    len
                )
            }, ${
                getArrayElemPointer(
                    variableIndex(variable.index),
                    getRegister(TEMPORARY_REGISTERS[(instruction.arg1 as TemporaryRegister).index], 8),
                    len
                )
            }\n"
        } else if (instruction.arg0 is Variable && instruction.arg1 is Literal) {
            val variable = getVariables()[(instruction.arg0 as Variable).name]!!
            return "$instructionStr ${
                getPointerValue(
                    instruction.result!!,
                    len
                )
            }, ${
                getArrayElemPointer(
                    variableIndex(variable.index), (instruction.arg1 as Literal).value, len
                )
            }\n"
        }
        TODO()
    }

    private fun getArrayElemPointer(
        string0: String,
        string1: String,
        asmPointerLength: Int
    ) = "${getASMPointerLength(asmPointerLength)} [$string0 + $string1]"

    private fun compilePushArgument(instruction: IRValue): String {
        if (ARGUMENT_REGISTERS.size > currentPushArgumentIndex) {
            if (instruction.resultType.getLength() <= 2) {
                return "movsx ${
                    getRegister(
                        ARGUMENT_REGISTERS[currentPushArgumentIndex++],
                        4
                    )
                }, ${getPointerValue(instruction.arg0!!, instruction.resultType.getLength())}\n"
            }
            return "mov ${
                getRegister(
                    ARGUMENT_REGISTERS[currentPushArgumentIndex++],
                    instruction.resultType.getLength()
                )
            }, ${getPointerValue(instruction.arg0!!, instruction.resultType.getLength())}\n"
        }
        return "push ${getPointerValue(instruction.arg0!!, instruction.resultType.getLength())}\n"
    }

    private fun compilePopArg(instruction: IRValue): String {
        if (ARGUMENT_REGISTERS.size > currentPopArgumentIndex) {
            if (instruction.resultType.getLength() <= 2) {
                return "mov eax, ${getRegister(ARGUMENT_REGISTERS[currentPopArgumentIndex++], 4)}\n" +
                        "mov ${getPointerValue(instruction.result!!, instruction.resultType.getLength())}, ${
                            getRegister(
                                "ax",
                                instruction.resultType.getLength()
                            )
                        }\n"


            }
            return "mov ${getPointerValue(instruction.result!!, instruction.resultType.getLength())}, ${
                getRegister(
                    ARGUMENT_REGISTERS[currentPopArgumentIndex++],
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
        currentPushArgumentIndex = 0
        return "call ${instruction.arg0 as FunctionLabel}\n"
    }

    private fun compileJmpNE(instruction: IRValue): String {
        return compileJumpCmp(instruction, "jne")
    }

    private fun compileJumpCmp(instruction: IRValue, jumpOperation: String): String {
        return "$jumpOperation ${instruction.result}\n"
    }

    private fun compileFuncDef(instruction: IRValue): String {
        currentVarIndex = 0
        currentPopArgumentIndex = 0
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
                    instruction.resultType.getLength()
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
        return "lea ${
            getPointerValue(
                instruction.result!!,
                instruction.resultType.getLength()
            )
        }, [rbp${if (index < 0) index.toString() else "+$index"}]\n"
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
        variables.last()[(instruction.result as Variable).name] = X86Variable(currentVarIndex)
        return ""
    }

    private fun compilePlusOperation(instruction: IRValue): String {
        return if (instruction.arg0 is TemporaryRegister) {
            compileOperationTwoArgs("add", instruction)
        } else TODO()
    }

    private fun compileOperationTwoArgs(op: String, instruction: IRValue) =
        "$op ${getPointerValue(instruction.arg0!!, instruction.resultType.getLength())}, ${
            getPointerValue(
                instruction.arg1!!,
                instruction.resultType.getLength()
            )
        }\n" +
                compileCopy(IRValue(IRType.COPY, instruction.arg0, null, instruction.result, instruction.resultType))

    /**
     * Compiles a COPY instruction.
     * @return Returns a string with the following format: 'mov result, arg0\n'
     */
    private fun compileCopy(instruction: IRValue): String {
        if (instruction.arg0 == instruction.result) return ""
        return "mov ${getPointerValue(instruction.result!!, instruction.resultType.getLength())}, ${
            getPointerValue(
                instruction.arg0!!,
                instruction.resultType.getLength()
            )
        }\n"
    }

    /**
     * Returns the formatted index of a variable.
     * @param index The index of the variable
     *
     * @return Returns a string with the following format: 'rbp +/- index'
     */
    private fun variableIndex(index: Int): String {
        return if (index < 0)
            "rbp${index}"
        else "rbp+$index"
    }

    /**
     * Returns the string value of a given operand.
     * @param value The value to be formatted
     * @param valueType The type of the value. Needed for variables and registers.
     */
    private fun getPointerValue(value: ValueType, valueType: Int): String {
        return when (value) {
            is Variable -> {
                val variable = getVariables()[value.name]!!
                "${getASMPointerLength(valueType)} [${variableIndex(variable.index + value.extraOffset)}]"
            }
            is TemporaryLabel, is FunctionLabel -> value.toString()

            is TemporaryRegister -> getRegister(TEMPORARY_REGISTERS[value.index], valueType)

            is Literal -> value.value
            else -> TODO()
        }
    }

    /**
     * Returns all the currently declared variables.
     */
    private fun getVariables(): MutableMap<String, X86Variable> {
        return variables.fold(mutableMapOf()) { acc, x -> acc.putAll(x); acc }
    }
}