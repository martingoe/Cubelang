package com.cubearrow.cubelang.lexer

import com.cubearrow.cubelang.utils.IOUtils.Companion.readAllText
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import kotlin.test.Test

class TokenSequenceTest {
    @Test
    internal fun testTokenSequence() {
        val tokenGrammar = TokenGrammar(readAllText("src/nativeMain/resources/TokenGrammar.bnf"))
        val tokenSequence = TokenSequence("a = call(1);", tokenGrammar)

        val expected = listOf(Token("a", TokenType.IDENTIFIER, 1, 1),
                Token("=", TokenType.EQUALS, 1, 2),
                Token("call", TokenType.IDENTIFIER, 1, 5),
                Token("(", TokenType.BRCKTL, 1, 8),
                Token("1", TokenType.DOUBLE, 1, 9),
                Token(")", TokenType.BRCKTR, 1, 10),
                Token(";", TokenType.SEMICOLON, 1, 11),
                Token("", TokenType.EOF, 1, 13))

        assert(tokenSequence.tokenSequence == expected)
    }




}
