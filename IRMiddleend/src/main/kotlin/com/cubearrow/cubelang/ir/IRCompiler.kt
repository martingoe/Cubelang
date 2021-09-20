package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.errors.ErrorManager
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.tokens.TokenType
import java.io.File
import java.lang.Integer.max
import java.util.*


class IRCompiler(
    private val expressions: List<Expression>,
    private val stdlibPath: String,
    private val definedFunctions: Map<String, List<Function>>,
    private val errorManager: ErrorManager
) : Expression.ExpressionVisitor<Unit> {
    private var inSubOperation: Boolean = false
    private var isInTopComparison: Boolean = true
    private var currentTempRegisterIndex = 0
    private var currentTempLabelIndex = 0
    private var currentRegistersToSkip = mutableListOf<Int>()
    private var currentReturnLabelIndex = 0

    private var currentRegistersToPush = 0

    private var resultList = mutableListOf<IRValue>()
    private var variables: Stack<MutableMap<String, Type>> = Stack()
    private val functions: MutableList<Function> = mutableListOf()
    val structs: MutableMap<String, Struct> = mutableMapOf()

    companion object {
        private val excludedFromCleaningInstructions =
            listOf(
                IRType.COPY_FROM_REG_OFFSET,
                IRType.PLUS_OP,
                IRType.DIV_OP,
                IRType.MINUS_OP,
                IRType.MUL_OP,
                IRType.COPY_FROM_DEREF,
                IRType.COPY_FROM_ARRAY_ELEM,
                IRType.COPY_TO_ARRAY_ELEM
            )
        val lengthsOfTypes = mutableMapOf("I8" to 1, "I16" to 2, "I32" to 4, "I64" to 8, "CHAR" to 1)
    }

    init {
        variables.push(mutableMapOf())
    }


    fun parse(): List<IRValue> {
        TypeChecker(expressions, errorManager, definedFunctions).checkTypes()
        expressions.filterIsInstance<Expression.FunctionDefinition>().forEach { it ->
            functions.add(
                Function(
                    it.name.substring,
                    it.args.map { it as Expression.ArgumentDefinition }.associate { it.name.substring to it.type },
                    it.type
                )
            )
        }

        for (expression in expressions) {
            evaluate(expression)
        }
        return cleanUpCopies(resultList)
    }

    private fun cleanUpCopies(list: List<IRValue>): List<IRValue> {
        val result = mutableListOf<IRValue>()
        var i = 0
        while (i < list.size) {
            if (list[i].type == IRType.COPY && list[i].arg0 == list[i].result) {
                i++
                continue
            } else if (list[i].type == IRType.COPY && list.size >= i && list[i + 1].arg0 == list[i].result &&
                !excludedFromCleaningInstructions.contains(list[i + 1].type) && list[i].result !is Variable
            ) {
                val x = list[i + 1]
                x.arg0 = list[i].arg0
                i++
                result.add(x)
            } else if (list[i].type == IRType.COPY && list.size <= i && list[i + 1].arg1 == list[i].result) {
                val x = list[i + 1]
                x.arg1 = list[i].arg0
                i++
                result.add(x)
            } else {
                result.add(list[i])
            }
            i++
        }
        return result.toList()
    }

    private fun getVariables(): MutableMap<String, Type> {
        return variables.fold(mutableMapOf()) { acc, x -> acc.putAll(x); acc }
    }

    private fun pushValue(value: IRValue) {
        resultList.add(value)
    }

    private fun evaluate(expression: Expression) {
        expression.accept(this)
    }

    override fun visitAssignment(assignment: Expression.Assignment) {
        compileCopy(assignment.name.substring, assignment.valueExpression)
    }

    private fun increaseTempRegisterIndex(): Int {
        val beforeIncrease = currentTempRegisterIndex
        currentRegistersToPush = max(currentRegistersToPush, beforeIncrease)
        while (currentRegistersToSkip.contains(currentTempRegisterIndex + 1))
            currentTempRegisterIndex++
        currentTempRegisterIndex++
        return beforeIncrease
    }

    fun getValue(value: Expression): ValueType {
        evaluate(value)
        return resultList.last().result!!
    }

    private fun getTypeOfLiteral(value: Any?): Type {
        return when (value) {
            is Int -> NormalType(NormalTypes.I32)
            is Char -> NormalType(NormalTypes.CHAR)
            else -> error("Could not find the literal type.")
        }
    }


    private fun compileCopy(name: String, value: Expression) {
        when (value) {
            is Expression.ValueFromPointer -> {
                val valueIRType = getValue(value.expression)
                val resultType = (resultList.last().resultType as PointerType).subtype
                if (resultType is StructType) {
                    val splitLength = splitStruct(resultType.getLength())
                    var completedOffset = 0
                    for (i in splitLength) {
                        pushValue(
                            IRValue(
                                IRType.COPY_FROM_REG_OFFSET,
                                valueIRType,
                                Literal(completedOffset.toString()),
                                TemporaryRegister(currentTempRegisterIndex),
                                getIntTypeFromLength(i)
                            )
                        )
                        pushValue(IRValue(IRType.COPY, resultList.last().result, null, Variable(name, completedOffset), getIntTypeFromLength(i)))
                        completedOffset += i
                    }
                } else {
                    pushValue(
                        IRValue(
                            IRType.COPY_FROM_DEREF,
                            valueIRType,
                            null,
                            TemporaryRegister(increaseTempRegisterIndex()),
                            resultType
                        )
                    )
                    pushValue(IRValue(IRType.COPY, resultList.last().result, null, Variable(name), resultType))
                }
            }
            else -> {
                val valueType = getValue(value)
                pushValue(IRValue(IRType.COPY, valueType, null, Variable(name), resultList.last().resultType))
            }
        }
    }

    private fun getIntTypeFromLength(length: Int): NormalType {
        return when (length) {
            1 -> NormalType(NormalTypes.I8)
            2 -> NormalType(NormalTypes.I16)
            4 -> NormalType(NormalTypes.I32)
            8 -> NormalType(NormalTypes.I64)
            else -> error("Could not find an int type for length ${length}")
        }
    }

    private fun splitStruct(structLength: Int): List<Int> {
        var remainder = structLength
        val result = mutableListOf<Int>()
        for (i in arrayOf(8, 4, 2, 1)) {
            for (j in 0 until (remainder / i)) {
                result.add(i)
            }
            remainder %= i
        }
        return result
    }

    private fun getValueOfLiteral(literal: Expression.Literal): String {
        return if (literal.value is Char)
            (literal.value as Char).code.toString()
        else literal.value.toString()
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization) {
        clearUsedRegisters()
        currentTempRegisterIndex = 0

        val index = resultList.size
        if (varInitialization.valueExpression != null)
            compileCopy(varInitialization.name.substring, varInitialization.valueExpression!!)
        val valueType: Type = varInitialization.type

        variables.last()[varInitialization.name.substring] = valueType
        resultList.add(index, IRValue(IRType.VAR_DEF, null, null, Variable(varInitialization.name.substring), valueType))
    }

    private fun clearUsedRegisters() {
        currentTempRegisterIndex = 0
        currentRegistersToSkip.clear()
    }

    private fun getOperationFromString(string: String): IRType {
        return when (string) {
            "+" -> IRType.PLUS_OP
            "-" -> IRType.MINUS_OP
            "*" -> IRType.MUL_OP
            "/" -> IRType.DIV_OP

            else -> error("Could not find the requested operation")
        }
    }

    override fun visitOperation(operation: Expression.Operation) {
        val wasInSub = inSubOperation
        inSubOperation = true
        val previousTempRegisterIndex = currentTempRegisterIndex

//        val index = resultList.size
        var rhs = getValue(operation.rightExpression)
        // If it is not a suboperation,
        if (!wasInSub) {
            currentTempRegisterIndex = previousTempRegisterIndex
            increaseTempRegisterIndex()
            pushValue(IRValue(IRType.COPY, rhs, null, TemporaryRegister(currentTempRegisterIndex), resultList.last().resultType))
            currentRegistersToSkip.add(currentTempRegisterIndex)
            rhs = resultList.last().result!!
            currentTempRegisterIndex = previousTempRegisterIndex
        }

        val lhs = getValue(operation.leftExpression)
        val result =
            if (lhs is TemporaryRegister) lhs else TemporaryRegister(increaseTempRegisterIndex())

        pushValue(
            IRValue(
                getOperationFromString(operation.operator.substring),
                lhs,
                rhs,
                result,
                resultList.last().resultType
            )
        )
        if (!wasInSub) {
//            saveRegisters(previousTempRegisterIndex, index, result)
            inSubOperation = false
        }
        clearUsedRegisters()
    }

    private fun saveRegisters(previousTempRegisterIndex: Int, index: Int) {
        for (i in 1..previousTempRegisterIndex + 1)
            resultList.add(index, IRValue(IRType.PUSH_REG, TemporaryRegister(i), null, null, NoneType()))

        for (i in 1..previousTempRegisterIndex + 1)
            resultList.add(IRValue(IRType.POP_REG, TemporaryRegister(i), null, null, NoneType()))
    }

    override fun visitCall(call: Expression.Call) {
        val previousTempRegisterIndex = currentTempRegisterIndex
        for (expression in call.arguments) {
            val valueType = getValue(expression)
            currentTempRegisterIndex = previousTempRegisterIndex
            pushValue(IRValue(IRType.PUSH_ARG, valueType, null, null, resultList.last().resultType))
        }
        val functionName = (call.callee as Expression.VarCall).varName.substring
        val type = functions.first { it.name == functionName }
        pushValue(IRValue(IRType.CALL, FunctionLabel(functionName), null, TemporaryRegister(0), type.returnType!!))
    }

    override fun visitLiteral(literal: Expression.Literal) {
        val valueOfLiteral = getValueOfLiteral(literal)
        pushValue(
            IRValue(
                IRType.COPY,
                Literal(valueOfLiteral),
                null,
                TemporaryRegister(increaseTempRegisterIndex()),
                getTypeOfLiteral(literal.value)
            )
        )
    }

    override fun visitVarCall(varCall: Expression.VarCall) {
        pushValue(
            IRValue(
                IRType.COPY,
                Variable(varCall.varName.substring),
                null,
                TemporaryRegister(increaseTempRegisterIndex()),
                getVariables()[varCall.varName.substring]!!
            )
        )
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        clearUsedRegisters()
        currentRegistersToPush = 0
        currentTempLabelIndex = 1
        currentReturnLabelIndex = 0
        functions.add(Function(functionDefinition.name.substring, functionDefinition.args.map { it as Expression.ArgumentDefinition }
            .associate { it.name.substring to it.type }, functionDefinition.type))
        variables.push(mutableMapOf())
        pushValue(IRValue(IRType.FUNC_DEF, FunctionLabel(functionDefinition.name.substring), null, null, functionDefinition.type))

        for (arg in functionDefinition.args)
            evaluate(arg)

        val pushIndex = resultList.size
        evaluate(functionDefinition.body)

        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(currentReturnLabelIndex), NoneType()))
        saveRegisters(currentRegistersToPush, pushIndex)
        pushValue(IRValue(IRType.RET, null, null, null, NoneType()))
        variables.pop()
    }

    override fun visitComparison(comparison: Expression.Comparison) {
        TODO("Not yet implemented")
    }

    private fun getInvJumpOperationFromComparator(comparisonString: String): IRType {
        return when (comparisonString) {
            "==" -> IRType.JMP_NE
            "!=" -> IRType.JMP_EQ
            "<" -> IRType.JMP_GE
            "<=" -> IRType.JMP_G
            ">" -> IRType.JMP_LE
            ">=" -> IRType.JMP_L
            else -> error("Could not find the requested operation")
        }
    }

    private fun getJmpOperationFromComparator(comparisonString: String): IRType {
        return when (comparisonString) {
            "==" -> IRType.JMP_EQ
            "!=" -> IRType.JMP_NE
            "<" -> IRType.JMP_L
            "<=" -> IRType.JMP_LE
            ">" -> IRType.JMP_G
            ">=" -> IRType.JMP_GE
            else -> error("Could not find the requested operation")
        }
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt) {
        val labelIndex = currentTempLabelIndex
        currentTempLabelIndex += if (ifStmnt.elseBody != null) 2 else 1

        getJmpLogicalOrComparison(ifStmnt.condition, labelIndex)

        evaluate(ifStmnt.ifBody)

        if (ifStmnt.elseBody != null)
            pushValue(IRValue(IRType.JMP, TemporaryLabel(labelIndex + 1), null, null, NoneType()))
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(labelIndex), NoneType()))
        if (ifStmnt.elseBody != null) {
            evaluate(ifStmnt.elseBody!!)
            pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(labelIndex + 1), NoneType()))
        }
    }

    private fun getJmpComparison(comparison: Expression.Comparison, tempLabelIndex: Int, useInverted: Boolean = true) {
        val left = getValue(comparison.leftExpression)
        val right = getValue(comparison.rightExpression)

        pushValue(
            IRValue(
                IRType.CMP,
                left,
                right,
                null,
                resultList.last().resultType
            )
        )

        pushValue(
            IRValue(
                if (useInverted) getInvJumpOperationFromComparator(comparison.comparator.substring) else
                    getJmpOperationFromComparator(comparison.comparator.substring),
                null,
                null,
                TemporaryLabel(tempLabelIndex),
                resultList.last().resultType
            )
        )
    }

    private fun getJmpLogicalOrComparison(expression: Expression, tempLabelIndex: Int, useInverted: Boolean = true) {
        if (expression is Expression.Comparison) getJmpComparison(expression, tempLabelIndex, useInverted)
        else if (expression is Expression.Logical && expression.logical.tokenType == TokenType.AND) {
            getJmpLogicalOrComparison(expression.leftExpression, tempLabelIndex)
            getJmpLogicalOrComparison(expression.rightExpression, tempLabelIndex)
        } else if (expression is Expression.Logical && expression.logical.tokenType == TokenType.OR) {
            val afterLabelIndex = increaseTempRegisterIndex()
            getJmpLogicalOrComparison(expression.leftExpression, afterLabelIndex, false)
            getJmpLogicalOrComparison(expression.rightExpression, tempLabelIndex, true)
            pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(afterLabelIndex), NoneType()))
        } else {
            val value = getValue(expression)
            pushValue(IRValue(IRType.JMP_NE, value, Literal("1"), TemporaryLabel(tempLabelIndex), NoneType()))
        }
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt) {
        if (returnStmnt.returnValue != null) {
            val value = getValue(returnStmnt.returnValue!!)
            pushValue(IRValue(IRType.COPY, value, null, TemporaryRegister(0), resultList.last().resultType))
        }
        pushValue(IRValue(IRType.JMP, TemporaryLabel(currentReturnLabelIndex), null, null, NoneType()))
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt) {
        val firstLabel = currentTempLabelIndex++
        val secondLabel = currentTempLabelIndex++
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(firstLabel), NoneType()))
        getJmpLogicalOrComparison(whileStmnt.condition, secondLabel)
        evaluate(whileStmnt.body)

        pushValue(IRValue(IRType.JMP, TemporaryLabel(firstLabel), null, null, NoneType()))
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(secondLabel), NoneType()))
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt) {
        evaluate(forStmnt.inBrackets[0])
        val resetLabel = currentTempLabelIndex++
        val exitLabel = currentTempLabelIndex++
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(resetLabel), NoneType()))
        getJmpLogicalOrComparison(forStmnt.inBrackets[1], exitLabel)
        clearUsedRegisters()
        evaluate(forStmnt.body)
        clearUsedRegisters()

        evaluate(forStmnt.inBrackets[2])
        pushValue(IRValue(IRType.JMP, TemporaryLabel(resetLabel), null, null, NoneType()))
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(exitLabel), NoneType()))
    }

    override fun visitStructDefinition(structDefinition: Expression.StructDefinition) {
        val name = structDefinition.name.substring
        val variables = structDefinition.body.map { it.name.substring to it.type }
        structs[name] = Struct(name, variables)
        lengthsOfTypes[name] = variables.fold(0) { acc, pair -> acc + pair.second.getLength() }
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet) {
        getInstanceGetOrSet(instanceGet, IRType.COPY_FROM_STRUCT_GET, TemporaryRegister(increaseTempRegisterIndex()))
    }

    private fun getInstanceGetOrSet(instanceGet: Expression.InstanceGet, irType: IRType, result: ValueType) {
        if (instanceGet.expression is Expression.VarCall) {
            val varName = (instanceGet.expression as Expression.VarCall).varName.substring
            val variable = getVariables()[varName] ?: error("Could not find the variable")
            val structType = if (variable is PointerType) variable.subtype as StructType else variable as StructType
            pushValue(
                IRValue(
                    irType,
                    Variable(varName),
                    StructSubvalue(instanceGet.identifier.substring, structType),
                    result,
                    structs[structType.typeName]!!.variables.first { instanceGet.identifier.substring == it.first }.second
                )
            )
        } else {
            val value = getValue(instanceGet.expression)
            val type = resultList.last().resultType
            val structType = if (type is PointerType) type.subtype as StructType else type as StructType
            pushValue(
                IRValue(
                    irType,
                    value,
                    StructSubvalue(instanceGet.identifier.substring, structType),
                    result,
                    structs[structType.typeName]!!.variables.first { instanceGet.identifier.substring == it.first }.second
                )
            )
        }
    }

    override fun visitInstanceSet(instanceSet: Expression.InstanceSet) {
        val resultValue = getValue(instanceSet.value)
        getInstanceGetOrSet(instanceSet.instanceGet, IRType.COPY_TO_STRUCT_GET, resultValue)
    }

    override fun visitArgumentDefinition(argumentDefinition: Expression.ArgumentDefinition) {
        variables.last()[argumentDefinition.name.substring] = argumentDefinition.type
        pushValue(IRValue(IRType.VAR_DEF, null, null, Variable(argumentDefinition.name.substring), argumentDefinition.type))
        pushValue(IRValue(IRType.POP_ARG, null, null, Variable(argumentDefinition.name.substring), argumentDefinition.type))
    }

    override fun visitBlockStatement(blockStatement: Expression.BlockStatement) {
        for (statement in blockStatement.statements) {
            clearUsedRegisters()
            evaluate(statement)
            if (statement is Expression.ReturnStmnt)
                break
        }
    }

    override fun visitLogical(logical: Expression.Logical) {
        TODO("Not yet implemented")
    }

    override fun visitUnary(unary: Expression.Unary) {
        val value = getValue(unary.expression)
        if (unary.identifier.substring == "-") {
            pushValue(IRValue(IRType.NEG_UNARY, value, null, TemporaryRegister(increaseTempRegisterIndex()), resultList.last().resultType))
            return
        }
        TODO("Not yet implemented")
    }

    override fun visitGrouping(grouping: Expression.Grouping) {
        evaluate(grouping.expression)
    }

    private fun getTypeOfExpression(expression: Expression): Type? {
        return when (expression) {
            is Expression.Literal -> getTypeOfLiteral(expression.value)
            is Expression.VarCall -> getVariables()[expression.varName.substring]!!
            is Expression.ArrayGet -> (getTypeOfExpression(expression.expression) as ArrayType).subType
            is Expression.Call -> functions.first { it.name == (expression.callee as Expression.VarCall).varName.substring }.returnType
            is Expression.Grouping -> getTypeOfExpression(expression.expression)
            is Expression.Unary -> getTypeOfExpression(expression.expression)

            is Expression.Operation -> getTypeOfExpression(expression.leftExpression)
            else -> error("Unknown type.")
        }
    }


    override fun visitArrayGet(arrayGet: Expression.ArrayGet) {
        val arrayGets = getAllNestedArrayGets(arrayGet)

        var currentType = getTypeOfExpression(arrayGets.last().expression)

        val expressionValue = if (currentType is ArrayType) {
            Variable((arrayGets.last().expression as Expression.VarCall).varName.substring)
        } else if (currentType is PointerType && arrayGets.size == 1) {
            getValue(arrayGets[0].expression)
        } else {
            errorManager.error(-1, -1, "The compiler does not currently support the given configuration of array get expressions.")
            return
        }

        val resultOffsetRegister = TemporaryRegister(increaseTempRegisterIndex())
        pushValue(IRValue(IRType.COPY, Literal("0"), null, resultOffsetRegister, NormalType(NormalTypes.I64)))

        for (arrayGetToAddToCount in arrayGets.reversed()) {
            val lengthToBeAdded = getValue(arrayGetToAddToCount.inBrackets)
            currentType = when (currentType) {
                is PointerType -> currentType.subtype
                is ArrayType -> currentType.subType
                else -> NoneType()
            }

            multiplyValueByTypeLength(lengthToBeAdded, currentType)
            addValueToRegister(resultOffsetRegister, lengthToBeAdded)
        }
        if (currentType is ArrayType) {
            currentType = PointerType(currentType.subType)

        }
        pushValue(IRValue(IRType.COPY_FROM_ARRAY_ELEM, expressionValue, resultOffsetRegister, resultOffsetRegister, currentType!!))
    }

    private fun getAllNestedArrayGets(arrayGet: Expression.ArrayGet): MutableList<Expression.ArrayGet> {
        val arrayGets = mutableListOf(arrayGet)
        while (arrayGets.last().expression is Expression.ArrayGet) {
            arrayGets.add(arrayGets.last().expression as Expression.ArrayGet)
        }
        return arrayGets
    }

    private fun addValueToRegister(
        resultRegister: TemporaryRegister,
        value: ValueType
    ) {
        pushValue(
            IRValue(
                IRType.PLUS_OP,
                resultRegister,
                value,
                resultRegister,
                NormalType(
                    NormalTypes.I32
                )
            )
        )
    }

    private fun multiplyValueByTypeLength(res: ValueType, currentType: Type) {
        pushValue(
            IRValue(
                IRType.MUL_OP,
                res,
                Literal(
                    currentType.getLength().toString()
                ),
                res,
                NormalType(
                    NormalTypes.I32
                )
            )
        )
    }

    override fun visitArraySet(arraySet: Expression.ArraySet) {
        evaluate(arraySet.arrayGet)
        val arrayValue = resultList.last().arg0
        val inBracketValue = resultList.last().arg1
        val resultType = resultList.last().resultType


        resultList.removeLast()
        val valueExpression = getValue(arraySet.value)
        pushValue(
            IRValue(
                IRType.COPY_TO_ARRAY_ELEM,
                arrayValue,
                inBracketValue,
                valueExpression,
                resultType
            )
        )
    }

    override fun visitImportStmnt(importStmnt: Expression.ImportStmnt) {
        functions.addAll(definedFunctions[importStmnt.identifier.substring]!!)
        val name =
            (if (importStmnt.identifier.tokenType == TokenType.IDENTIFIER) File(stdlibPath).absolutePath + "/" + importStmnt.identifier.substring else File(
                importStmnt.identifier.substring.substringBeforeLast(".")
            ).absolutePath) + ".asm"
        pushValue(IRValue(IRType.INCLUDE, Literal(name), null, null, NoneType()))
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet) {
        var type = getVariables()[pointerGet.varCall.varName.substring]!!
        println(type)
        if (type is ArrayType)
            type = type.subType
        println(type)
        pushValue(
            IRValue(
                IRType.COPY_FROM_REF,
                Variable(pointerGet.varCall.varName.substring),
                null,
                TemporaryRegister(increaseTempRegisterIndex()),
                PointerType(type)
            )
        )
    }

    override fun visitValueFromPointer(valueFromPointer: Expression.ValueFromPointer) {
        evaluate(valueFromPointer.expression)
        pushValue(
            IRValue(
                IRType.COPY_FROM_DEREF,
                resultList.last().result,
                null,
                TemporaryRegister(increaseTempRegisterIndex()),
                (resultList.last().resultType as PointerType).subtype
            )
        )
    }

    override fun visitEmpty(empty: Expression.Empty) {
        return
    }
}