package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.errors.ErrorManager
import java.util.*

class TypeChecker(
    private val expressions: List<Expression>,
    private val errorManager: ErrorManager,
    private val definedFunctions: Map<String, List<Function>>
) : Expression.ExpressionVisitor<Type> {
    private var defaultTypes = arrayOf("i64", "i32", "i16", "i8", "char")

    private var variables: Stack<MutableMap<String, Type>> = Stack()
    private val functions: MutableList<Function> = mutableListOf()
    private val structs: MutableMap<String, Struct> = mutableMapOf()

    init {
        variables.push(mutableMapOf())
    }
    fun checkTypes() {
        expressions.forEach { evaluate(it) }
    }

    override fun visitAssignment(assignment: Expression.Assignment): Type {
        val variable = getVariables()[assignment.name.substring]
        if (variable == null) {
            errorManager.error(assignment.name.line, assignment.name.index, "The required variable (${assignment.name.substring} could not be found.")
        }
        assertEqualTypes(
            evaluate(assignment.valueExpression),
            variable!!,
            "The variable type and the value type do not match.",
            assignment.name.line,
            assignment.name.index
        )
        return NoneType()
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization): Type {
        var type = varInitialization.type
        var actualType = varInitialization.valueExpression?.let { evaluate(it) }
        if (type !is NoneType && actualType != null) {
            if (varInitialization.valueExpression is Expression.Literal)
                actualType = type
            assertEqualTypes(
                type,
                actualType,
                "The implicit type ($actualType) does not match the explicit type ($type).",
                varInitialization.name.line,
                varInitialization.name.index
            )
        } else if (type is NoneType && actualType != null) {
            type = actualType
        } else if (type == NoneType() && actualType == null) {
            errorManager.error(
                varInitialization.name.line,
                varInitialization.name.index,
                "Variable initializations need either an explicit type or a value."
            )
        }
        variables.last()[varInitialization.name.substring] = type
        return NoneType()
    }

    override fun visitOperation(operation: Expression.Operation): Type {
        val leftType = evaluate(operation.leftExpression)
        val rightType = evaluate(operation.rightExpression)
        assertEqualTypes(
            leftType,
            rightType,
            "Cannot compute mathematical operations on mismatching types ($leftType and $rightType)",
            operation.operator.line,
            operation.operator.index
        )
        return leftType
    }

    private fun assertEqualTypes(type1: Type, type2: Type, errorMessage: String, line: Int = -1, index: Int = -1) {
        if (type1 != type2) {
            errorManager.error(line, index, errorMessage)
            TODO()
        }
    }

    private fun evaluate(expression: Expression): Type {
        return expression.accept(this)
    }

    override fun visitCall(call: Expression.Call): Type {
        if (call.callee is Expression.VarCall) {
            val function =
                functions.firstOrNull { it.name == (call.callee as Expression.VarCall).varName.substring && it.args.size == call.arguments.size }
            if (function == null) {
                errorManager.error(-1, -1, "Could not find the called function.")
                return NoneType()
            }
            var i = 0
            for (arg in function.args) {
                assertEqualTypes(evaluate(call.arguments[i]), arg.value, "Mismatched types when calling '${function.name}'.")
                i++
            }
            return function.returnType ?: NoneType()
        }
        TODO()
    }

    override fun visitLiteral(literal: Expression.Literal): Type {
        return when (literal.value) {
            is Int -> NormalType("i32")
            is Char -> NormalType("char")
            else -> error("Could not find the literal type.")
        }
    }

    override fun visitVarCall(varCall: Expression.VarCall): Type {
        val variable = getVariables()[varCall.varName.substring]
        if (variable == null) {
            errorManager.error(varCall.varName.line, varCall.varName.index, "Could not find the requested variable '${varCall.varName.substring}")
            return NoneType()
        }
        return variable
    }

    private fun getVariables(): MutableMap<String, Type> {
        return variables.fold(mutableMapOf()) { acc, x -> acc.putAll(x); acc }
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition): Type {
        functions.add(
            Function(
                functionDefinition.name.substring,
                functionDefinition.args.map { it as Expression.ArgumentDefinition }.associate { it.name.substring to it.type },
                functionDefinition.type
            )
        )
        variables.push(functions.last().args as MutableMap<String, Type>?)
        evaluate(functionDefinition.body)
        variables.pop()
        return NoneType()
    }

    override fun visitComparison(comparison: Expression.Comparison): Type {
        val leftType = evaluate(comparison.leftExpression)
        val rightType = evaluate(comparison.rightExpression)
        assertEqualTypes(
            leftType,
            rightType,
            "Cannot compute comparisons on mismatching types ($leftType and $rightType)",
            comparison.comparator.line,
            comparison.comparator.index
        )
        return NormalType("i8")
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt): Type {
        evaluate(ifStmnt.ifBody)
        evaluate(ifStmnt.condition)
        return NoneType()
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): Type {
        returnStmnt.returnValue?.let { evaluate(it) }
        return NoneType()
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): Type {
        evaluate(whileStmnt.condition)
        evaluate(whileStmnt.body)
        return NoneType()
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt): Type {
        forStmnt.inBrackets.forEach { evaluate(it) }
        evaluate(forStmnt.body)
        return NoneType()
    }

    override fun visitStructDefinition(structDefinition: Expression.StructDefinition): Type {
        val variables = structDefinition.body.map { it.name.substring to it.type }
        structs[structDefinition.name.substring] = Struct(structDefinition.name.substring, variables)
        return NoneType()
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Type {
        var type = evaluate(instanceGet.expression)
        if((type !is NormalType || defaultTypes.contains(type.typeName)) || type is PointerType && (type.subtype !is NormalType || defaultTypes.contains(
                (type.subtype as NormalType).typeName))){
            errorManager.error(instanceGet.identifier.line, instanceGet.identifier.index, "The requested value is not a struct.")
            return NoneType()
        }
        if(type is PointerType) type = type.subtype
        val structType = structs[(type as NormalType).typeName]!!.variables.firstOrNull { it.first == instanceGet.identifier.substring }
        if(structType == null) {
            errorManager.error(instanceGet.identifier.line, instanceGet.identifier.index, "The struct does not have the requested value.")
            return NoneType()
        }
        return structType.second
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet): Type {
        val expectedType = evaluate(instanceSet.instanceGet)
        val actual = evaluate(instanceSet.value)
        assertEqualTypes(expectedType, actual, "The expected type does not match the actual value.", instanceSet.instanceGet.identifier.line, instanceSet.instanceGet.identifier.index)
        return NoneType()
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition): Type {
        return argumentDefinition.type
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement): Type {
        blockStatement.statements.forEach { evaluate(it) }
        return NoneType()
    }

    override fun visitLogical(logical: Expression.Logical): Type {
        val leftType = evaluate(logical.leftExpression)
        val rightType = evaluate(logical.rightExpression)
        assertEqualTypes(
            leftType,
            rightType,
            "Cannot compute logical expressions on mismatching types ($leftType and $rightType)",
            logical.logical.line,
            logical.logical.index
        )
        return NormalType("i8")
    }

    override fun visitUnary(unary: Expression.Unary): Type {
        return evaluate(unary.expression)
    }

    override fun visitGrouping(grouping: Expression.Grouping): Type {
        return evaluate(grouping.expression)
    }

    override fun visitArrayGet(arrayGet: Expression.ArrayGet): Type {
        val type = evaluate(arrayGet.expression)
        if (type !is ArrayType || type is PointerType && type.subType !is ArrayType) {
            errorManager.error(-1, -1, "The requested value is not an array.")
        }
        if (type is ArrayType)
            return type.subType
        else if (type is PointerType)
            return (type.subtype as ArrayType).subType
        return NoneType()
    }

    override fun visitArraySet(arraySet: Expression.ArraySet): Type {
        val expectedType = evaluate(arraySet.arrayGet)
        val actualType = evaluate(arraySet.value)
        assertEqualTypes(expectedType, actualType, "Could not assign the type '$actualType' to the type '$expectedType'.")
        return NoneType()
    }

    override fun visitImportStmnt(importStmnt: Expression.ImportStmnt): Type {
        functions.addAll(definedFunctions[importStmnt.identifier.substring]!!)
        return NoneType()
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet): Type {
        val variable = getVariables()[pointerGet.varCall.varName.substring]
        if (variable == null) {
            errorManager.error(pointerGet.varCall.varName.line, pointerGet.varCall.varName.index, "The requested variable could not be found.")
            return NoneType()
        }
        return PointerType(variable)
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer): Type {
        val type = evaluate(valueFromPointer.expression)
        if (type !is PointerType) {
            errorManager.error(-1, -1, "Cannot read from a type which is not a pointer ($type).")
            return NoneType()
        }
        return type.subtype
    }

    override fun visitEmpty(empty: Expression.Empty): Type {
        return NoneType()
    }
}