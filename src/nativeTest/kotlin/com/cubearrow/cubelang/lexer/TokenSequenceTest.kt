package com.cubearrow.cubelang.lexer

import kotlin.test.Test

class TokenSequenceTest {
    @Test
    internal fun testTokenSequence() {
        val tokenSequence = TokenSequence("a = call(1);")

        val expected = listOf(
            Token("a", TokenType.IDENTIFIER, 1, 1),
            Token("=", TokenType.EQUALS, 1, 2),
            Token("call", TokenType.IDENTIFIER, 1, 5),
            Token("(", TokenType.BRCKTL, 1, 8),
            Token("1", TokenType.NUMBER, 1, 9),
            Token(")", TokenType.BRCKTR, 1, 10),
            Token(";", TokenType.SEMICOLON, 1, 11),
            Token("", TokenType.EOF, 1, 13)
        )

        assert(tokenSequence.tokenSequence == expected)
    }


}
