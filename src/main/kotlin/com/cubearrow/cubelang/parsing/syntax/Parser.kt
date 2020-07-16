package com.cubearrow.cubelang.parsing.syntax

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parsing.tokenization.Token
import com.cubearrow.cubelang.parsing.tokenization.TokenType

class Parser(var tokens: List<Token>) {
    companion object {
        val unidentifiableTokenTypes = listOf(TokenType.IDENTIFIER, TokenType.NUMBER)
    }

    private var current = -1
    fun parse() {
        while (current < tokens.size - 1) {
            var x = nextExpression(null)
            println(x)
        }
    }

    private fun nextExpression(previousToken: Token?): Expression? {
        current++
        val currentToken = tokens[current]
        if (currentToken.tokenType == TokenType.SEMICOLON && previousToken != null) {
            return parseExpressionFromSingleToken(previousToken)
        }

        if (previousToken == null && unidentifiableTokenTypes.contains(currentToken.tokenType)) {
            return nextExpression(currentToken)
        } else if (currentToken.tokenType == TokenType.OPERATOR && previousToken != null) {
            return Expression.Operation(parseExpressionFromSingleToken(previousToken)!!, currentToken, nextExpression(null)!!)
        } else if (currentToken.tokenType == TokenType.EQUALS && tokens[current + 1].tokenType != TokenType.EQUALS && previousToken != null) {
            return Expression.Assignment(previousToken, nextExpression(null) as Expression)
        }
        Main.error(currentToken.line, currentToken.index, null, "Unexpected token \"${currentToken.substring}\"")
        return null
    }

    private fun parseExpressionFromSingleToken(previousToken: Token): Expression? {
        if (previousToken.tokenType.equals(TokenType.NUMBER)) {
            return Expression.Literal(previousToken)
        }
        return null
    }
}