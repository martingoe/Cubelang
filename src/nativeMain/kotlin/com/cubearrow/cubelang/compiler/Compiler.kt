package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.compiler.specificcompilers.*
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.IOUtils.Companion.writeAllLines
import com.cubearrow.cubelang.utils.NormalType
import com.cubearrow.cubelang.utils.Type

class Compiler(expressions: List<Expression>, path: String) : Expression.ExpressionVisitor<String> {
    companion object {
        val ARGUMENT_INDEXES = mapOf(0 to "di", 1 to "si", 2 to "dx", 3 to "cx", 4 to "8", 5 to "9")
        val OPERATION_REGISTERS = mapOf(0 to "bx", 1 to "12", 2 to "13", 3 to "14")
        val LENGTHS_OF_TYPES = mapOf("int" to 4, "char" to 1)
    }

    private var context = CompilerContext(this)

    data class LocalVariable(var index: Int, var type: Type, var length: Int)
    data class Function(var name: String, var args: Map<String, Type>, var returnType: Type?)

    init {
        context.functions["printChar"] = Function("printChar", mapOf("value" to NormalType("char")), null)
        context.functions["putchar"] = Function("putchar", mapOf("value" to NormalType("char")), null)
        context.functions["getCurrentTime"] = Function("getCurrentTime", mapOf(), NormalType("int"))
        context.functions["printInt"] = Function("printInt", mapOf("value" to NormalType("int")), null)

        context.variables.add(HashMap())
        context.stackIndex.add(0)

        var statements = ""
        var functions = ""
        expressions.filterIsInstance<Expression.FunctionDefinition>().forEach { functions += it.accept(this) + "\n" }
        expressions.filter { it !is Expression.FunctionDefinition }.forEach { statements += it.accept(this) + "\n" }

        val result = """${getBasicStructure()}
main:
mov rbp, rsp
sub rsp, ${context.stackIndex.removeLast()}

$statements


$functions"""
        writeAllLines(path, result)
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
getCurrentTime:
    mov rax, 201
    xor rdi, rdi        
    syscall
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

    override fun visitArrayGet(arrayGet: Expression.ArrayGet): String {
        return ArrayGetCompiler(context).accept(arrayGet)
    }

    override fun visitArraySet(arraySet: Expression.ArraySet): String {
        return ArraySetCompiler(context).accept(arraySet)
    }
}