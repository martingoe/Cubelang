package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

class Compiler(expressions: List<Expression>, path: String) : Expression.ExpressionVisitor<String> {
    companion object {
        val ARGUMENT_INDEXES = mapOf(0 to "di", 1 to "si", 2 to "dx", 3 to "cx", 4 to "8", 5 to "9")
    }

    data class LocalVariable(var index: Int, var type: String, var length: Int)
    data class Function(var name: String, var args: Map<String, String>, var returnType: String?)

    private var stackIndex = Stack<Int>()
    private var lengthsOfTypes = mapOf("int" to 4, "char" to 1)
    private var currentReturnLength: Int? = null
    private var lIndex = 2
    private var inIfCondition = false
    private var separateReturnSegment = false
    private var argumentIndex = 0
    private var operationResultSize = 0

    private var variables: Stack<MutableMap<String, LocalVariable>> = Stack()
    private var functions: MutableMap<String, Function> = HashMap()

    init {
        functions["printChar"] = Function("printChar", mapOf("value" to "char"), null)

        functions["printInt"] = Function("printInt", mapOf("value" to "int"), null)
        variables.push(HashMap())
        stackIndex.push(0)
        var statements = ""
        var functions = ""
        expressions.forEach {
            if (it is Expression.FunctionDefinition) {
                functions += it.accept(this) + "\n"
            } else {
                statements += it.accept(this) + "\n"
            }
        }

        val result = """${getBasicStructure()}
main:
mov rbp, rsp
sub rsp, ${stackIndex.pop()}

$statements

mov rax, 60
mov rdi, 0
syscall

$functions"""
        val file = File(path)
        file.createNewFile()
        file.writeText(result)
    }

    private fun getBasicStructure(): String {
        return """
extern printf
extern putchar
section .data
    intPrintFormat db "%d", 10, 0
section .text
    global main
printInt:
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret
    
printChar:
    call putchar
    mov rdi, 10
    call putchar 
    ret
 """.trimIndent()
    }


    override fun visitAssignment(assignment: Expression.Assignment): String {
        val variable = variables.peek()[assignment.identifier1.substring]
        if (variable == null) {
            Main.error(assignment.identifier1.line, assignment.identifier1.index, null, "The variable \"${assignment.identifier1.substring}\" is not defined")
            //Unreachable
            return ""
        }

        return when {
            assignment.expression1 is Expression.Literal && (assignment.expression1 as Expression.Literal).any1 is Int -> {
                "mov ${getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${assignment.expression1.accept(this)}"
            }
            assignment.expression1 is Expression.VarCall -> {
                assignToVariable(variable, variables.peek()[(assignment.expression1 as Expression.VarCall).identifier1.substring]
                        ?: error("Variable not found"))
            }
            else -> {
                "${assignment.expression1.accept(this)} \n" +
                        "mov ${getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${CompilerUtils.getRegister("ax", variable.length)}"
            }
        }
    }

    private fun assignToVariable(variableToAssignTo: LocalVariable, variableToAssign: LocalVariable): String {
        val length = lengthsOfTypes[variableToAssignTo.type] ?: error("Type not yet supported")
        val register = CompilerUtils.getRegister("ax", length)
        return """
                |mov $register, ${getASMPointerLength(length)} [rbp - ${variableToAssign.index}]
                |mov ${getASMPointerLength(length)} [rbp - ${variableToAssignTo.index}], $register
            """.trimMargin()
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization): String {
        if (varInitialization.expressionNull1 != null) {
            val value = varInitialization.expressionNull1?.accept(this)

            return when (varInitialization.expressionNull1) {
                is Expression.Literal -> {
                    val type = ExpressionUtils.getType(varInitialization.identifierNull1?.substring, (varInitialization.expressionNull1 as Expression.Literal).any1)
                    val length = lengthsOfTypes[type] ?: error("Type not yet supported")
                    initializeVariable(length, varInitialization, LocalVariable(stackIndex.peek() + length, type, length))
                    "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], $value"
                }
                is Expression.VarCall -> {
                    val varCall = varInitialization.expressionNull1 as Expression.VarCall
                    val type = variables.peek()[varCall.identifier1.substring]!!.type
                    varInitialization.identifierNull1?.let { if (it.substring != type) Main.error(it.line, it.index, null, "Mismatched types") }
                    val length = lengthsOfTypes[type] ?: error("Type not yet supported")
                    val variable = LocalVariable(stackIndex.peek() + length, type, length)

                    initializeVariable(length, varInitialization, variable)
                    assignToVariable(variable, variables.peek()[varCall.identifier1.substring]
                            ?: error("Variable not found"))
                }
                is Expression.Call -> {
                    initializeVariableWithCall(varInitialization, value)
                }
                is Expression.Operation -> {
                    initializeVariable(operationResultSize, varInitialization, LocalVariable(stackIndex.peek() + operationResultSize, "any", operationResultSize))

                    "$value \n" +
                            "mov ${getASMPointerLength(operationResultSize)} [rbp - ${stackIndex.peek()}], ${CompilerUtils.getRegister("ax", operationResultSize)}"
                }
                else -> {
                    val length = 8
                    initializeVariable(length, varInitialization, LocalVariable(stackIndex.peek() + length, "any", length))

                    "$value \n" +
                            "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], rax"
                }
            }
        }

        variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), varInitialization.identifierNull1!!.substring, lengthsOfTypes[varInitialization.identifierNull1!!.substring]
                ?: error("The returned type could not be found"))
        return ""
    }

    private fun initializeVariable(length: Int, varInitialization: Expression.VarInitialization, variable: LocalVariable) {
        stackIndex.push(stackIndex.pop() + length)
        variables.peek()[varInitialization.identifier1.substring] = variable
    }

    private fun initializeVariableWithCall(varInitialization: Expression.VarInitialization, value: String?): String {
        val call = varInitialization.expressionNull1 as Expression.Call
        val function = functions[call.identifier1.substring] ?: error("The called function does not exist")
        if (function.returnType == null) {
            Main.error((varInitialization.expressionNull1 as Expression.Call).identifier1.line, (varInitialization.expressionNull1 as Expression.Call).identifier1.line, null, "The function does not return a value")
            return ""
        }
        varInitialization.identifierNull1?.let { if (it.substring != function.returnType) Main.error(it.line, it.index, null, "The types do not match") }

        val length = lengthsOfTypes[function.returnType]!!
        initializeVariable(length, varInitialization, LocalVariable(stackIndex.peek() + length, function.returnType!!, length))
        return "$value \n" +
                "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], ${CompilerUtils.getRegister("ax", length)}"
    }

    override fun visitOperation(operation: Expression.Operation): String {
        val rightPair = getOperationSide(operation.expression2)
        val rightSide = rightPair.first + "\nmov rbx, rax"
        val rightRegister = CompilerUtils.getRegister("ax", rightPair.second)

        val leftPair = getOperationSide(operation.expression1)
        val leftSide = leftPair.first
        val leftRegister = CompilerUtils.getRegister("bx", leftPair.second)
        operationResultSize = leftPair.second
        return "$rightSide\n$leftSide\n${CompilerUtils.getOperator(operation.operator1.substring)} $rightRegister, $leftRegister"
    }

    private fun getOperationSide(side: Expression): Pair<String, Int> {
        val registerSize: Int
        val leftSide = when (side) {
            is Expression.Literal -> {
                val value = side.accept(this)
                val length = lengthsOfTypes[ExpressionUtils.getType(null, side.any1)]
                        ?: error("Unsupported type")
                val register = CompilerUtils.getRegister("ax", length)
                registerSize = length
                "mov ${register}, $value"
            }
            is Expression.VarCall -> {
                val variable = variables.peek()[side.identifier1.substring]
                        ?: error("The variable could not be found")
                val register = CompilerUtils.getRegister("ax", variable.length)

                registerSize = variable.length
                "mov $register, ${side.accept(this)}"
            }
            is Expression.Call -> {
                val function = functions[side.identifier1.substring] ?: error("The called function does not exist")
                registerSize = lengthsOfTypes[function.returnType]!!
                side.accept(this)
            }
            else -> {
                registerSize = 8
                side.accept(this)
            }
        }
        return Pair(leftSide, registerSize)
    }

    override fun visitCall(call: Expression.Call): String {
        val function = functions[call.identifier1.substring]
        if (function != null) {
            argumentIndex = 0
            var args = ""
            for (i in 0 until call.expressionLst1.size) {
                val argumentExpression = call.expressionLst1[i]
                val expectedArgumentType = function.args[function.args.keys.elementAt(i)] ?: error("Unreachable")
                var argumentLength = lengthsOfTypes[expectedArgumentType] ?: error("The requested type has not yet been implemented")
                val axRegister = CompilerUtils.getRegister("ax", argumentLength)
                if (argumentLength >= 4 || argumentExpression is Expression.Literal) {
                    argumentLength = max(argumentLength, 4)
                    val baseString = "mov ${CompilerUtils.getRegister(ARGUMENT_INDEXES[argumentIndex++]!!, argumentLength)}, "
                    args += if (argumentExpression is Expression.Literal || argumentExpression is Expression.VarCall) {
                        "$baseString${argumentExpression.accept(this)} \n"
                    } else {
                        "${argumentExpression.accept(this)} \n" +
                                "$baseString$axRegister \n"
                    }
                } else {
                    args += if (argumentExpression is Expression.VarCall) {
                        "mov $axRegister, ${argumentExpression.accept(this)} \n" +
                                "movsx ${CompilerUtils.getRegister(ARGUMENT_INDEXES[argumentIndex++]!!, 8)}, $axRegister\n"
                    } else {
                        "${argumentExpression.accept(this)} \n" +
                                "movsx ${CompilerUtils.getRegister(ARGUMENT_INDEXES[argumentIndex++]!!, 8)}, $axRegister\n"
                    }

                }
            }
            return "${args}call ${call.identifier1.substring}"
        }
        Main.error(call.identifier1.line, call.identifier1.index, null, "The called function does not exist.")
        return ""
    }

    override fun visitLiteral(literal: Expression.Literal): String {
        if (literal.any1 is Int) {
            return literal.any1.toString()
        } else if (literal.any1 is Char) {
            return (literal.any1 as Char).toByte().toInt().toString()
        }
        TODO("Not yet implemented")
    }

    override fun visitVarCall(varCall: Expression.VarCall): String {
        val x: LocalVariable? = variables.peek()[varCall.identifier1.substring]
        if (x != null) {
            val lengthAsString = getASMPointerLength(x.length)
            return "$lengthAsString [rbp-${x.index}]"
        }
        Main.error(varCall.identifier1.line, varCall.identifier1.index, null,
                "The requested variable does not exist in the current scope.")
        return ""
    }

    private fun getASMPointerLength(length: Int): String {
        return when (length) {
            1 -> "BYTE"
            2 -> "WORD"
            4 -> "DWORD"
            8 -> "QWORD"
            else -> throw UnknownByteSizeException()
        }
    }

    class UnknownByteSizeException : Throwable()

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition): String {
        val args = ExpressionUtils.mapArgumentDefinitions(functionDefinition.expressionLst1)
        if (args.size > 5) Main.error(functionDefinition.identifier1.line, functionDefinition.identifier1.index, null, "The function must only have 5 arguments")
        functions[functionDefinition.identifier1.substring] = Function(functionDefinition.identifier1.substring, args, functionDefinition.identifierNull1?.substring)

        stackIndex.push(0)
        variables.push(HashMap())
        currentReturnLength = lengthsOfTypes[functionDefinition.identifierNull1?.substring]
        argumentIndex = 0
        var statements = ""
        functionDefinition.expressionLst1.forEach { statements += it.accept(this) + "\n" }
        functionDefinition.expressionLst2.forEach { statements += it.accept(this) + "\n" }
        variables.pop()
        currentReturnLength = null

        return """${functionDefinition.identifier1.substring}:
            |push rbp
            |mov rbp, rsp
            |sub rsp, ${stackIndex.pop()}
            |$statements
            |${if (separateReturnSegment) ".L${lIndex}:" else ""}
            |leave
            |ret
        """.trimMargin()
    }

    override fun visitComparison(comparison: Expression.Comparison): String {
        if (comparison.expression1 is Expression.VarCall && inIfCondition) {
            if (comparison.expression2 is Expression.Literal || comparison.expression2 is Expression.VarCall) {
                return "cmp ${comparison.expression1.accept(this)}, ${comparison.expression2.accept(this)}\n" +
                        when (comparison.comparator1.substring) {
                            "==" -> "jne"
                            "!=" -> "je"
                            "<" -> "jge"
                            "<=" -> "jg"
                            ">" -> "jle"
                            ">=" -> "jl"
                            else -> error("Comparison operator not expected")
                        } + " .L${lIndex}"
            }
        }
        TODO("Not yet implemented")
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt): String {
        this.inIfCondition = true
        val condition = ifStmnt.expression1.accept(this) + "\n"
        var first = ""
        ifStmnt.expressionLst1.forEach {
            var x = it.accept(this)
            if (it is Expression.ReturnStmnt) {
                x += "\njmp .L${lIndex + 1}"
                separateReturnSegment = true
            }
            first += x + "\n"
        }
        first += if (ifStmnt.expressionLst2.isNotEmpty() && !separateReturnSegment) "jmp .L${lIndex + 1}\n" else ""

        val after = ".L${lIndex++}:\n${ifStmnt.expressionLst2.joinToString { it.accept(this) }}"
        return condition + first + after + if (ifStmnt.expressionLst2.isNotEmpty() && !separateReturnSegment) "\n.L${lIndex}:" else ""
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): String {
        if (currentReturnLength == null) {
            Main.error(-1, 1, null, "You cannot return from the current function or are not in one.")
            return ""
        }

        if (returnStmnt.expression1 is Expression.Literal || returnStmnt.expression1 is Expression.VarCall) {
            return "mov ${CompilerUtils.getRegister("ax", currentReturnLength!!)}, ${returnStmnt.expression1.accept(this)}"
        }
        return ""
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): String {
        TODO("Not yet implemented")
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt): String {
        TODO("Not yet implemented")
    }

    override fun visitClassDefinition(classDefinition: Expression.ClassDefinition): String {
        TODO("Not yet implemented")
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): String {
        TODO("Not yet implemented")
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet): String {
        TODO("Not yet implemented")
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition): String {
        val length: Int = (lengthsOfTypes[argumentDefinition.identifier2.substring]
                ?: Main.error(argumentDefinition.identifier2.line, argumentDefinition.identifier2.index, null,
                        "The specified type cannot be found")) as Int

        stackIndex.push(stackIndex.pop() + length)
        variables.peek()[argumentDefinition.identifier1.substring] = LocalVariable(stackIndex.peek(), argumentDefinition.identifier2.substring, length)

        val register = ARGUMENT_INDEXES[argumentIndex++]!!
        return if (length > 2) {
            "mov ${getASMPointerLength(length)}[rbp - ${stackIndex.peek()}], ${CompilerUtils.getRegister(register, length)}"
        } else {
            "mov eax, ${CompilerUtils.getRegister(register, 4)}\n" +
                    "mov ${getASMPointerLength(length)}[rbp - ${stackIndex.peek()}], ${CompilerUtils.getRegister("ax", length)}"
        }
    }
}