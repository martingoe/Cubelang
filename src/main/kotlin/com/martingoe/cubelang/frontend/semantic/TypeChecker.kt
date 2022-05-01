package com.martingoe.cubelang.frontend.semantic

import com.martingoe.cubelang.common.*
import com.martingoe.cubelang.common.definitions.Function
import com.martingoe.cubelang.common.definitions.Struct
import com.martingoe.cubelang.common.errors.ErrorManager
import com.martingoe.cubelang.common.tokens.Token
import java.util.*

/**
 * Check if the given types match and assign the resulting types to expressions.
 *
 * This class also fills the [[SymbolTableSingleton]] with the needed information
 *
 */
class TypeChecker(
    private val expressions: List<Statement>,
    private val errorManager: ErrorManager,
    private val definedFunctions: Map<String, List<Function>>
) : Expression.ExpressionVisitor<Type>, Statement.StatementVisitor<Type> {
    private var currentVarIndex = 0
    private var scope = Stack<Int>()
    private var variables: Stack<MutableMap<String, Type>> = Stack()

    init {
        variables.push(mutableMapOf())
        scope.push(-1)
    }

    fun checkTypes() {
        expressions.forEach { evaluateStmnt(it) }
    }

    override fun visitAssignment(assignment: Expression.Assignment): Type {
        val type = evaluate(assignment.leftSide)
        val valueType = evaluate(assignment.valueExpression)
        assertEqualTypes(type, valueType, "The two types do not match.", assignment.equals)
        if (valueType is NormalType && valueType.type == NormalTypes.ANY_INT) {
            assignment.valueExpression.resultType = type
        }
        assignment.resultType = type
        return type
    }

    override fun visitVarInitialization(varInitialization: Statement.VarInitialization): Type {
        var type = varInitialization.type
        val actualType = varInitialization.valueExpression?.let { evaluate(it) }
        if (type !is NoneType && actualType != null) {
            assertEqualTypes(
                type,
                actualType,
                "The implicit type ($actualType) does not match the explicit type ($type).",
                varInitialization.name
            )
        } else if (type is NoneType && actualType != null) {
            if (actualType is NormalType && actualType.type == NormalTypes.ANY_INT) {
                varInitialization.type = NormalType(NormalTypes.I32)
                varInitialization.valueExpression!!.resultType = NormalType(NormalTypes.I32)
                type = NormalType(NormalTypes.I32)
            } else {
                varInitialization.type = actualType
                type = actualType
            }
        } else if (type == NoneType()) {
            errorManager.error(
                varInitialization.name,
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
            operation.operator
        )
        operation.resultType = leftType
        return leftType
    }

    private fun assertEqualTypes(type1: Type, type2: Type, errorMessage: String, token: Token) {
        if (type1 != type2) {
            errorManager.error(token, errorMessage)
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
            errorManager.error(call.bracket, "Could not find the called function.")
            return NoneType()
        }
        var i = 0
        for (arg in function.args) {
            assertEqualTypes(evaluate(call.arguments[i]), arg.value, "Mismatched types when calling '${function.name}'.", call.bracket)
            i++
        }
        call.resultType = function.returnType ?: NoneType()
        return function.returnType ?: NoneType()
    }

    override fun visitLiteral(literal: Expression.Literal): Type {
        val type = when (literal.value) {
            is Int -> NormalType(NormalTypes.ANY_INT)
            is Char -> NormalType(NormalTypes.CHAR)
            else -> {
                errorManager.error(literal.token!!, "Could not find the literal type.")
                NormalType(NormalTypes.ANY)
            }
        }
        literal.resultType = type
        return type
    }

    override fun visitVarCall(varCall: Expression.VarCall): Type {
        val type = try {
            getVariables().first { it.name == varCall.varName.substring }.type
        } catch (e: NoSuchElementException) {
            errorManager.error(varCall.varName, "Could not find the requested variable '${varCall.varName.substring}")
            NoneType()
        }
        varCall.resultType = type
        return type
    }

    private fun getVariables(): List<VarNode> {
        return SymbolTableSingleton.getCurrentSymbolTable().getVariablesInCurrentScope(scope)
    }

    override fun visitFunctionDefinition(functionDefinition: Statement.FunctionDefinition): Type {
        currentVarIndex = 0
        val args = mapArgumentDefinitionToMap(functionDefinition.args)
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

        var i = 0
        var posOffset = 16
        functionDefinition.args.forEach {
            val ARGUMENT_REG_COUNT = 6
            if (i < ARGUMENT_REG_COUNT) {

                currentVarIndex += it.type.getLength()
                SymbolTableSingleton.getCurrentSymbolTable()
                    .defineVariable(scope, it.name.substring, it.type, currentVarIndex)
            } else {
                currentVarIndex += it.type.getLength()
                SymbolTableSingleton.getCurrentSymbolTable()
                    .defineVariable(scope, it.name.substring, it.type, -posOffset)
                posOffset += 8
            }
            i++
        }
        evaluateStmnt(functionDefinition.body)
        scope.pop()
        return NoneType()
    }

    private fun mapArgumentDefinitionToMap(args: List<Statement.ArgumentDefinition>): Map<String, Type> {
        return args.associate { it.name.substring to it.type }
    }

    override fun visitComparison(comparison: Expression.Comparison): Type {
        val leftType = evaluate(comparison.leftExpression)
        val rightType = evaluate(comparison.rightExpression)
        assertEqualTypes(
            leftType,
            rightType,
            "Cannot compute comparisons on mismatching types ($leftType and $rightType)",
            comparison.comparator
        )
        if (rightType is NormalType && rightType.type == NormalTypes.ANY_INT) {
            comparison.rightExpression.resultType = comparison.leftExpression.resultType
        }
        comparison.resultType = leftType
        return NormalType(NormalTypes.I8)
    }

    override fun visitIfStmnt(ifStmnt: Statement.IfStmnt): Type {
        evaluateStmnt(ifStmnt.ifBody)
        evaluate(ifStmnt.condition)
        ifStmnt.elseBody?.let { evaluateStmnt(it) }
        return NoneType()
    }

    override fun visitReturnStmnt(returnStmnt: Statement.ReturnStmnt): Type {
        returnStmnt.returnValue?.let { evaluate(it) }
        return NoneType()
    }

    override fun visitWhileStmnt(whileStmnt: Statement.WhileStmnt): Type {
        evaluate(whileStmnt.condition)
        evaluateStmnt(whileStmnt.body)
        return NoneType()
    }

    override fun visitForStmnt(forStmnt: Statement.ForStmnt): Type {
        forStmnt.inBrackets.forEach { evaluateStmnt(it) }
        evaluateStmnt(forStmnt.body)
        return NoneType()
    }

    override fun visitStructDefinition(structDefinition: Statement.StructDefinition): Type {
        val variables = structDefinition.body.map { it.name.substring to it.type }
        SymbolTableSingleton.getCurrentSymbolTable().structs[structDefinition.name.substring] = Struct(structDefinition.name.substring, variables)
        return NoneType()
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet): Type {
        var type = evaluate(instanceGet.expression)
        if (type !is StructType || (type is PointerType && type.subtype !is StructType)) {
            errorManager.error(instanceGet.identifier, "The requested value is not a struct.")
            return NoneType()
        } else if (type is PointerType) type = type.subtype
        val structType = SymbolTableSingleton.getCurrentSymbolTable()
            .getStruct((type as StructType).typeName)!!.variables.firstOrNull { it.first == instanceGet.identifier.substring }
        if (structType == null) {
            errorManager.error(instanceGet.identifier, "The struct does not have the requested value.")
            return NoneType()
        }
        instanceGet.resultType = structType.second
        return structType.second
    }


    override fun visitArgumentDefinition(argumentDefinition: Statement.ArgumentDefinition): Type {
        return argumentDefinition.type
    }

    override fun visitBlockStatement(blockStatement: Statement.BlockStatement): Type {
        SymbolTableSingleton.getCurrentSymbolTable().addScopeAt(scope)
        scope.push(scope.pop() + 1)
        scope.push(-1)
        blockStatement.statements.forEach { evaluateStmnt(it) }
        scope.pop()
        return NoneType()
    }

    private fun evaluateStmnt(it: Statement) {
        it.accept(this)
    }

    override fun visitLogical(logical: Expression.Logical): Type {
        val leftType = evaluate(logical.leftExpression)
        val rightType = evaluate(logical.rightExpression)
        assertEqualTypes(
            leftType,
            rightType,
            "Cannot compute logical expressions on mismatching types ($leftType and $rightType)",
            logical.logical
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
        val arrayGetList = mutableListOf(arrayGet)
        evaluate(arrayGet.inBrackets)
        while (arrayGetList.last().expression is Expression.ArrayGet) {
            depth++
            arrayGetList.add(arrayGetList.last().expression as Expression.ArrayGet)
            evaluate(arrayGetList.last().inBrackets)
        }

        val type = evaluate(arrayGetList.last().expression)
        var resultType = type
        for (i in 0 until depth) {
            arrayGetList[i].resultType = resultType

            resultType = when (resultType) {
                is ArrayType -> resultType.subType
                is PointerType -> resultType.subtype
                else -> {
                    errorManager.error(arrayGet.bracket, "The requested value is not an array.")
                    return NoneType()
                }
            }
        }
        if (resultType is ArrayType)
            resultType = PointerType(resultType.subType)
        arrayGet.resultType = resultType
        return resultType
    }


    override fun visitImportStmnt(importStmnt: Statement.ImportStmnt): Type {
        SymbolTableSingleton.getCurrentSymbolTable().functions.addAll(definedFunctions[importStmnt.identifier.substring]!!)
        return NoneType()
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet): Type {
        val varCall = pointerGet.expression as Expression.VarCall

        val type = try {
            var variable = getVariables().first { it.name == varCall.varName.substring }.type

            if (variable is ArrayType)
                variable = variable.subType
            PointerType(variable)
        } catch (e: NoSuchElementException) {
            errorManager.error(varCall.varName, "The requested variable could not be found.")
            NoneType()
        }
        pointerGet.resultType = type
        return type
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer): Type {
        val type = evaluate(valueFromPointer.expression)
        if (type !is PointerType) {
            errorManager.error(valueFromPointer.star, "Cannot read from a type which is not a pointer ($type).")
            return NoneType()
        }
        valueFromPointer.resultType = type.subtype
        return type.subtype
    }

    override fun visitEmpty(empty: Statement.Empty): Type {
        return NoneType()
    }

    override fun visitRegister(register: Expression.Register): Type {
        TODO("Not yet implemented")
    }

    override fun visitExpressionStatement(expressionStatement: Statement.ExpressionStatement): Type {
        evaluate(expressionStatement.expression)
        return NoneType()
    }

    override fun acceptFramePointer(framePointer: Expression.FramePointer): Type {
        framePointer.resultType = NormalType(NormalTypes.I64)
        return NormalType(NormalTypes.I64)
    }

    override fun acceptExtendTo64Bits(extendTo64Bit: Expression.ExtendTo64Bit): Type {
        return NormalType(NormalTypes.I64)
    }

    override fun visitExternFunctionDefinition(externFunctionDefinition: Statement.ExternFunctionDefinition): Type {
        SymbolTableSingleton.getCurrentSymbolTable().functions.add(
            Function(
                externFunctionDefinition.name.substring,
                mapArgumentDefinitionToMap(externFunctionDefinition.args),
                externFunctionDefinition.type
            )
        )
        return NoneType()
    }

    override fun visitStringLiteral(stringLiteral: Expression.StringLiteral): Type {
        SymbolTableSingleton.getCurrentSymbolTable().addStringLiteral(stringLiteral.value.substring)
        stringLiteral.resultType = PointerType(NormalType(NormalTypes.CHAR))
        return PointerType(NormalType(NormalTypes.CHAR))
    }
}
