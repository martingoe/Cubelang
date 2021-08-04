package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.NormalTypes
import com.cubearrow.cubelang.common.errors.ErrorManager
import org.junit.jupiter.api.Test

class ParserTest {
    @Test
    internal fun testAssignment() {
        val token = Token("a", TokenType.IDENTIFIER, 1, 1)
        val tokens = listOf(
            token,
            Token("=", TokenType.EQUALS, 1, 2),
            Token("2", TokenType.NUMBER, 1, 3),
            Token(";", TokenType.SEMICOLON, 1, 4),
            Token("", TokenType.EOF, 1, 6)
        )
        val actual = Parser(tokens, ErrorManager(listOf("a=2;"), false)).parse()[0] as Expression.Assignment
        assert((actual.valueExpression as Expression.Literal).value == 2)
        assert(actual.name == token)
    }

    @Test
    internal fun testVarInitialization() {
        val nameToken = Token("a", TokenType.IDENTIFIER, 1, 4)
        val typeToken = Token("i32", TokenType.IDENTIFIER, 1, 6)
        val tokens = listOf(
            Token("var", TokenType.VAR, 1, 1),
            nameToken,
            Token(":", TokenType.COLON, 1, 5),
            typeToken,
            Token("=", TokenType.EQUALS, 1, 7),
            Token("2", TokenType.NUMBER, 1, 8),
            Token(";", TokenType.SEMICOLON, 1, 9),
            Token("", TokenType.EOF, 1, 0)
        )
        val actual = Parser(tokens, ErrorManager(listOf("a=2;"), false)).parse()[0] as Expression.VarInitialization
        assert((actual.valueExpression as Expression.Literal).value == 2)

        assert(actual.type == NormalType(NormalTypes.I32))
        assert(actual.name == nameToken)
    }

    @Test
    internal fun varCall() {
        val token = Token("a", TokenType.IDENTIFIER, 1, 1)
        val tokens = listOf(
            token,
            Token(";", TokenType.SEMICOLON, 1, 2),
            Token("", TokenType.EOF, 1, 4)
        )
        val actual = Parser(tokens, ErrorManager(listOf("a;"), false)).parse()[0] as Expression.VarCall
        assert(actual.varName == token)
    }

    @Test
    internal fun comparison() {
        val comparator = Token("<", TokenType.COMPARATOR, 1, 2)
        val first = Token("a", TokenType.IDENTIFIER, 1, 1)
        val second = Token("2", TokenType.NUMBER, 1, 3)
        val tokens = listOf(
            first,
            comparator,
            second,
            Token(";", TokenType.SEMICOLON, 1, 4),
            Token("", TokenType.EOF, 1, 6)
        )
        val actual = Parser(tokens, ErrorManager(listOf("a<2;"), false)).parse()[0] as Expression.Comparison
        assert((actual.rightExpression as Expression.Literal).value == 2)

        assert((actual.leftExpression as Expression.VarCall).varName == first)
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
        val actual = Parser(tokens, ErrorManager(listOf("{a};"), false)).parse()[0] as Expression.BlockStatement
        assert((actual.statements[0] as Expression.VarCall).varName == varCallToken)
    }
}