package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType

class Parser(private var tokens: List<Token>, private val expressionSeparator: List<TokenType>) {
    companion object {
        val unidentifiableTokenTypes = listOf(TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.FUN)
    }

    private var current = -1
    private var expressions = ArrayList<Expression>()
    fun parse() : MutableList<Expression>{
        while (current < tokens.size - 1) {
            val expression = nextExpression(null)
            expression?.let { expressions.add(it) }
        }
        return expressions
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
        } else if (currentToken.tokenType == TokenType.EQUALS && previousToken != null) {
            return Expression.Assignment(previousToken, nextExpression(null) as Expression)
        } else if (currentToken.tokenType == TokenType.BRCKTL && previousToken?.tokenType == TokenType.IDENTIFIER) {
            val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)
            return Expression.Call(previousToken, args)
        } else if(currentToken.tokenType == TokenType.IDENTIFIER && previousToken?.tokenType == TokenType.FUN){
            current++
            val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)
            val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
            return Expression.FunctionDefinition(currentToken, args, body)
        } else if(currentToken.tokenType == TokenType.COMPARATOR){
            if(previousToken != null) parseExpressionFromSingleToken(previousToken)?.let { expressions.add(it) }
            return parseComparison()
        } else if(currentToken.tokenType == TokenType.IF){
            return parseIfStmnt()
        }
        Main.error(currentToken.line, currentToken.index, null, "Unexpected token \"${currentToken.substring}\"")
        return null
    }

    private fun parseIfStmnt(): Expression.IfStmnt? {
        current++
        val expression = nextExpression(null)
        current++
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        val elseBody: List<Expression> = if(tokens[current+1].tokenType == TokenType.ELSE){
            current += 2
            multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        }
        else{
            ArrayList()
        }
        return expression?.let { Expression.IfStmnt(it, body, elseBody as MutableList<Expression>) }
    }

    private fun parseComparison() : Expression.Comparison?{
        val leftExpression = this.expressions.removeAt(this.expressions.size - 1)
        val comparator = tokens[current]
        current++
        val parser = Parser(tokens.subList(current, tokens.size), listOf(TokenType.SEMICOLON, TokenType.BRCKTR, TokenType.CURLYR))
        val rightExpression = parser.nextExpression(null)

        current += parser.current
        return if(rightExpression != null) {
            Expression.Comparison(leftExpression, comparator, rightExpression)
        }
        else{
            Main.error(comparator.line, comparator.index, null, "Wrongly formatted comparison found.")
            // Unreachable
            null
        }
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
            if (currentToken.tokenType == endsAt || argsParser.tokens[argsParser.current +1].tokenType == endsAt) {
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