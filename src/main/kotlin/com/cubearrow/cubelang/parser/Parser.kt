package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.main.Main

class Parser(private var tokens: List<Token>, private val expressionSeparator: List<TokenType>) {
    companion object {
        val unidentifiableTokenTypes = listOf(TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.STRING, TokenType.FUN)
    }

    private var current = -1
    private var expressions = ArrayList<Expression>()
    fun parse(): MutableList<Expression> {
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
            if (previousToken != null) {
                return parseExpressionFromSingleToken(previousToken)
            }
            return null
        }

        if (previousToken == null && unidentifiableTokenTypes.contains(currentToken.tokenType)) {
            return nextExpression(currentToken)
        } else if (currentToken.tokenType == TokenType.OPERATOR) {
            if (previousToken != null) parseExpressionFromSingleToken(previousToken)?.let { expressions.add(it) }
            return parseOperation()
        } else if (currentToken.tokenType == TokenType.EQUALS && previousToken != null) {
            return Expression.Assignment(previousToken, nextExpressionUntilEnd() as Expression)
        } else if (currentToken.tokenType == TokenType.BRCKTL && previousToken?.tokenType == TokenType.IDENTIFIER) {
            val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)
            current--
            return Expression.Call(previousToken, args)
        } else if (currentToken.tokenType == TokenType.IDENTIFIER && previousToken?.tokenType == TokenType.FUN) {
            return parseFunctionDefinition(currentToken)
        } else if (currentToken.tokenType == TokenType.COMPARATOR) {
            if (previousToken != null) parseExpressionFromSingleToken(previousToken)?.let { expressions.add(it) }
            return parseComparison()
        } else if (currentToken.tokenType == TokenType.IF) {
            return parseIfStmnt()
        } else if (currentToken.tokenType == TokenType.RETURN) {
            return nextExpressionUntilEnd()?.let { Expression.ReturnStmnt(it) }
        } else if (currentToken.tokenType == TokenType.WHILE) {
            return parseWhileStatement()
        }else if(currentToken.tokenType == TokenType.FOR){
            return parseForLoop()
        }
        Main.error(currentToken.line, currentToken.index, null, "Unexpected token \"${currentToken.substring}\"")
        return null
    }

    private fun parseForLoop(): Expression? {
        current++
        val args = multipleExpressions(TokenType.BRCKTR, TokenType.SEMICOLON)
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        return Expression.ForStmnt(args, body)
    }

    private fun parseWhileStatement(): Expression? {
        current++
        val expression = nextExpression(null)
        current++
        if (tokens[current].tokenType != TokenType.CURLYR) current++
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        return expression?.let { Expression.WhileStmnt(it, body) }
    }

    private fun nextExpressionUntilEnd(): Expression? {
        var result: Expression? = nextExpression(null)
        var temp: Expression? = result
        while (temp != null) {
            result = temp
            expressions.add(temp)
            temp = nextExpression(null)
            expressions.remove(result)
        }
        current--
        return result
    }

    private fun parseFunctionDefinition(currentToken: Token): Expression.FunctionDefinition {
        current++
        val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        return Expression.FunctionDefinition(currentToken, args, body)
    }

    private fun parseIfStmnt(): Expression.IfStmnt? {
        current++
        val expression = nextExpression(null)
        current++
        if (tokens[current].tokenType != TokenType.CURLYR) current++
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        val elseBody: List<Expression> = if (tokens[current].tokenType == TokenType.ELSE) {
            current++
            multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        } else {
            ArrayList()
        }
        return expression?.let { Expression.IfStmnt(it, body, elseBody as MutableList<Expression>) }
    }

    private fun parseComparison(): Expression.Comparison? {
        val (leftExpression, comparator, rightExpression) = parseExpressionsWithMiddleToken()
        return if (rightExpression != null) {
            Expression.Comparison(leftExpression, comparator, rightExpression)
        } else {
            Main.error(comparator.line, comparator.index, null, "Wrongly formatted comparison found.")
            // Unreachable
            null
        }
    }

    private fun parseOperation(): Expression.Operation? {
        val (leftExpression, operator, rightExpression) = parseExpressionsWithMiddleToken()
        return if (rightExpression != null) {
            Expression.Operation(leftExpression, operator, rightExpression)
        } else {
            Main.error(operator.line, operator.index, null, "Wrongly formatted comparison found.")
            // Unreachable
            null
        }
    }

    private fun parseExpressionsWithMiddleToken(): Triple<Expression, Token, Expression?> {
        val leftExpression = this.expressions.removeAt(this.expressions.size - 1)
        if (tokens[current].tokenType != TokenType.OPERATOR)
            current++
        val comparator = tokens[current]
        current++
        val parser = Parser(tokens.subList(current, tokens.size), listOf(TokenType.SEMICOLON, TokenType.BRCKTR, TokenType.CURLYR))
        val rightExpression = parser.nextExpression(null)

        current += parser.current
        return Triple(leftExpression, comparator, rightExpression)
    }

    private fun multipleExpressions(endsAt: TokenType, delimiter: TokenType): MutableList<Expression> {
        val result = ArrayList<Expression>()
        val argsParser = Parser(tokens.subList(current + 1, tokens.size), listOf(endsAt, delimiter))
        while (true) {
            var expression: Expression? = argsParser.nextExpression(null) ?: break
            expression?.let { argsParser.expressions.add(it) }
            argsParser.current++
            var currentToken = argsParser.tokens[argsParser.current]
            // Continue parsing if the expression is not finished
            if (argsParser.tokens[argsParser.current].tokenType != delimiter && argsParser.tokens[argsParser.current].tokenType != endsAt) {
                argsParser.current--
                expression = argsParser.nextExpression(null)
                argsParser.current++
                currentToken = argsParser.tokens[argsParser.current]
            }
            if (expression != null && (currentToken.tokenType == delimiter || currentToken.tokenType == endsAt)) {
                result.add(expression)
            }
            if (currentToken.tokenType == endsAt || argsParser.tokens[argsParser.current + 1].tokenType == endsAt) {
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
        current--
        if (previousToken?.tokenType == TokenType.NUMBER || previousToken?.tokenType == TokenType.STRING) {
            return parseLiteral(previousToken.substring)
        } else if (previousToken?.tokenType == TokenType.IDENTIFIER) {
            return Expression.VarCall(previousToken)
        }
        return null
    }

    private fun parseLiteral(substring: String): Expression.Literal? {
        try {
            return Expression.Literal(substring.toDouble())
        } catch (error: NumberFormatException) {
        }
        return Expression.Literal(substring)
    }
}