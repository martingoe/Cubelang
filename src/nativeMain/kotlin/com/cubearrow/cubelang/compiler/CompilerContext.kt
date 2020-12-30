package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.compiler.specificcompilers.ForLoopCompiler
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
    var functions: MutableMap<String, Compiler.Function> = HashMap(),
    var inJmpCondition: Boolean = false
)