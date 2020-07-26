package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType

class Parser(private var tokens: List<Token>, private val expressionSeparator: List<TokenType>) {
    companion object {
        val unidentifiableTokenTypes = listOf(TokenType.IDENTIFIER, TokenType.NUMBER)
    }

    private var current = -1
    fun parse() {
        while (current < tokens.size - 1) {
            val x = nextExpression(null)
            println(x)
        }
    }

    private fun nextExpression(previousToken: Token?): Expression? {
        current++
        val currentToken = tokens[current]
        if (expressionSeparator.contains(currentToken.tokenType)) {
            return parseExpressionFromSingleToken(previousToken)
        }

        if (previousToken == null && unidentifiableTokenTypes.contains(currentToken.tokenType)) {
            return nextExpression(currentToken)
        } else if (currentToken.tokenType == TokenType.OPERATOR && previousToken != null) {
            return Expression.Operation(parseExpressionFromSingleToken(previousToken)!!, currentToken, nextExpression(null)!!)
        } else if (currentToken.tokenType == TokenType.EQUALS && tokens[current + 1].tokenType != TokenType.EQUALS && previousToken != null) {
            return Expression.Assignment(previousToken, nextExpression(null) as Expression)
        } else if (currentToken.tokenType == TokenType.BRCKTL && previousToken?.tokenType == TokenType.IDENTIFIER) {
            val args = multipleExpressions(TokenType.BRCKTR, TokenType.KOMMA)
            return Expression.Call(previousToken, args)
        }
        Main.error(currentToken.line, currentToken.index, null, "Unexpected token \"${currentToken.substring}\"")
        return null
    }

    private fun multipleExpressions(endsAt: TokenType, delimiter: TokenType): MutableList<Expression> {
        val result = ArrayList<Expression>()
        val argsParser = Parser(tokens.subList(current + 1, tokens.size), listOf(endsAt, delimiter))
        while (true) {
            val expression = argsParser.nextExpression(null)
            val currentToken = argsParser.tokens[argsParser.current]
            if (expression != null) {
                result.add(expression)
            }
            if (currentToken.tokenType == endsAt) {
                break
            }
            if (currentToken.tokenType != delimiter) {
                Main.error(currentToken.line, currentToken.index, null, "Unexpected token \"${currentToken.substring}\"")
                break
            }
        }
        current += argsParser.current + 2
        return result
    }

    private fun parseExpressionFromSingleToken(previousToken: Token?): Expression? {
        if (previousToken?.tokenType == TokenType.NUMBER) {
            return Expression.Literal(previousToken)
        } else if (previousToken?.tokenType == TokenType.IDENTIFIER) {
            return Expression.VarCall(previousToken)
        }
        return null
    }
}