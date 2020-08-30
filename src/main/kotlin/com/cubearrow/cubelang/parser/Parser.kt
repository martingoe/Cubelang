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

        var value: Expression? = null
        if (previousToken == null && unidentifiableTokenTypes.contains(currentToken.tokenType)) {
            value = nextExpression(currentToken)
        } else if (currentToken.tokenType == TokenType.OPERATOR) {
            if (previousToken != null) parseExpressionFromSingleToken(previousToken)?.let { expressions.add(it);current++ }
            current--
            value = parseOperation()
        } else if (currentToken.tokenType == TokenType.EQUALS && previousToken != null) {
            value = Expression.Assignment(previousToken, nextExpressionUntilEnd() as Expression)
        } else if (currentToken.tokenType == TokenType.BRCKTL && previousToken?.tokenType == TokenType.IDENTIFIER) {
            val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)
            value = Expression.Call(previousToken, args)
        } else if (currentToken.tokenType == TokenType.IDENTIFIER && previousToken?.tokenType == TokenType.FUN) {
            value = parseFunctionDefinition(currentToken)
        } else if (currentToken.tokenType == TokenType.COMPARATOR) {
            if (previousToken != null) parseExpressionFromSingleToken(previousToken)?.let { expressions.add(it) }
            value = parseComparison()
        } else if (currentToken.tokenType == TokenType.IF) {
            value = parseIfStmnt()
        } else if (currentToken.tokenType == TokenType.RETURN) {
            value = nextExpressionUntilEnd()?.let { Expression.ReturnStmnt(it) }
        } else if (currentToken.tokenType == TokenType.WHILE) {
            value = parseWhileStatement()
        } else if (currentToken.tokenType == TokenType.FOR) {
            value = parseForLoop()
        } else if (currentToken.tokenType == TokenType.VAR) {
            value = parseVarInitialization()
        } else if (currentToken.tokenType == TokenType.CLASS) {
            value = parseClass()
        } else if (currentToken.tokenType == TokenType.DOT) {
            if (previousToken != null) {
                parseExpressionFromSingleToken(previousToken)?.let { expressions.add(it) }
                current++
            }
            value = parseGetOrSet()
        }
        value?.let { return it }
        Main.error(currentToken.line, currentToken.index, null, "Unexpected token: \"${currentToken.substring}\"")
        return null
    }

    private fun parseGetOrSet(): Expression? {
        val previous = this.expressions.removeAt(this.expressions.size - 1)
        val expressions = multipleExpressions(listOf(TokenType.BRCKTR, TokenType.SEMICOLON, TokenType.OPERATOR, TokenType.EQUALS) as MutableList<TokenType>, TokenType.DOT)
        expressions.add(0, previous)
        val result: Expression
        if (tokens[current].tokenType == TokenType.EQUALS) {
            current -= 2
            expressions.removeLast()
            nextExpression(null)?.let { expressions.add(it) }
        }

        if (expressions[expressions.size - 1] is Expression.VarCall) current--

        result = if (expressions[expressions.size - 1] is Expression.Assignment) {
            Expression.InstanceSet(expressions[expressions.size - 2], expressions[expressions.size - 1])
        } else {
            Expression.InstanceGet(expressions[expressions.size - 2], expressions[expressions.size - 1])
        }
        var i = expressions.size - 3
        while (i >= 0) {
            if (result is Expression.InstanceGet) {
                result.expression1 = Expression.InstanceGet(expressions[i], expressions[i + 1])
            }
            if (result is Expression.InstanceSet) {
                result.expression1 = Expression.InstanceGet(expressions[i], expressions[i + 1])
            }
            i--

        }
        return result
    }

    private fun parseClass(): Expression? {
        val name = consume(TokenType.IDENTIFIER, "Expected an identifier after 'class'")

        consume(TokenType.CURLYL, "Expected '{' after the class identifier")
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON).filter { it is Expression.VarInitialization || it is Expression.FunctionDefinition }
        return Expression.ClassDefinition(name, body as MutableList<Expression>)
    }

    private fun parseVarInitialization(): Expression? {
        val identifier = consume(TokenType.IDENTIFIER, "Expected an identifier after 'var'")
        consume(TokenType.EQUALS, "Expected '=' after in a variable initialization")
        val expression = nextExpressionUntilEnd()
        return expression?.let { Expression.VarInitialization(identifier, it) }
    }


    private fun parseForLoop(): Expression? {
        consume(TokenType.BRCKTL, "Expected '(' after 'for'.")
        val args = multipleExpressions(TokenType.BRCKTR, TokenType.SEMICOLON)
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        return Expression.ForStmnt(args, body)
    }

    private fun parseWhileStatement(): Expression? {
        consume(TokenType.BRCKTL, "Expected '(' after 'while'.")
        val condition = nextExpression(null)
        consume(TokenType.BRCKTR, "Expected ')' after the condition of a while loop.")
        consume(TokenType.CURLYL, "Expected '{' starting the body of a while loop.")
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        return condition?.let { Expression.WhileStmnt(it, body) }
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

    private fun parseFunctionDefinition(name: Token): Expression.FunctionDefinition {
        consume(TokenType.BRCKTL, "Expected '(' after a function name.")
        val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)

        consume(TokenType.CURLYL, "Expected '{' after the function args.")
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)

        return Expression.FunctionDefinition(name, args, body)
    }

    private fun parseIfStmnt(): Expression.IfStmnt? {
        consume(TokenType.BRCKTL, "Expected '(' after 'if'.")
        val condition = nextExpression(null)
        consume(TokenType.BRCKTR, "Expected '(' after the condition of the if statement.")
        consume(TokenType.CURLYL, "Expected '{' starting the body of the if statement.")
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        val elseBody: List<Expression> = if (tokens[current].tokenType == TokenType.ELSE) {
            consume(TokenType.CURLYL, "Expected '{' starting the else body of the if statement.")
            multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        } else {
            ArrayList()
        }

        return condition?.let { Expression.IfStmnt(it, body, elseBody as MutableList<Expression>) }
    }

    private fun parseComparison(): Expression.Comparison? {
        val (leftExpression, comparator, rightExpression) = parseExpressionsWithMiddleToken(TokenType.COMPARATOR)
        return if (rightExpression != null) {
            Expression.Comparison(leftExpression, comparator, rightExpression)
        } else {
            Main.error(comparator.line, comparator.index, null, "Wrongly formatted comparison found.")
            // Unreachable
            null
        }
    }

    private fun parseOperation(): Expression.Operation? {
        val (leftExpression, operator, rightExpression) = parseExpressionsWithMiddleToken(TokenType.OPERATOR)
        return if (rightExpression != null) {
            Expression.Operation(leftExpression, operator, rightExpression)
        } else {
            Main.error(operator.line, operator.index, null, "Wrongly formatted comparison found.")
            // Unreachable
            null
        }
    }

    private fun parseExpressionsWithMiddleToken(middleToken: TokenType): Triple<Expression, Token, Expression?> {
        val leftExpression = this.expressions.removeAt(this.expressions.size - 1)
        val comparator = consume(middleToken, "Expected a token in the middle which could not be found.")
        val parser = Parser(tokens.subList(++current, tokens.size), listOf(TokenType.SEMICOLON, TokenType.BRCKTR, TokenType.CURLYR))
        val rightExpression = parser.nextExpression(null)

        current += parser.current
        return Triple(leftExpression, comparator, rightExpression)
    }

    private fun multipleExpressions(endsAt: TokenType, delimiter: TokenType): MutableList<Expression> {
        return multipleExpressions(listOf(endsAt) as MutableList<TokenType>, delimiter)
    }

    private fun multipleExpressions(endsAt: MutableList<TokenType>, delimiter: TokenType): MutableList<Expression> {
        val result = ArrayList<Expression>()
        val all: MutableList<TokenType> = ArrayList()
        all.addAll(endsAt)
        all.add(delimiter)
        val argsParser = Parser(tokens.subList(current + 1, tokens.size), all)
        while (!argsParser.peek(endsAt)) {
            var expression = argsParser.nextExpression(null) ?: break
            argsParser.expressions.add(expression)

            // Continue parsing if the expression is not finished
            while (!argsParser.peek(delimiter) && !argsParser.peek(endsAt)) {
                expression = argsParser.nextExpression(null) ?: expression
            }
            result.add(expression)

            if (argsParser.peek(endsAt)) break
            argsParser.consume(delimiter, "Expected the delimiter between expressions.")
        }
        current += argsParser.current + 2

        if (argsParser.expressions.size > 0 && argsParser.expressions.removeLast() is Expression.Call && !endsAt.contains(tokens[current].tokenType)) current--
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

    private fun peek(tokenType: TokenType): Boolean = tokens[current + 1].tokenType == tokenType

    private fun peek(tokenType: List<TokenType>): Boolean = tokenType.contains(tokens[current + 1].tokenType)

    private fun consume(tokenType: TokenType, errorMessage: String): Token {
        this.current++
        if (tokens[current].tokenType != tokenType) {
            Main.error(tokens[current].line, tokens[current].index, null, errorMessage)
        }
        return tokens[current]
    }
}