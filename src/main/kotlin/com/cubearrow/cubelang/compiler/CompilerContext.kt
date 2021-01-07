package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.compiler.specificcompilers.ForLoopCompiler
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.CommonErrorMessages
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.NormalType
import com.cubearrow.cubelang.utils.Type

/**
 * A context class for the variables used in the [Compiler].
 *
 * @param compilerInstance The instance to operate on. An example would be compiling the statements in a for loop from [ForLoopCompiler].
 * @param currentReturnType The register size of the returned value of a function. The domain is 2^n for 0<=n<=3.
 * @param isInSubOperation Is used to find out weather an operation should push and pop the used registers for the other operation(s).
 * @param operationIndex The depth of the operation
 * @param stackIndex The index of the lowest variable saved on the stack in the current scope.
 * @param lIndex The current amount of sublabels called '.Ln'.
 * @param inIfStatement Required for return statements to know weather to jmp or not
 * @param separateReturnSegment Telling the compiler to create a separate return segment for functions. Used if you return from a if statement etc.
 *
 */
data class CompilerContext(
    var compilerInstance: Compiler,
    var currentReturnType: Type? = null,
    var isInSubOperation: Boolean = false,
    var operationIndex: Int = -1,
    var stackIndex: MutableList<Int> = ArrayList(),
    var lIndex: Int = 0,
    var inIfStatement: Boolean = false,
    var separateReturnSegment: Boolean = false,
    var argumentIndex: Int = 0,
    var operationResultType: Type? = null,
    var variables: MutableList<MutableMap<String, Compiler.LocalVariable>> = ArrayList(),
    var functions: MutableList<Compiler.Function> = ArrayList(),
    var inJmpCondition: Boolean = false) {

   /**
    *
    */
   fun getVariableFromArrayGet(expression: Expression): Compiler.LocalVariable? {
      if (expression is Expression.ArrayGet) {
         return getVariableFromArrayGet(expression.expression)
      } else if (expression is Expression.VarCall) {
         return getVariable(expression.varName.substring)
      }
      return null
   }
    fun getVariable(name: String): Compiler.LocalVariable? {
        return variables.reduce { acc, mutableMap ->
            acc.putAll(mutableMap)
            acc
        }[name]
    }

    fun addFunction(function: Compiler.Function){
        if(getFunction(function.name, function.args.size) != null) {
            Main.error(-1, -1, "The function with the name ${function.name} and ${function.args.size} arguments has previously been defined.")
        } else{
            functions.add(function)
        }
    }
    fun addFunctions(functions: List<Compiler.Function>){
        functions.forEach { addFunction(it) }
    }

    fun getFunction(functionName: String, argumentSize: Int): Compiler.Function? {
        return functions.firstOrNull { functionName == it.name && it.args.values.size == argumentSize }
    }
    private fun beforeAndPointerArrayGet(arrayGet: Expression.ArrayGet): Triple<String, String, Type> {
        val pointer = CompilerUtils.getRegister("ax", operationResultType!!.getRawLength())
        return Triple(arrayGet.accept(compilerInstance), pointer, operationResultType!!)
    }

    fun moveExpressionToX(expression: Expression): Triple<String, String, Type> {
        val accept = expression.accept(compilerInstance)
        return when (expression) {
            is Expression.VarCall -> {
                val localVariable = getVariable(expression.varName.substring)
                if (localVariable == null) {
                    CommonErrorMessages.xNotFound("variable", expression.varName)
                    error("")
                }
                Triple("", accept, localVariable.type)
            }
            is Expression.ArrayGet -> {
                beforeAndPointerArrayGet(expression)
            }
            is Expression.Call -> {
                moveCallToX(expression)
            }
            is Expression.Grouping, is Expression.Operation, is Expression.Comparison -> {
                if(operationResultType == null){
                    Main.error(-1, -1, "The expression does not return a type.")
                }
                Triple(accept, CompilerUtils.getRegister("ax", operationResultType!!.getRawLength()), operationResultType!!)
            }
            is Expression.Literal -> {
                val type = ExpressionUtils.getType(null, expression.value)
                Triple("", accept, type)
            }
            is Expression.PointerGet -> {
                Triple(accept, "rax", operationResultType!!)
            }
            is Expression.ValueFromPointer -> {
                Triple(accept, CompilerUtils.getRegister("ax", operationResultType!!.getRawLength()), operationResultType!!)
            }
            else -> Triple("", "", NormalType("any"))
        }
    }

    private fun moveCallToX(call: Expression.Call): Triple<String, String, Type> {
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
                error("")
            }
            return Triple(
                call.accept(compilerInstance),
                CompilerUtils.getRegister("ax", function.returnType!!.getRawLength()),
                function.returnType!!
            )
        }
        error("Cannot yet compile the requested call")
    }
}