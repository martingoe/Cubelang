package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.utils.NullValue

/**
 * The parser generates a [List] of [Expression] from tokens.
 *
 * @param tokens The tokens to be parsed into a [List] of [Expression]
 * @param expressionSeparator A list of [TokenType] which represent the separators for the individual [Expression]s
 */
class Parser(private var tokens: List<Token>, private val expressionSeparator: List<TokenType>) {
    companion object {
        val unidentifiableTokenTypes = listOf(TokenType.IDENTIFIER, TokenType.DOUBLE, TokenType.STRING, TokenType.FUN, TokenType.NULLVALUE, TokenType.CHAR)
    }

    private var current = -1
    private var expressions = ArrayList<Expression>()

    /**
     * Parses the actual tokens to the needed [List] of [Expression] by moving a pointer along the tokens until the end.
     */
    fun parse(): MutableList<Expression> {
        while (current < tokens.size - 1) {
            val expression = nextExpression(null)
            expression?.let { expressions.add(it) }
        }
        return expressions
    }

    private fun nextExpression(previousToken: Token?, notConsumeSemicolon:Boolean = false): Expression? {
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
            value = nextExpression(currentToken, notConsumeSemicolon)
        } else if (currentToken.tokenType == TokenType.OPERATOR) {
            if (previousToken != null) parseExpressionFromSingleToken(previousToken)?.let { expressions.add(it);current++ }
            current--
            value = parseOperation()
        } else if (currentToken.tokenType == TokenType.EQUALS && previousToken != null) {
            value = parseAssignment(previousToken)
        } else if (currentToken.tokenType == TokenType.BRCKTL && previousToken?.tokenType == TokenType.IDENTIFIER) {
            val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)
            if(peek(TokenType.SEMICOLON) && !notConsumeSemicolon) current++
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
        } else if (currentToken.tokenType == TokenType.COLON && previousToken != null) {
            value = parseArgumentDefinition()
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

    private fun parseArgumentDefinition(): Expression? {
        val name = tokens[current - 1]
        val type = consume(TokenType.IDENTIFIER, "Expected a type after ':' in an argument definition")
        consume(listOf(TokenType.BRCKTR, TokenType.COMMA), "Expected a ')' or a ',' after an argument definition")
        return Expression.ArgumentDefinition(name, type)
    }

    private fun parseAssignment(name: Token): Expression.Assignment {
        val expression = nextExpressionUntilEnd() as Expression
        current++
        return Expression.Assignment(name, expression)
    }

    private fun parseGetOrSet(): Expression? {
        val previous = this.expressions.removeAt(this.expressions.size - 1)
        val expressions = multipleExpressions(listOf(TokenType.BRCKTR, TokenType.SEMICOLON, TokenType.OPERATOR, TokenType.EQUALS) as MutableList<TokenType>, listOf(TokenType.DOT))
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
        val implements = getType()
        consume(TokenType.CURLYL, "Expected '{' after the class identifier")
        val body = multipleExpressions(listOf(TokenType.CURLYR), listOf(TokenType.SEMICOLON, TokenType.CURLYR)).filter { it is Expression.VarInitialization || it is Expression.FunctionDefinition }


        return Expression.ClassDefinition(name, implements, body as MutableList<Expression>)
    }

    private fun getType(): Token? {
        if (peek(TokenType.COLON)) {
            current++
            return consume(TokenType.IDENTIFIER, "Expected an identifier after ':'")
        }
        return null
    }

    private fun parseVarInitialization(): Expression? {
        val identifier = consume(TokenType.IDENTIFIER, "Expected an identifier after 'var'")
        val type = getType()
        var value: Expression? = null
        if (peek(TokenType.EQUALS)) {
            current++
            value = nextExpressionUntilEnd()
        }
        consume(TokenType.SEMICOLON, "Expected an ';'")
        return Expression.VarInitialization(identifier, type, value)
    }


    private fun parseForLoop(): Expression? {
        consume(TokenType.BRCKTL, "Expected '(' after 'for'.")
        val args = multipleExpressions(TokenType.BRCKTR, TokenType.SEMICOLON)
        consume(TokenType.CURLYL, "Expected '{' starting the body of a for loop")
        val body = multipleExpressions(listOf(TokenType.CURLYR), listOf(TokenType.SEMICOLON, TokenType.CURLYR))
        consume(TokenType.CURLYR, "Expected '}' after the for loop body.")
        return Expression.ForStmnt(args, body)
    }

    private fun parseWhileStatement(): Expression? {
        consume(TokenType.BRCKTL, "Expected '(' after 'while'.")
        val condition = nextExpression(null)
        consume(TokenType.BRCKTR, "Expected ')' after the condition of a while loop.")
        consume(TokenType.CURLYL, "Expected '{' starting the body of a while loop.")
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        consume(TokenType.CURLYR, "Expected '}' after the while loop body.")
        return condition?.let { Expression.WhileStmnt(it, body) }
    }

    private fun nextExpressionUntilEnd(): Expression? {
        var result: Expression? = nextExpression(null, true)
        var temp: Expression? = result
        while (temp != null) {
            result = temp
            expressions.add(temp)
            temp = nextExpression(null, true)
            expressions.remove(result)
        }
        current--
        return result
    }

    private fun parseFunctionDefinition(name: Token): Expression.FunctionDefinition {
        consume(TokenType.BRCKTL, "Expected '(' after a function name.")
        val args = multipleExpressions(TokenType.BRCKTR, TokenType.COMMA)

        val type = getType()
        consume(TokenType.CURLYL, "Expected '{' after the function args.")
        val body = multipleExpressions(listOf(TokenType.CURLYR), listOf(TokenType.SEMICOLON, TokenType.CURLYR))

        return Expression.FunctionDefinition(name, args, type, body)
    }

    private fun parseIfStmnt(): Expression.IfStmnt? {
        consume(TokenType.BRCKTL, "Expected '(' after 'if'.")
        val condition = nextExpression(null)
        consume(TokenType.BRCKTR, "Expected '(' after the condition of the if statement.")
        consume(TokenType.CURLYL, "Expected '{' starting the body of the if statement.")
        val body = multipleExpressions(TokenType.CURLYR, TokenType.SEMICOLON)
        consume(TokenType.CURLYR, "Expected '}' closing the if statement")
        val elseBody: List<Expression> = if (peek(TokenType.ELSE)) {
            current++
            consume(TokenType.CURLYL, "Expected '{' starting the else body of the if statement.")
            multipleExpressions(listOf(TokenType.CURLYR), listOf(TokenType.SEMICOLON, TokenType.CURLYR))
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
        return multipleExpressions(listOf(endsAt), listOf(delimiter))
    }

    private fun multipleExpressions(endsAt: List<TokenType>, delimiters: List<TokenType>): MutableList<Expression> {
        val result = ArrayList<Expression>()
        val all: MutableList<TokenType> = ArrayList()
        all.addAll(endsAt)
        all.addAll(delimiters)
        val argsParser = Parser(tokens.subList(current + 1, tokens.size), all)

        while (!argsParser.peek(endsAt)) {
            var expression = argsParser.nextExpression(null) ?: break
            argsParser.expressions.add(expression)

            // Continue parsing if the expression is not finished
            while (!delimiters.contains(argsParser.tokens[argsParser.current].tokenType) && !endsAt.contains(argsParser.tokens[argsParser.current].tokenType)) {
                expression = argsParser.nextExpression(null) ?: expression
            }
            result.add(expression)

            val tokenType = argsParser.tokens[argsParser.current].tokenType
            if (endsAt.contains(tokenType) && !delimiters.contains(tokenType)) {
                break
            }
        }
        current += argsParser.current + 1

        return result
    }

    private fun parseExpressionFromSingleToken(previousToken: Token): Expression? {
        current--
        if (previousToken.tokenType == TokenType.IDENTIFIER) {
            return Expression.VarCall(previousToken)
        }
        return parseLiteral(previousToken)
    }

    private fun parseLiteral(token: Token): Expression.Literal? {
        if (token.tokenType == TokenType.DOUBLE) {
            if (!token.substring.contains(".")) {
                return Expression.Literal(token.substring.toInt())
            }
            return Expression.Literal(token.substring.toDouble())
        }
        if(token.tokenType == TokenType.CHAR){
            return Expression.Literal(token.substring[0])
        }
        return when (token.tokenType) {
            TokenType.STRING -> Expression.Literal(token.substring)

            TokenType.NULLVALUE -> Expression.Literal(NullValue())
            else -> null
        }
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
    private fun consume(tokenType: List<TokenType>, errorMessage: String): Token {
        this.current++
        if (!tokenType.contains(this.tokens[current].tokenType)) {
            Main.error(tokens[current].line, tokens[current].index, null, errorMessage)
        }
        return tokens[current]
    }
}