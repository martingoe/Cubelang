package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.errors.ErrorManager
import com.cubearrow.cubelang.compiler.specificcompilers.ForLoopCompiler
import com.cubearrow.cubelang.compiler.utils.CommonErrorMessages
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.TypeUtils

/**
 * A context class for the variables used in the [Compiler]. This class also contains some utility functions which use the variables of the class.
 */
data class CompilerContext(
    /**
     * The instance to operate on. An example would be compiling the statements in a for loop from [ForLoopCompiler].
     */
    var compilerInstance: Compiler,
    /**
     * The return type of the current function.
     */
    var currentReturnType: Type? = null,
    /**
     * A [Boolean] used to find out whether an operation should push and pop the used registers for the other operation(s).
     */
    var isInSubOperation: Boolean = false,
    /**
     * The depth of the current operation.
     */
    var operationIndex: Int = -1,
    /**
     * A [MutableList] with the indexes of the lowest variable saved on the stack in the current scope/function.
     */
    var stackIndex: MutableList<Int> = ArrayList(),
    /**
     * The current amount of sublabels called '.Ln'.
     */
    var lIndex: Int = 0,
    /**
     * Required for return statements to know weather to jmp or not.
     */
    var jmpOnReturn: Boolean = false,
    /**
     * Tells the compiler to create a separate return segment for functions. Used once you return from a if statement etc.
     */
    var separateReturnSegment: Boolean = false,
    /**
     * The current index for the function arguments.
     */
    var argumentIndex: Int = 0,
    /**
     * The result type of the expression that has just been evaluated.
     */
    var operationResultType: Type? = null,
    /**
     * A [MutableList] which acts as a stack to store all of the variables present.
     */
    var variables: MutableList<MutableMap<String, Compiler.LocalVariable>> = ArrayList(),
    /**
     * A [MutableList] of all defined functions.
     */
    var functions: MutableList<Function> = ArrayList(),
    /**
     * Used for comparisons to know whether or not to jump to an l sublabel.
     */
    var inJmpCondition: Boolean = false,

    /**
     * Evaluates whether or not the comparison is in an or expression.
     */
    var isOR: Boolean = false,

    /**
     * A list of structs.
     */
    var structs: MutableMap<String, Compiler.Struct> = HashMap(),

    val errorManager: ErrorManager
) {


    /**
     * Goes through the expression with recursion until it gets to [Expression.VarCall] and returns the variable if found.
     *
     * @param expression The expression to get the variable from. Should be either [Expression.ArrayGet] or [Expression.VarCall].
     */
    fun getVariableFromArrayGet(expression: Expression): Compiler.LocalVariable? {
        if (expression is Expression.ArrayGet) {
            return getVariableFromArrayGet(expression.expression)
        } else if (expression is Expression.VarCall) {
            return getVariable(expression.varName.substring)
        }
        return null
    }

    /**
     * Returns the [Compiler.LocalVariable] with the requested name from all elements of the current [variables] stack.
     *
     * @param name The name of the requested [Compiler.LocalVariable].
     */
    fun getVariable(name: String): Compiler.LocalVariable? {
        return variables.reduce { acc, mutableMap ->
            acc.putAll(mutableMap)
            acc
        }[name]
    }

    /**
     * Adds a [Function] to the [functions] if a function with the signature doesn't exist yet.
     * @param function The function to add.
     */
    private fun addFunction(function: Function) {
        if (getFunction(function.name, function.args.size) != null) {
            error(-1, -1, "The function with the name ${function.name} and ${function.args.size} arguments has previously been defined.")
        } else {
            functions.add(function)
        }
    }

    /**
     * Adds a list of functions to the [functions] using [addFunction].
     *
     * @param functionsToAdd The functions to add.
     */
    fun addFunctions(functionsToAdd: List<Function>) {
        functionsToAdd.forEach { addFunction(it) }
    }

    /**
     * Returns a function if the name and number of arguments match.
     *
     * @param functionName The name of the function searched for.
     * @param argumentSize The number of arguments in the requested function.
     */
    fun getFunction(functionName: String, argumentSize: Int): Function? =
        functions.firstOrNull { functionName == it.name && it.args.values.size == argumentSize }


    fun getVariablePointer(variable: Compiler.LocalVariable): String = "[rbp-${variable.index}]"


    /**
     * Returns the information needed to move the specific expression to a register.
     *
     * @param expression The expression to move.
     * @return Returns the specific [MoveInformation] needed to perform actions with it.
     */
    fun moveExpressionToX(expression: Expression): MoveInformation {
        val accept = expression.accept(compilerInstance)
        return when (expression) {
            is Expression.VarCall -> {
                moveVarCallToX(expression, accept)
            }
            is Expression.InstanceGet ->
                MoveInformation("", accept, operationResultType!!)

            is Expression.Call -> {
                moveCallToX(expression)
            }
            is Expression.Grouping, is Expression.Operation, is Expression.Comparison, is Expression.Unary -> {
                if (operationResultType == null) {
                    error(-1, -1, "The expression does not return a type.")
                }
                MoveInformation(accept, CompilerUtils.getRegister("ax", TypeUtils.getRawLength(operationResultType!!)), operationResultType!!)
            }
            is Expression.Literal -> {
                val type = Type.getType(null, expression.value)
                MoveInformation("", accept, type)
            }
            is Expression.PointerGet -> {
                MoveInformation(accept, "rax", operationResultType!!)
            }
            is Expression.ValueFromPointer, is Expression.ArrayGet -> {
                val split = accept.split("&")
                MoveInformation(split[0], split[1], operationResultType!!)
            }
            else -> MoveInformation("", "", NormalType("any"))
        }
    }

    private fun moveVarCallToX(
        expression: Expression.VarCall,
        accept: String
    ): MoveInformation {
        val localVariable = getVariable(expression.varName.substring)
        return MoveInformation("", accept, localVariable!!.type)
    }

    private fun moveCallToX(call: Expression.Call): MoveInformation {
        if (call.callee is Expression.VarCall) {
            val varCall = call.callee as Expression.VarCall
            val function = getFunction(varCall.varName.substring, call.arguments.size)
            if (function == null) {
                CommonErrorMessages.xNotFound("called function", varCall.varName, this)
                error("Could not find the function")
            }
            if (function.returnType == null) {
                error(
                    varCall.varName.line,
                    varCall.varName.index,
                    "The called function does not return a value."
                )
            } else {
                return MoveInformation(
                    call.accept(compilerInstance),
                    CompilerUtils.getRegister("ax", TypeUtils.getRawLength(function.returnType!!)),
                    function.returnType!!
                )
            }
        }
        error("Cannot yet compile the requested call")
    }

    /**
     * Evaluates the expression with the [compilerInstance]
     *
     * @param expression The expression to be evaluated
     */
    fun evaluate(expression: Expression): String {
        return expression.accept(compilerInstance)
    }

    /**
     * Returns the asm code to assign an existing variable to a variable.
     *
     * @param variableToAssign The variable to be assigned to the other one.
     * @param variableToAssignTo The variable to be assigned to.
     *
     * @return Returns the required x86_64 NASM code.
     */
    fun assignVariableToVariable(
        variableToAssignTo: Compiler.LocalVariable,
        variableToAssign: Compiler.LocalVariable
    ): String {
        CompilerUtils.checkMatchingTypes(variableToAssign.type, variableToAssignTo.type, context = this)
        val length = TypeUtils.getLength(variableToAssign.type)
        val register = CompilerUtils.getRegister("ax", length)
        return CompilerUtils.moveLocationToLocation(
            "${CompilerUtils.getASMPointerLength(length)} [rbp - ${variableToAssignTo.index}]",
            "${CompilerUtils.getASMPointerLength(length)} [rbp - ${variableToAssign.index}]",
            register
        )
    }

    fun error(line: Int, index: Int, message: String) {
        errorManager.error(line, index, message)
    }
}