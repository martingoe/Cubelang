package com.martingoe.cubelang.backend


/**
 * Returns the inverse of the jmp operation requested by the comparison string.
 *
 * e.g: == -> jne
 */
internal fun getInvJumpOperationFromComparator(comparisonString: String): String {
    return when (comparisonString) {
        "==" -> "jne"
        "!=" -> "je"
        "<" -> "jge"
        "<=" -> "jg"
        ">" -> "jle"
        ">=" -> "jl"
        else -> error("Could not find the requested operation")
    }
}

/**
 * Returns the jmp operation appropriate for the comparison string
 *
 * e.g: == -> leq
 */
internal fun getJmpOperationFromComparator(comparisonString: String): String {
    return when (comparisonString) {
        "==" -> "jeq"
        "!=" -> "jne"
        "<" -> "jl"
        "<=" -> "jle"
        ">" -> "jg"
        ">=" -> "jge"
        else -> error("Could not find the requested operation")
    }
}