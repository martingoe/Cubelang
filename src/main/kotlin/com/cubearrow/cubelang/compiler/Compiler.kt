package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import java.io.File
import java.util.*
import kotlin.collections.HashMap

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

    private var variables: Stack<MutableMap<String, LocalVariable>> = Stack()
    private var functions: MutableMap<String, Function> = HashMap()

    private fun evaluateList(list: List<Expression>): String {
        return list.joinToString { it.accept(this) + "\n" }
    }

    init {
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
_start:
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
        return """section .text
    global _start
 """.trimIndent()
    }


    override fun visitAssignment(assignment: Expression.Assignment): String {
        val variable = variables.peek()[assignment.identifier1.substring]
        if (variable == null) {
            Main.error(assignment.identifier1.line, assignment.identifier1.index, null, "The variable \"${assignment.identifier1.substring}\" is not defined")
            //Unreachable
            return ""
        }

        return if (assignment.expression1 is Expression.VarCall || (assignment.expression1 is Expression.Literal && (assignment.expression1 as Expression.Literal).any1 is Int)) {
            "mov ${getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${assignment.expression1.accept(this)}"
        } else {
            "${assignment.expression1.accept(this)} \n" +
                    "mov ${getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${CompilerUtils.getRegister("ax", variable.length)}"
        }
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization): String {
        if (varInitialization.expressionNull1 != null) {
            val value = varInitialization.expressionNull1?.accept(this)

            return if (varInitialization.expressionNull1 is Expression.Literal || varInitialization.expressionNull1 is Expression.VarCall) {
                val type: String
                if (varInitialization.expressionNull1 is Expression.Literal) {
                    type = ExpressionUtils.getType(varInitialization.identifierNull1?.substring, (varInitialization.expressionNull1 as Expression.Literal).any1)
                } else {
                    val varCall = varInitialization.expressionNull1 as Expression.VarCall
                    type = variables.peek()[varCall.identifier1.substring]!!.type
                    varInitialization.identifierNull1?.let { if (it.substring != type) Main.error(it.line, it.index, null, "Mismatched types") }
                }
                val length = lengthsOfTypes[type] ?: TODO("Type not yet supported")
                stackIndex.push(stackIndex.pop() + length)
                variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), type, length)
                "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], $value"
            } else if (varInitialization.expressionNull1 is Expression.Call) {
                initializeCallingFunction(varInitialization, value)
            } else {
                val length = 8
                stackIndex.push(stackIndex.pop() + length)
                variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), "any", length)
                "$value \n" +
                        "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], rax"
            }
        }

        variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), varInitialization.identifierNull1!!.substring, lengthsOfTypes[varInitialization.identifierNull1!!.substring]
                ?: error("The returned type could not be found"))
        return ""
    }

    private fun initializeCallingFunction(varInitialization: Expression.VarInitialization, value: String?): String {
        val call = varInitialization.expressionNull1 as Expression.Call
        if (functions[call.identifier1.substring]?.returnType == null) {
            Main.error((varInitialization.expressionNull1 as Expression.Call).identifier1.line, (varInitialization.expressionNull1 as Expression.Call).identifier1.line, null, "The function does not return a value")
            return ""
        }

        val length = lengthsOfTypes[functions[call.identifier1.substring]!!.returnType]!!
        stackIndex.push(stackIndex.pop() + length)

        val type = ExpressionUtils.getType(functions[varInitialization.identifier1.substring]?.returnType, null)
        variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), type, length)
        return "$value \n" +
                "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], ${CompilerUtils.getRegister("ax", length)}"
    }

    override fun visitOperation(operation: Expression.Operation): String {
        TODO("Not yet implemented")
    }

    override fun visitCall(call: Expression.Call): String {
        val function = functions[call.identifier1.substring]
        if (function != null) {
            argumentIndex = 0
            var args = ""
            for (i in 0 until call.expressionLst1.size) {
                val expression = call.expressionLst1[i]
                val argumentType = function.args[function.args.keys.elementAt(i)] ?: TODO()
                val argumentLength = lengthsOfTypes[argumentType] ?: TODO()
                val baseString = "mov ${ARGUMENT_INDEXES[argumentIndex++]?.let { CompilerUtils.getRegister(it, argumentLength) }}, "
                args += if (expression is Expression.Literal || expression is Expression.VarCall) {
                    "$baseString${expression.accept(this)} \n"
                } else {
                    "${expression.accept(this)} \n" +
                            "$baseString${CompilerUtils.getRegister("ax", argumentLength)} \n"
                }
            }
            return "${args}call ${call.identifier1.substring}"
        }
        Main.error(call.identifier1.line, call.identifier1.index, null, "The called function does not exist.")
        TODO("Not yet implemented")
    }

    override fun visitLiteral(literal: Expression.Literal): String {
        if (literal.any1 is Int) {
            return literal.any1.toString()
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
        functions[functionDefinition.identifier1.substring] = Function(functionDefinition.identifier1.substring, args, functionDefinition.identifierNull1?.substring)
        stackIndex.push(0)
        variables.push(HashMap())
        currentReturnLength = lengthsOfTypes[functionDefinition.identifierNull1?.substring]
        argumentIndex = 0
        val statements = evaluateList(functionDefinition.expressionLst1) + evaluateList(functionDefinition.expressionLst2)
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
        val first = ifStmnt.expressionLst1.joinToString {
            var x = it.accept(this)
            if (it is Expression.ReturnStmnt) {
                x += "\njmp .L${lIndex + 1}"
                separateReturnSegment = true
            }
            x + "\n"
        }
        val after = ".L${lIndex++}:\n${ifStmnt.expressionLst2.joinToString { it.accept(this) }}"
        return condition + first + after
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

        return "mov ${getASMPointerLength(length)}[rbp - ${stackIndex.peek()}], ${ARGUMENT_INDEXES[argumentIndex++]?.let { CompilerUtils.getRegister(it, length) }}"
    }
}