package com.martingoe.cubelang.test.frontend.lexing

import com.martingoe.cubelang.common.tokens.Token
import com.martingoe.cubelang.common.tokens.TokenType
import com.martingoe.cubelang.frontend.lexing.Lexer
import org.junit.Test


class LexerTest {
    @Test
    fun testTokenSequence() {
        val tokenSequence = Lexer("a = call(1);")

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
    @Test
    fun testChar(){
        assert(
            Token("a", TokenType.CHAR, 1, 1) ==
                Lexer("'a'").tokenSequence[0])
    }


}
