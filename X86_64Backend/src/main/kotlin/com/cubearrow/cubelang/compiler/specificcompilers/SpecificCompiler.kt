package com.cubearrow.cubelang.compiler.specificcompilers

interface SpecificCompiler<T> {
    fun accept(expression: T): String
}