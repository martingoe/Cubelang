package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class Compiler(expressions: List<Expression>, path: String) : Expression.ExpressionVisitor<String> {
    data class LocalVariable(var index: Int, var type: String, var length: Int)
    data class Function(var name: String, var args: Map<String, String>, var returnType: String?)

    private var stackIndex = Stack<Int>()
    private var lengthsOfTypes = mapOf("int" to 4, "char" to 1)
    private var currentReturnLength: Int? = null

    private var variables: Stack<MutableMap<String, LocalVariable>> = Stack()
    private var functions: MutableMap<String, Function> = HashMap()

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

    private fun getRegister(baseName: String, length: Int): String {
        when (length) {
            8 -> return "r${baseName}x"
            4 -> {
                return try {
                    baseName.toInt()
                    "r${baseName}d"
                } catch (e: NumberFormatException) {
                    "e${baseName}x"
                }
            }
            2 -> return try {
                baseName.toInt()
                "r${baseName}w"
            } catch (e: NumberFormatException) {
                "${baseName}h"
            }
            1 -> return try {
                baseName.toInt()
                "r${baseName}b"
            } catch (e: NumberFormatException) {
                "${baseName}l"
            }
            else -> return ""
        }
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
                    "mov ${getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${getRegister("a", variable.length)}"
        }
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization): String {
        if (varInitialization.expressionNull1 != null) {
            val value = varInitialization.expressionNull1?.accept(this)

            return if (varInitialization.expressionNull1 is Expression.Literal || varInitialization.expressionNull1 is Expression.VarCall) {
                val type = ExpressionUtils.getType(varInitialization.identifierNull1?.substring, (varInitialization.expressionNull1 as Expression.Literal).any1)
                val length = lengthsOfTypes[type] ?: TODO("Type not yet supported")
                stackIndex.push(stackIndex.pop() + length)
                variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), type, length)
                "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], $value"
            }else if(varInitialization.expressionNull1 is Expression.Call){
                initializeCallingFunction(varInitialization, value)
            } else {
                val length = 8
                stackIndex.push(stackIndex.pop() + length)
                variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), "any", length)
                "$value \n" +
                        "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], rax"
            }
        }

        variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), varInitialization.identifierNull1!!.substring, lengthsOfTypes[varInitialization.identifierNull1!!.substring] ?: error("The returned type could not be found"))
        return ""
    }

    private fun initializeCallingFunction(varInitialization: Expression.VarInitialization, value: String?): String {
        if (functions[varInitialization.identifier1.substring]?.returnType == null) {
            Main.error((varInitialization.expressionNull1 as Expression.Call).identifier1.line, (varInitialization.expressionNull1 as Expression.Call).identifier1.line, null, "The function does not return a value")
            return ""
        }

        val length = lengthsOfTypes[functions[varInitialization.identifier1.substring]!!.returnType]!!
        stackIndex.push(stackIndex.pop() + length)

        val type = ExpressionUtils.getType(functions[varInitialization.identifier1.substring]?.returnType, null)
        variables.peek()[varInitialization.identifier1.substring] = LocalVariable(stackIndex.peek(), type, length)
        return "$value \n" +
                "mov ${getASMPointerLength(length)} [rbp - ${stackIndex.peek()}], ${getRegister("a", length)}"
    }

    override fun visitOperation(operation: Expression.Operation): String {
        TODO("Not yet implemented")
    }

    override fun visitCall(call: Expression.Call): String {
        if (functions[call.identifier1.substring] != null)
            return "call ${call.identifier1.substring}"
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
        TODO("Not yet implemented")
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
        functions[functionDefinition.identifier1.substring] = Function(functionDefinition.identifier1.substring, mapOf("i" to "e"), functionDefinition.identifierNull1?.substring) //TODO
        stackIndex.push(0)
        val basic = """${functionDefinition.identifier1.substring}:
            |push rbp
            |mov rbp, rsp
            |sub rsp, """.trimMargin()

        var statements = ""
        currentReturnLength = lengthsOfTypes[functionDefinition.identifierNull1?.substring]
        functionDefinition.expressionLst2.forEach {
            statements += it.accept(this) + "\n"
        }
        currentReturnLength = null
        return basic + """${stackIndex.pop()}
            |$statements
            |leave
            |ret
        """.trimMargin()

    }

    override fun visitComparison(comparison: Expression.Comparison): String {
        TODO("Not yet implemented")
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt): String {
        TODO("Not yet implemented")
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): String {
        if (currentReturnLength == null) {
            Main.error(-1, 1, null, "You cannot return from the current function or are not in one.")
            return ""
        }

        if (returnStmnt.expression1 is Expression.Literal || returnStmnt.expression1 is Expression.VarCall) {
            return "mov ${getRegister("a", currentReturnLength!!)}, ${returnStmnt.expression1.accept(this)}"
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
}