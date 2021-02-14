package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import org.junit.jupiter.api.Test

class CompilerUtilsTest {
    @Test
    fun testGetRegister() {
        assert(CompilerUtils.getRegister("ax", 8) == "rax")
        assert(CompilerUtils.getRegister("ax", 4) == "eax")
        assert(CompilerUtils.getRegister("ax", 2) == "ah")
        assert(CompilerUtils.getRegister("ax", 1) == "al")

        assert(CompilerUtils.getRegister("10", 1) == "r10b")
        assert(CompilerUtils.getRegister("10", 2) == "r10w")
        assert(CompilerUtils.getRegister("10", 4) == "r10d")
        assert(CompilerUtils.getRegister("10", 8) == "r10")
    }

    @Test
    fun testSplitLengthIntoRegisterLengths() {
        assert(CompilerUtils.splitLengthIntoRegisterLengths(8) == mutableListOf(Pair(8, 1), Pair(4, 0), Pair(2, 0), Pair(1, 0)))
        assert(CompilerUtils.splitLengthIntoRegisterLengths(16) == mutableListOf(Pair(8, 2), Pair(4, 0), Pair(2, 0), Pair(1, 0)))
        assert(CompilerUtils.splitLengthIntoRegisterLengths(45) == mutableListOf(Pair(8, 5), Pair(4, 1), Pair(2, 0), Pair(1, 1)))
    }

    @Test
    fun testGetASMPointerLength() {
        assert(CompilerUtils.getASMPointerLength(8) == "QWORD")
        assert(CompilerUtils.getASMPointerLength(4) == "DWORD")
        assert(CompilerUtils.getASMPointerLength(2) == "WORD")
        assert(CompilerUtils.getASMPointerLength(1) == "BYTE")
    }

    @Test
    fun testGetTokenFromArrayGet() {
        val token = Token("}", TokenType.CURLYR, -1, -1)
        assert(CompilerUtils.getTokenFromArrayGet(Expression.VarCall(token)) == token)
        assert(CompilerUtils.getTokenFromArrayGet(Expression.ArrayGet(Expression.VarCall(token), Expression.Literal(4))) == token)
    }

    @Test
    fun testGetOperationDepth() {
        assert(CompilerUtils.getOperationDepth(Expression.Literal(-1)) == 0)
        assert(
            CompilerUtils.getOperationDepth(
                Expression.Operation(
                    Expression.Literal(-1),
                    Token("+", TokenType.PLUSMINUS, -1, -1),
                    Expression.Literal(-1)
                )
            ) == 1
        )
    }

}