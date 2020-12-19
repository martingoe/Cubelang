package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.compiler.NormalType
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType
import kotlin.test.Test

class ParserTest {
    @Test
    internal fun testAssignment() {
        val token = Token("a", TokenType.IDENTIFIER, 1, 1)
        val tokens = listOf(
            token,
            Token("=", TokenType.EQUALS, 1, 2),
            Token("2", TokenType.DOUBLE, 1, 3),
            Token(";", TokenType.SEMICOLON, 1, 4),
            Token("", TokenType.EOF, 1, 6)
        )
        val actual = Parser(tokens).parse()[0] as Expression.Assignment
        assert((actual.expression as Expression.Literal).any == 2)
        assert(actual.identifier == token)
    }

    @Test
    internal fun testVarInitialization() {
        val nameToken = Token("a", TokenType.IDENTIFIER, 1, 4)
        val typeToken = Token("int", TokenType.IDENTIFIER, 1, 6)
        val tokens = listOf(
            Token("var", TokenType.VAR, 1, 1),
            nameToken,
            Token(":", TokenType.COLON, 1, 5),
            typeToken,
            Token("=", TokenType.EQUALS, 1, 7),
            Token("2", TokenType.DOUBLE, 1, 8),
            Token(";", TokenType.SEMICOLON, 1, 9),
            Token("", TokenType.EOF, 1, 0)
        )
        val actual = Parser(tokens).parse()[0] as Expression.VarInitialization
        assert((actual.expressionNull as Expression.Literal).any == 2)

        assert(actual.typeNull!! == NormalType(typeToken.substring))
        assert(actual.identifier == nameToken)
    }

    @Test
    internal fun varCall() {
        val token = Token("a", TokenType.IDENTIFIER, 1, 1)
        val tokens = listOf(
            token,
            Token(";", TokenType.SEMICOLON, 1, 2),
            Token("", TokenType.EOF, 1, 4)
        )
        val actual = Parser(tokens).parse()[0] as Expression.VarCall
        assert(actual.identifier == token)
    }

    @Test
    internal fun comparison() {
        val comparator = Token("<", TokenType.COMPARATOR, 1, 2)
        val first = Token("a", TokenType.IDENTIFIER, 1, 1)
        val second = Token("2", TokenType.DOUBLE, 1, 3)
        val tokens = listOf(
            first,
            comparator,
            second,
            Token(";", TokenType.SEMICOLON, 1, 4),
            Token("", TokenType.EOF, 1, 6)
        )
        val actual = Parser(tokens).parse()[0] as Expression.Comparison
        assert((actual.expression2 as Expression.Literal).any == 2)

        assert((actual.expression as Expression.VarCall).identifier == first)
        assert(actual.comparator == comparator)
    }

    @Test
    internal fun block(){
        val varCallToken = Token("a", TokenType.IDENTIFIER, 1, 2)
        val tokens = listOf(
            Token("{", TokenType.CURLYL, 1, 1),
            varCallToken,
            Token(";", TokenType.SEMICOLON, 1, 3),
            Token("}", TokenType.CURLYR, 1, 4),
            Token("", TokenType.EOF, 1, 5)
        )
        val actual = Parser(tokens).parse()[0] as Expression.BlockStatement
        assert((actual.expressionLst[0] as Expression.VarCall).identifier == varCallToken)
    }
}