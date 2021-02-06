package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.compiler.specificcompilers.ForLoopCompiler
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.CommonErrorMessages
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.NormalType
import com.cubearrow.cubelang.utils.Type

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
    var functions: MutableList<Compiler.Function> = ArrayList(),
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
    var structs: MutableMap<String, Compiler.Struct> = HashMap()
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
    fun addFunction(function: Compiler.Function) {
        if (getFunction(function.name, function.args.size) != null) {
            Main.error(-1, -1, "The function with the name ${function.name} and ${function.args.size} arguments has previously been defined.")
        } else {
            functions.add(function)
        }
    }

    /**
     * Adds a list of functions to the [functions] using [addFunction].
     *
     * @param functionsToAdd The functions to add.
     */
    fun addFunctions(functionsToAdd: List<Compiler.Function>) {
        functionsToAdd.forEach { addFunction(it) }
    }

    /**
     * Returns a function if the name and number of arguments match.
     *
     * @param functionName The name of the function searched for.
     * @param argumentSize The number of arguments in the requested function.
     */
    fun getFunction(functionName: String, argumentSize: Int): Compiler.Function? =
        functions.firstOrNull { functionName == it.name && it.args.values.size == argumentSize }


    private fun beforeAndPointerArrayGet(arrayGet: Expression.ArrayGet): MoveInformation {
        val s = arrayGet.accept(compilerInstance).split("&")
        return MoveInformation(s[0], s[1], operationResultType!!)
    }

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
            is Expression.InstanceGet -> moveInstanceGetToX(expression, accept)
            is Expression.ArrayGet -> {
                beforeAndPointerArrayGet(expression)
            }
            is Expression.Call -> {
                moveCallToX(expression)
            }
            is Expression.Grouping, is Expression.Operation, is Expression.Comparison, is Expression.Unary -> {
                if (operationResultType == null) {
                    Main.error(-1, -1, "The expression does not return a type.")
                }
                MoveInformation(accept, CompilerUtils.getRegister("ax", operationResultType!!.getRawLength()), operationResultType!!)
            }
            is Expression.Literal -> {
                val type = ExpressionUtils.getType(null, expression.value)
                MoveInformation("", accept, type)
            }
            is Expression.PointerGet -> {
                MoveInformation(accept, "rax", operationResultType!!)
            }
            is Expression.ValueFromPointer -> {
                val split = accept.split("&")
                MoveInformation(split[0], split[1], operationResultType!!)
            }
            else -> MoveInformation("", "", NormalType("any"))
        }
    }

    private fun moveInstanceGetToX(expression: Expression.InstanceGet,
    accept: String): MoveInformation{
        val varCall = expression.expression as Expression.VarCall
        val localVariable = getVariable(varCall.varName.substring)
        if (localVariable == null) {
            CommonErrorMessages.xNotFound("variable", varCall.varName)
            error("")
        }
        return MoveInformation("", accept, operationResultType!!)
    }
    private fun moveVarCallToX(
        expression: Expression.VarCall,
        accept: String
    ): MoveInformation {
        val localVariable = getVariable(expression.varName.substring)
        if (localVariable == null) {
            CommonErrorMessages.xNotFound("variable", expression.varName)
            error("")
        }
        return MoveInformation("", accept, localVariable.type)
    }

    private fun moveCallToX(call: Expression.Call): MoveInformation {
        if (call.callee is Expression.VarCall) {
            val function = getFunction(call.callee.varName.substring, call.arguments.size)
            if (function == null) {
                CommonErrorMessages.xNotFound("called function", call.callee.varName)
                error("Could not find the function")
            }
            if (function.returnType == null) {
                Main.error(
                    call.callee.varName.line,
                    call.callee.varName.index,
                    "The called function does not return a value."
                )
            } else {
                return MoveInformation(
                    call.accept(compilerInstance),
                    CompilerUtils.getRegister("ax", function.returnType!!.getRawLength()),
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
}