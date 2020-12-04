package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.specificcompilers.*
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils.Companion.getType
import com.cubearrow.cubelang.utils.UsualErrorMessages
import java.io.File

class Compiler(expressions: List<Expression>, path: String) : Expression.ExpressionVisitor<String> {
    companion object {
        val ARGUMENT_INDEXES = NonNullMap(mapOf(0 to "di", 1 to "si", 2 to "dx", 3 to "cx", 4 to "8", 5 to "9") as MutableMap<Int, String>)
        val OPERATION_REGISTERS = NonNullMap(mapOf(0 to "bx", 1 to "12", 2 to "13", 3 to "14") as MutableMap<Int, String>)
        var LENGTHS_OF_TYPES = NonNullMap(mapOf("int" to 4, "char" to 1) as MutableMap<String, Int>)
    }

    private var context = CompilerContext(this)

    data class LocalVariable(var index: Int, var type: String, var length: Int)
    data class Function(var name: String, var args: Map<String, String>, var returnType: String?)

    init {
        ARGUMENT_INDEXES.message = "The requested argument number could not be found"
        LENGTHS_OF_TYPES.message = "Type not yet supported"
        context.functions["printChar"] = Function("printChar", mapOf("value" to "char"), null)

        context.functions["printInt"] = Function("printInt", mapOf("value" to "int"), null)
        context.variables.push(HashMap())
        context.stackIndex.push(0)

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
sub rsp, ${context.stackIndex.pop()}

$statements


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
        return AssignmentCompiler(context).accept(assignment)
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization): String {
        return VarInitializationCompiler(context).accept(varInitialization)
    }

    override fun visitOperation(operation: Expression.Operation): String {
        return OperationCompiler(context).accept(operation)
    }

    override fun visitCall(call: Expression.Call): String {
        return CallCompiler(context).accept(call)
    }

    override fun visitLiteral(literal: Expression.Literal): String {
        return LiteralCompiler(context).accept(literal)
    }

    override fun visitVarCall(varCall: Expression.VarCall): String {
        return VarCallCompiler(context).accept(varCall)
    }


    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition): String {
        return FunctionDefinitionCompiler(context).accept(functionDefinition)
    }

    override fun visitComparison(comparison: Expression.Comparison): String {
        return ComparisonCompiler(context).accept(comparison)
    }


    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt): String {
        return IfStatementCompiler(context).accept(ifStmnt)
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): String {
        return ReturnCompiler(context).accept(returnStmnt)
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): String {
        return WhileCompiler(context).accept(whileStmnt)
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt): String {
        return ForLoopCompiler(context).accept(forStmnt)
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
        return ArgumentDefinitionCompiler(context).accept(argumentDefinition)
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement): String {
        return BlockCompiler(context).accept(blockStatement)
    }

    override fun visitLogical(logical: Expression.Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitUnary(unary: Expression.Unary): String {
        TODO("Not yet implemented")
    }

    override fun visitGrouping(grouping: Expression.Grouping): String {
        return GroupingCompiler(context).accept(grouping)
    }

    override fun visitEmpty(empty: Expression.Empty): String {
        return ""
    }
}