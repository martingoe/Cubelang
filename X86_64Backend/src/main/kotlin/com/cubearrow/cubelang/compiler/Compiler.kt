package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.compiler.specificcompilers.*
import com.cubearrow.cubelang.compiler.utils.IOUtils.Companion.writeAllLines
import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.common.errors.ErrorLibrary
import com.cubearrow.cubelang.common.definitions.Function

class Compiler(private val expressions: List<Expression>, private val definedFunctions: MutableList<Function>, private val path: String, lines: List<String>) : Expression.ExpressionVisitor<String> {
    companion object {
        val ARGUMENT_INDEXES = arrayOf("di", "si", "dx", "cx", "8", "9")
        val OPERATION_REGISTERS = arrayOf("bx", "12", "13", "14")
        val GENERAL_PURPOSE_REGISTERS = listOf("ax", "dx", "bx", "di", "si", "cx", "8")
        val lengthsOfTypes = mutableMapOf("i32" to 4, "i64" to 8, "i16" to 2, "char" to 1, "i8" to 1)
        val PRIMARY_TYPES = arrayOf("i64", "i32","i16", "i8", "char")

        const val LIBRARY_PATH = "library/"
    }

    var context = CompilerContext(this, errorLibrary = ErrorLibrary(lines, true))

    data class LocalVariable(var index: Int, var type: Type)
    data class Struct(var name: String, var vars: MutableList<Pair<String, Type>>)


    fun compile() {
        context.variables.add(HashMap())
        context.stackIndex.add(0)
        context.functions.addAll(definedFunctions)

        var importStatements = ""
        var functions = ""
        expressions.filter{it is Expression.ImportStmnt || it is Expression.StructDefinition}.forEach { importStatements += it.accept(this) + "\n" }
        expressions.filterIsInstance<Expression.FunctionDefinition>().forEach { functions += it.accept(this) + "\n" }
        repeat(expressions.filter { it !is Expression.FunctionDefinition && it !is Expression.StructDefinition && it !is Expression.ImportStmnt }.size) {
            context.error(
                -1,
                -1,
                "Expected a function definition at top level"
            )
        }

        val result = """${getBasicStructure(importStatements)}
$functions"""
        writeAllLines(path, result)
    }

    private fun getBasicStructure(importStatements: String): String {
        return """$importStatements
section .text
    global main
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

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): String {
        return InstanceGetCompiler(context).accept(instanceGet)
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet): String {
        return context.moveExpressionToX(instanceSet.value).moveTo(context.evaluate(instanceSet.instanceGet))
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition): String {
        return ArgumentDefinitionCompiler(context).accept(argumentDefinition)
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement): String {
        return BlockCompiler(context).accept(blockStatement)
    }

    override fun visitLogical(logical: Expression.Logical): String {
        return LogicalCompiler(context).accept(logical)
    }

    override fun visitUnary(unary: Expression.Unary): String {
        return UnaryCompiler(context).accept(unary)
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

    override fun visitImportStmnt(importStmnt: Expression.ImportStmnt): String {
        return ImportStmntCompiler(context).accept(importStmnt)
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet): String {
       return PointerGetCompiler(context).accept(pointerGet)
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer): String {
        return ValueFromPointerCompiler(context).accept(valueFromPointer)
    }

    override fun visitStructDefinition(structDefinition: Expression.StructDefinition): String {
        return StructDefinitionCompiler(context).accept(structDefinition)
    }
}