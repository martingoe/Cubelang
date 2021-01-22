package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.utils.Type

data class MoveInformation(
    /**
     * The assembly string needed to set up the value of the expression.
     */
    val before: String,
    /**
     * The pointer used to actually move to eg. a register.
     */
    val pointer: String,
    /**
     * The type of the expression.
     */
    val type: Type){
    /**
     * Creates the string to move to a pointer.
     *
     * @param toMoveTo The pointer to move to.
     */
    fun moveTo(toMoveTo: String): String {
        return (if (before.isNotBlank()) "$before\n" else "") + (if(toMoveTo != pointer) "mov $toMoveTo, $pointer\n" else "")
    }
}