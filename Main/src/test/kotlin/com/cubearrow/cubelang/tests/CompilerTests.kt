package com.cubearrow.cubelang.tests

import org.junit.jupiter.api.Test

class CompilerTests {
    @Test
    fun fibonacciTest() {
        val (s, s1) = getResultOfTest("fibonacci")

        assert(s!! == s1!!)
    }

    @Test
    fun forLoopSumTest() {
        val (s, s1) = getResultOfTest("forLoopSum")
        assert(s!! == s1!!)
    }

    @Test
    fun pointerDereferencationTest() {
        val (s, s1) = getResultOfTest("pointerDereferencation")
        assert(s!! == s1!!)
    }

    @Test
    fun euclideanTest() {
        val (s, s1) = getResultOfTest("euclidean")
        assert(s!! == s1!!)
    }

    @Test
    fun stackArgumentsTest() {
        val (s, s1) = getResultOfTest("stackArguments")
        assert(s!! == s1!!)
    }

    @Test
    fun structTest() {
        val (s, s1) = getResultOfTest("struct")
        assert(s!! == s1!!)
    }

    @Test
    fun array2dTest() {
        val (s, s1) = getResultOfTest("2darray")
        assert(s!! == s1!!)
    }

    @Test
    fun arrayToPointerTest() {
        val (s, s1) = getResultOfTest("arrayToPointer")
        assert(s!! == s1!!)
    }

    @Test
    fun arrayToPointerImplicitConversionTest() {
        val (s, s1) = getResultOfTest("arrayToPointerImplicitConversion")
        assert(s!! == s1!!)
    }
}