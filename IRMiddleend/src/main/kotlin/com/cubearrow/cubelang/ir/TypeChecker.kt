package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.errors.ErrorManager
import com.cubearrow.cubelang.common.SymbolTableSingleton
import java.util.*

class TypeChecker(
    private val expressions: List<Expression>,
    private val errorManager: ErrorManager
) : Expression.ExpressionVisitor<Type> {
    var currentVarIndex = 0
    var currentPosVarIndex = 16
    var scope = Stack<Int>()
    private var variables: Stack<MutableMap<String, Type>> = Stack()

    init {
        variables.push(mutableMapOf())
        scope.push(-1)
    }

    fun checkTypes() {
        expressions.forEach { evaluate(it) }
    }

    override fun visitAssignment(assignment: Expression.Assignment): Type {
        var variable: VarNode? = null
        try {
            variable = getVariables().first { it.name == assignment.name.substring }
        } catch (e: NoSuchElementException) {
            errorManager.error(assignment.name.line, assignment.name.index, "The required variable (${assignment.name.substring} could not be found.")
        }
        variable?.type?.let {
            assertEqualTypes(
                evaluate(assignment.valueExpression),
                it,
                "The variable type and the value type do not match.",
                assignment.name.line,
                assignment.name.index
            )
        }
        return NoneType()
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization): Type {
        var type = varInitialization.type
        val actualType = varInitialization.valueExpression?.let { evaluate(it) }
        if (type !is NoneType && actualType != null) {
            assertEqualTypes(
                type,
                actualType,
                "The implicit type ($actualType) does not match the explicit type ($type).",
                varInitialization.name.line,
                varInitialization.name.index
            )
        } else if (type is NoneType && actualType != null) {
            if (actualType is NormalType && actualType.type == NormalTypes.ANY_INT) {
                varInitialization.type = NormalType(NormalTypes.I32)
                type = NormalType(NormalTypes.I32)
            } else {
                varInitialization.type = actualType
                type = actualType
            }
        } else if (type == NoneType()) {
            errorManager.error(
                varInitialization.name.line,
                varInitialization.name.index,
                "Variable initializations need either an explicit type or a value."
            )
        }
        currentVarIndex += type.getLength()
        SymbolTableSingleton.getCurrentSymbolTable().defineVariable(scope, varInitialization.name.substring, type, currentVarIndex)
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
        operation.resultType = leftType
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
        val function =
            SymbolTableSingleton.getCurrentSymbolTable().functions.firstOrNull { it.name == call.callee.varName.substring && it.args.size == call.arguments.size }
        if (function == null) {
            errorManager.error(-1, -1, "Could not find the called function.")
            return NoneType()
        }
        var i = 0
        for (arg in function.args) {
            assertEqualTypes(evaluate(call.arguments[i]), arg.value, "Mismatched types when calling '${function.name}'.")
            i++
        }
        call.resultType = function.returnType ?: NoneType()
        return function.returnType ?: NoneType()
    }

    override fun visitLiteral(literal: Expression.Literal): Type {
        val type = when (literal.value) {
            is Int -> NormalType(NormalTypes.ANY_INT)
            is Char -> NormalType(NormalTypes.CHAR)
            else -> error("Could not find the literal type.")
        }
        literal.resultType = type
        return type
    }

    override fun visitVarCall(varCall: Expression.VarCall): Type {
        val type = try {
            getVariables().first { it.name == varCall.varName.substring }.type
        } catch (e: NoSuchElementException) {
            errorManager.error(varCall.varName.line, varCall.varName.index, "Could not find the requested variable '${varCall.varName.substring}")
            NoneType()
        }
        varCall.resultType = type
        return type
    }

    private fun getVariables(): List<VarNode> {
        return SymbolTableSingleton.getCurrentSymbolTable().getVariablesInCurrentScope(scope)
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition): Type {
        currentPosVarIndex = 16
        currentVarIndex = 0
        val args = functionDefinition.args.map { it as Expression.ArgumentDefinition }.associate { it.name.substring to it.type }
        SymbolTableSingleton.getCurrentSymbolTable().functions.add(
            Function(
                functionDefinition.name.substring,
                args,
                functionDefinition.type
            )
        )
        SymbolTableSingleton.getCurrentSymbolTable().addScopeAt(scope)
        scope.push(scope.pop() + 1)
        scope.push(-1)
        for(i in 0 until functionDefinition.args.size){
            val argumentDefinition = functionDefinition.args[i] as Expression.ArgumentDefinition

            if(i < ARGUMENT_REGISTER_SIZE) {
                currentVarIndex += argumentDefinition.type.getLength()
                SymbolTableSingleton.getCurrentSymbolTable()
                    .defineVariable(scope, argumentDefinition.name.substring, argumentDefinition.type, currentVarIndex)
            } else{
                SymbolTableSingleton.getCurrentSymbolTable()
                    .defineVariable(scope, argumentDefinition.name.substring, argumentDefinition.type, -currentPosVarIndex)
                currentPosVarIndex += 8

            }
        }
        evaluate(functionDefinition.body)
        scope.pop()
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
        comparison.resultType = NormalType(NormalTypes.I8)
        return NormalType(NormalTypes.I8)
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt): Type {
        evaluate(ifStmnt.ifBody)
        evaluate(ifStmnt.condition)
        ifStmnt.elseBody?.let { evaluate(it) }
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
        SymbolTableSingleton.getCurrentSymbolTable().structs[structDefinition.name.substring] = Struct(structDefinition.name.substring, variables)
        return NoneType()
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Type {
        var type = evaluate(instanceGet.expression)
        if (type !is StructType || (type is PointerType && type.subtype !is StructType)) {
            errorManager.error(instanceGet.identifier.line, instanceGet.identifier.index, "The requested value is not a struct.")
            return NoneType()
        }
        if (type is PointerType) type = type.subtype
        val structType = SymbolTableSingleton.getCurrentSymbolTable()
            .getStruct((type as StructType).typeName)!!.variables.firstOrNull { it.first == instanceGet.identifier.substring }
        if (structType == null) {
            errorManager.error(instanceGet.identifier.line, instanceGet.identifier.index, "The struct does not have the requested value.")
            return NoneType()
        }
        instanceGet.resultType = structType.second
        return structType.second
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet): Type {
        val expectedType = evaluate(instanceSet.instanceGet)
        val actual = evaluate(instanceSet.value)
        assertEqualTypes(
            expectedType,
            actual,
            "The expected type does not match the actual value.",
            instanceSet.instanceGet.identifier.line,
            instanceSet.instanceGet.identifier.index
        )
        return NoneType()
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition): Type {
        return argumentDefinition.type
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement): Type {
        SymbolTableSingleton.getCurrentSymbolTable().addScopeAt(scope)
        scope.push(scope.pop() + 1)
        scope.push(-1)
        blockStatement.statements.forEach { evaluate(it) }
        scope.pop()
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
        logical.resultType = NormalType(NormalTypes.I8)
        return NormalType(NormalTypes.I8)
    }

    override fun visitUnary(unary: Expression.Unary): Type {
        val type = evaluate(unary.expression)
        unary.resultType = type
        return type
    }

    override fun visitGrouping(grouping: Expression.Grouping): Type {
        val type = evaluate(grouping.expression)
        grouping.resultType = type
        return type
    }

    override fun visitArrayGet(arrayGet: Expression.ArrayGet): Type {
        var depth = 1
        var currentArrayGet = arrayGet
        while (currentArrayGet.expression is Expression.ArrayGet) {
            depth++
            currentArrayGet = currentArrayGet.expression as Expression.ArrayGet
        }

        val type = evaluate(currentArrayGet.expression)
        var resultType = type
        for (i in 0 until depth) {
            resultType = when (resultType) {
                is ArrayType -> resultType.subType
                is PointerType -> resultType.subtype
                else -> {
                    errorManager.error(-1, -1, "The requested value is not an array.")
                    return NoneType()
                }
            }
        }
        if (resultType is ArrayType)
            return PointerType(resultType.subType)
        arrayGet.resultType = resultType
        return resultType
    }

    override fun visitArraySet(arraySet: Expression.ArraySet): Type {
        val expectedType = evaluate(arraySet.arrayGet)
        val actualType = evaluate(arraySet.value)
        assertEqualTypes(expectedType, actualType, "Could not assign the type '$actualType' to the type '$expectedType'.")
        return NoneType()
    }

    override fun visitImportStmnt(importStmnt: Expression.ImportStmnt): Type {
        SymbolTableSingleton.getCurrentSymbolTable().functions.addAll(SymbolTableSingleton.fileSymbolTables[importStmnt.identifier.substring]!!.functions)
        return NoneType()
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet): Type {
        val type = try {
            var variable = getVariables().first { it.name == pointerGet.varCall.varName.substring }.type

            if (variable is ArrayType)
                variable = variable.subType
            PointerType(variable)
        } catch (e: NoSuchElementException) {
            errorManager.error(pointerGet.varCall.varName.line, pointerGet.varCall.varName.index, "The requested variable could not be found.")
            NoneType()
        }
        pointerGet.resultType = type
        return type
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer): Type {
        val type = evaluate(valueFromPointer.expression)
        if (type !is PointerType) {
            errorManager.error(-1, -1, "Cannot read from a type which is not a pointer ($type).")
            return NoneType()
        }
        valueFromPointer.resultType = type.subtype
        return type.subtype
    }

    override fun visitEmpty(empty: Expression.Empty): Type {
        return NoneType()
    }

    companion object {
        const val ARGUMENT_REGISTER_SIZE = 6
    }
}