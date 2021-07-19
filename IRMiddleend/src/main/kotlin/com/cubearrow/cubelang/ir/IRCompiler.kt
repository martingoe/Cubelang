package com.cubearrow.cubelang.ir

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.definitions.Struct
import com.cubearrow.cubelang.common.errors.ErrorManager
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.tokens.TokenType
import java.io.File
import java.util.*


class IRCompiler(private val expressions: List<Expression>, private val stdlibPath: String, val definedFunctions: Map<String, List<Function>>, val errorManager: ErrorManager) : Expression.ExpressionVisitor<Unit> {
    var currentTempRegisterIndex = 0
    var currentTempLabelIndex = 0

    private val resultList = mutableListOf<IRValue>()
    private var variables: Stack<MutableMap<String, Type>> = Stack()
    private val functions: MutableList<Function> = mutableListOf()
    val structs: MutableMap<String, Struct> = mutableMapOf()

    init {
        variables.push(mutableMapOf())
    }


    fun parse(): List<IRValue> {
        TypeChecker(expressions, errorManager, definedFunctions).checkTypes()
        expressions.filterIsInstance<Expression.FunctionDefinition>().forEach { it -> functions.add(Function(it.name.substring, it.args.map {it as Expression.ArgumentDefinition}.associate { it.name.substring to it.type }, it.type )) }

        for (expression in expressions) {
            evaluate(expression)
        }
        println(resultList.size)
//        val res = cleanUpCopies(cleanUpCopies(resultList))
        val res = cleanUpCopies(resultList)
        println(res.size)
        return res
    }

    private fun cleanUpCopies(list: List<IRValue>): List<IRValue> {
        val result = mutableListOf<IRValue>()
        var i = 0
        while (i < list.size) {
            if (list[i].type == IRType.COPY && list[i + 1].arg0 == list[i].result && list[i + 1].type != IRType.COPY_FROM_DEREF && list[i].result !is Variable) {
                val x = list[i + 1]
                x.arg0 = list[i].arg0
                i++
                result.add(x)
            } else if (list[i].type == IRType.COPY && list[i + 1].arg1 == list[i].result) {
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

    fun getValue(value: Expression): ValueType {
        if (value is Expression.Literal) {
            pushValue(
                IRValue(
                    IRType.COPY,
                    Literal(getValueOfLiteral(value)),
                    null,
                    TemporaryRegister(currentTempRegisterIndex),
                    getTypeOfLiteral(value.value)
                )
            )
            return TemporaryRegister(currentTempRegisterIndex++)
        } else if (value is Expression.VarCall) {
            pushValue(
                IRValue(
                    IRType.COPY,
                    Variable(value.varName.substring),
                    null,
                    TemporaryRegister(currentTempRegisterIndex),
                    variables.last()[value.varName.substring]!!
                )
            )
            return TemporaryRegister(currentTempRegisterIndex++)
        }
        evaluate(value)
        return resultList.last().result!!
    }

    private fun getTypeOfLiteral(value: Any?): Type {
        return when (value) {
            is Int -> NormalType("i32")
            is Char -> NormalType("char")
            else -> error("Could not find the literal type.")
        }
    }


    private fun compileCopy(name: String, value: Expression) {
        when (value) {
            is Expression.PointerGet -> {
                val type = PointerType(variables.last()[value.varCall.varName.substring]!!)
                pushValue(
                    IRValue(
                        IRType.COPY_FROM_REF,
                        Variable(value.varCall.varName.substring),
                        null,
                        TemporaryRegister(++currentTempRegisterIndex),
                        type
                    )
                )
                pushValue(IRValue(IRType.COPY, TemporaryRegister(currentTempRegisterIndex), null, Variable(name), type))
            }
            is Expression.ValueFromPointer -> {
                val valueIRType = getValue(value.expression)
                val resultType = (resultList.last().resultType as PointerType).subtype
                pushValue(
                    IRValue(
                        IRType.COPY_FROM_DEREF,
                        valueIRType,
                        null,
                        TemporaryRegister(++currentTempRegisterIndex),
                        resultType
                    )
                )
                pushValue(IRValue(IRType.COPY, TemporaryRegister(currentTempRegisterIndex), null, Variable(name), resultType))
            }
            else -> {
                val valueType = getValue(value)
                pushValue(IRValue(IRType.COPY, valueType, null, Variable(name), resultList.last().resultType))
            }
        }
    }

    private fun getValueOfLiteral(literal: Expression.Literal): String {
        return if (literal.value is Char)
            (literal.value as Char).code.toString()
        else literal.value.toString()
    }

    override fun visitVarInitialization(varInitialization: Expression.VarInitialization) {
        currentTempRegisterIndex = 0

        val index = resultList.size
        if (varInitialization.valueExpression != null)
            compileCopy(varInitialization.name.substring, varInitialization.valueExpression!!)
        var valueType: Type = resultList.last().resultType
        if (varInitialization.type != NoneType() && varInitialization.valueExpression == null)
            valueType = varInitialization.type

        variables.last()[varInitialization.name.substring] = valueType
        resultList.add(index, IRValue(IRType.VAR_DEF, null, null, Variable(varInitialization.name.substring), valueType))

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
        val leftResult = getValue(operation.leftExpression)
        val rightResult = getValue(operation.rightExpression)
        val result = if (leftResult is TemporaryRegister) leftResult else TemporaryRegister(currentTempRegisterIndex++)
        pushValue(
            IRValue(
                getOperationFromString(operation.operator.substring),
                leftResult,
                rightResult,
                result,
                resultList.last().resultType
            )
        )
    }

    override fun visitCall(call: Expression.Call) {
        for (expression in call.arguments) {
            val valueType = getValue(expression)
            pushValue(IRValue(IRType.PUSH_ARG, valueType, null, null, resultList.last().resultType))
        }
        val functionName = (call.callee as Expression.VarCall).varName.substring
        val type = functions.first { it.name == functionName }
        pushValue(IRValue(IRType.CALL, FunctionLabel(functionName), null, TemporaryRegister(0), type.returnType!!))
    }

    override fun visitLiteral(literal: Expression.Literal) {
        val valueOfLiteral = getValueOfLiteral(literal)
        pushValue(IRValue(IRType.COPY, Literal(valueOfLiteral), null, TemporaryRegister(currentTempRegisterIndex++), getTypeOfLiteral(literal.value)))
    }

    override fun visitVarCall(varCall: Expression.VarCall) {
        pushValue(
            IRValue(
                IRType.COPY,
                Variable(varCall.varName.substring),
                null,
                TemporaryRegister(currentTempRegisterIndex++),
                getVariables()[varCall.varName.substring]!!
            )
        )
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition) {
        currentTempRegisterIndex = 0
        functions.add(Function(functionDefinition.name.substring, functionDefinition.args.map {it as Expression.ArgumentDefinition}
            .associate { it.name.substring to it.type }, functionDefinition.type))
        variables.push(mutableMapOf())
        pushValue(IRValue(IRType.FUNC_DEF, FunctionLabel(functionDefinition.name.substring), null, null, functionDefinition.type))
        for (arg in functionDefinition.args)
            evaluate(arg)
        evaluate(functionDefinition.body)
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
                if (useInverted) getInvJumpOperationFromComparator(comparison.comparator.substring) else
                    getJmpOperationFromComparator(comparison.comparator.substring),
                left,
                right,
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
            val afterLabelIndex = currentTempRegisterIndex++
            getJmpLogicalOrComparison(expression.leftExpression, afterLabelIndex, false)
            getJmpLogicalOrComparison(expression.rightExpression, tempLabelIndex, true)
            pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(afterLabelIndex), NoneType()))
        } else {
            val value = getValue(expression)
            pushValue(IRValue(IRType.JMP_NE, value, Literal("1"), TemporaryLabel(tempLabelIndex), NoneType()))
        }
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt) {
        currentTempRegisterIndex = 0
        if (returnStmnt.returnValue != null) {
            val value = getValue(returnStmnt.returnValue!!)
            pushValue(IRValue(IRType.RET, value, null, null, NoneType()))
        } else
            pushValue(IRValue(IRType.RET, null, null, null, NoneType()))
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt) {
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(currentTempLabelIndex++), NoneType()))
        getJmpLogicalOrComparison(whileStmnt.condition, currentTempLabelIndex++)
        evaluate(whileStmnt.body)

        pushValue(IRValue(IRType.JMP, TemporaryLabel(currentTempLabelIndex - 1), null, null, NoneType()))
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(currentTempLabelIndex), NoneType()))
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt) {
        evaluate(forStmnt.inBrackets[0])
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(currentTempLabelIndex++), NoneType()))
        getJmpLogicalOrComparison(forStmnt.inBrackets[1], currentTempLabelIndex++)
        evaluate(forStmnt.body)

        evaluate(forStmnt.inBrackets[2])
        pushValue(IRValue(IRType.JMP, TemporaryLabel(currentTempLabelIndex - 1), null, null, NoneType()))
        pushValue(IRValue(IRType.LABEL, null, null, TemporaryLabel(currentTempLabelIndex), NoneType()))
    }

    override fun visitStructDefinition(structDefinition: Expression.StructDefinition) {
        val variables = structDefinition.body.map { it.name.substring to it.type }
        structs[structDefinition.name.substring] = Struct(structDefinition.name.substring, variables)
    }

    override fun visitInstanceGet(instanceGet: Expression.InstanceGet) {
        getInstanceGetOrSet(instanceGet, IRType.COPY_FROM_STRUCT_GET, TemporaryRegister(currentTempRegisterIndex++))
    }

    private fun getInstanceGetOrSet(instanceGet: Expression.InstanceGet, irType: IRType, result: ValueType) {
        if (instanceGet.expression is Expression.VarCall) {
            val varName = (instanceGet.expression as Expression.VarCall).varName.substring
            val variable = getVariables()[varName] ?: error("Could not find the variable")
            val structType = if (variable is PointerType) variable.subtype as NormalType else variable as NormalType
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
            val structType = if (type is PointerType) type.subtype as NormalType else type as NormalType
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
            currentTempRegisterIndex = 0
            evaluate(statement)
        }
    }

    override fun visitLogical(logical: Expression.Logical) {
        TODO("Not yet implemented")
    }

    override fun visitUnary(unary: Expression.Unary) {
        val value = getValue(unary.expression)
        if (unary.identifier.substring == "-") {
            pushValue(IRValue(IRType.NEG_UNARY, value, null, TemporaryRegister(currentTempRegisterIndex++), resultList.last().resultType))
            return
        }
        TODO("Not yet implemented")
    }

    override fun visitGrouping(grouping: Expression.Grouping) {
        evaluate(grouping.expression)
    }

    override fun visitArrayGet(arrayGet: Expression.ArrayGet) {
        evaluate(arrayGet.inBrackets)
        val inBracketValue = resultList.last().result

        val expressionValue = getValue(arrayGet.expression)
        val resultType = resultList.last().resultType
        val subType = if (resultType is ArrayType) resultType.subType else if (resultType is PointerType) resultType.subtype else NoneType()
        pushValue(IRValue(IRType.COPY_FROM_ARRAY_ELEM, expressionValue, inBracketValue, TemporaryRegister(currentTempRegisterIndex++), subType))
    }

    override fun visitArraySet(arraySet: Expression.ArraySet) {
        val valueExpression = getValue(arraySet.value)

        evaluate(arraySet.arrayGet.inBrackets)
        val inBracketValue = resultList.last().result
        evaluate(arraySet.arrayGet.expression)
        val arrayValue = resultList.last().result

        // TODO: Pointer Types
        pushValue(
            IRValue(
                IRType.COPY_TO_ARRAY_ELEM,
                arrayValue,
                inBracketValue,
                valueExpression,
                (resultList.last().resultType as ArrayType).subType
            )
        )
    }

    override fun visitImportStmnt(importStmnt: Expression.ImportStmnt) {
        functions.addAll(definedFunctions[importStmnt.identifier.substring]!!)
        val name = (if (importStmnt.identifier.tokenType == TokenType.IDENTIFIER) File(stdlibPath).absolutePath + "/" + importStmnt.identifier.substring else File(importStmnt.identifier.substring.substringBeforeLast(".")).absolutePath) + ".asm"
        pushValue(IRValue(IRType.INCLUDE, Literal(name), null, null, NoneType()))
    }

    override fun visitPointerGet(pointerGet: Expression.PointerGet) {
        pushValue(
            IRValue(
                IRType.COPY_FROM_REF,
                Variable(pointerGet.varCall.varName.substring),
                null,
                TemporaryRegister(currentTempRegisterIndex++),
                PointerType(getVariables()[pointerGet.varCall.varName.substring]!!)
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
                TemporaryRegister(currentTempRegisterIndex++),
                (resultList.last().resultType as PointerType).subtype
            )
        )
    }

    override fun visitEmpty(empty: Expression.Empty) {
        return
    }
}