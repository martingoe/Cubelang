package com.cubearrow.cubelang.utils

import org.junit.jupiter.api.Test

class IOUtilsTest {
    @Test
    fun testInputOutput(){
        val text = "Hello World\ntest"
        IOUtils.writeAllLines("src/test/resources/IOTest.txt", text)
        assert(IOUtils.readAllText("src/test/resources/IOTest.txt") == text)
    }
}