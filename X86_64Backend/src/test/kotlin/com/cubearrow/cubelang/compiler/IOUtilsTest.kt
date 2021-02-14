package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.compiler.utils.IOUtils
import org.junit.jupiter.api.Test

class IOUtilsTest {
    @Test
    fun testInputOutput(){
        val text = "Hello World\ntest"
        IOUtils.writeAllLines("src/test/resources/IOTest.txt", text)
        assert(IOUtils.readAllText("src/test/resources/IOTest.txt") == text)
    }
}