package com.cubearrow.cubelang.tests

import org.junit.jupiter.api.Test

class CompilerTests {
    @Test
    fun fibonacciTest(){
        assert(getResultOfTest("fibonacci")!! == "832040\n")
    }
    @Test
    fun forLoopSumTest(){
        assert(getResultOfTest("forLoopSum")!! == "55\n")
    }
    @Test
    fun pointerDereferencationTest(){
        assert(getResultOfTest("pointerDereferencation")!! == "10\n")
    }
    @Test
    fun euclideanTest(){
        assert(getResultOfTest("euclidean")!! == "6\n")
    }
    @Test
    fun stackArgumentsTest(){
        assert(getResultOfTest("stackArguments")!! == "98\n")
    }
    @Test
    fun structTest(){
        assert(getResultOfTest("struct")!! == "10\n")
    }
}