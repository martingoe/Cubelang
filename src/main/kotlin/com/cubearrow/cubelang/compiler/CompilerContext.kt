package com.cubearrow.cubelang.compiler

import java.util.*
import kotlin.collections.HashMap

data class CompilerContext(
        var compilerInstance: Compiler,
        var currentReturnLength: Int? = null,
        var isInSubOperation: Boolean = false,
        var operationIndex: Int = -1,
        var stackIndex: Stack<Int> = Stack<Int>(),
        var lIndex: Int = 2,
        var inIfCondition: Boolean = false,
        var separateReturnSegment: Boolean = false,
        var argumentIndex: Int = 0,
        var operationResultSize: Int = 0,
        var variables: Stack<MutableMap<String, Compiler.LocalVariable>> = Stack(),
        var functions: MutableMap<String, Compiler.Function> = HashMap()
)