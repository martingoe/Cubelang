package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.utils.ArrayType
import com.cubearrow.cubelang.utils.NormalType
import com.cubearrow.cubelang.utils.Type
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType
import kotlin.system.exitProcess

class Parser(private var tokens: List<Token>) {
    private var current = -1

    fun parse(): List<Expression> {
        val statements: MutableList<Expression> = ArrayList()

        while (!isAtEnd()) {
            if(peek(TokenType.EOF)) break
            statements.add(declaration())
        }
        return statements
    }

    private fun statement(): Expression {
        return when (advance().tokenType) {
            TokenType.IMPORT -> importStatement()
            TokenType.VAR -> variableDefinition()
            TokenType.IF -> ifStatement()
            TokenType.WHILE -> whileStatement()
            TokenType.FOR -> forStatement()
            TokenType.CURLYL -> blockStatement()
            TokenType.CLASS -> classStatement()
            TokenType.FUN -> functionStatement()
            TokenType.RETURN -> returnStatement()
            else -> {
                current--
                return expressionStatement()
            }
        }
    }

    private fun importStatement(): Expression {
        val name = consume(TokenType.STRING, "Expected a string after the import keyword.")
        consume(TokenType.SEMICOLON, "Expected a ; after the import statement")
        return Expression.ImportStmnt(name)
    }

    private fun expressionStatement(): Expression {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected a ';' after a expression statement")
        return value
    }

    private fun returnStatement(): Expression {
        var value: Expression? = null
        if (!peek(TokenType.SEMICOLON))
            value = expression()
        consume(TokenType.SEMICOLON, "Expected a ';' after the return statement")
        return Expression.ReturnStmnt(value)
    }

    private fun functionStatement(): Expression {
        val name = consume(TokenType.IDENTIFIER, "Expected an identifier for the function name")
        consume(TokenType.BRCKTL, "Expected '(' after a function name.")

        val args: MutableList<Expression> = ArrayList()
        if (!peek(TokenType.BRCKTR)) {
            while (true) {
                val parameterName = consume(TokenType.IDENTIFIER, "Expected a parameter name")
                val type = getType() ?: throw ParseException("Expected a type for the parameter", previous())
                args.add(Expression.ArgumentDefinition(parameterName, type))

                if (!match(TokenType.COMMA)) break
            }
        }
        consume(TokenType.BRCKTR, "Expected a ')' closing the argument definitions")
        val type = getType()

        return Expression.FunctionDefinition(name, args, type, statement())
    }

    private fun classStatement(): Expression {
        val name = consume(TokenType.IDENTIFIER, "Expected an identifier as the class name")
        val implements = getType()
        consume(TokenType.BRCKTL, "Expected '{' starting the class body")
        val methods: MutableList<Expression> = ArrayList()
        while (!peek(TokenType.BRCKTR) && !isAtEnd()) {
            when {
                match(TokenType.FUN) -> methods.add(functionStatement())
                match(TokenType.VAR) -> methods.add(variableDefinition())
                else -> throw ParseException("Wrong statement type for a class", previous())
            }
        }
        return Expression.ClassDefinition(name, implements, methods)
    }

    private fun blockStatement(): Expression {
        val statements: MutableList<Expression> = ArrayList()

        while (!peek(TokenType.CURLYR) && !isAtEnd())
            statements.add(declaration())

        consume(TokenType.CURLYR, "Expected a '}' closing the code block")
        return Expression.BlockStatement(statements)
    }

    private fun forStatement(): Expression {
        consume(TokenType.BRCKTL, "Expected a '(' after 'for'")

        val init = when {
            match(TokenType.SEMICOLON) -> Expression.Empty(null)
            match(TokenType.VAR) -> variableDefinition()
            else -> throw ParseException("Expected a variable definition or null in the initialization of a for loop", previous())
        }
        val condition = if (match(TokenType.SEMICOLON)) Expression.Literal(true) else orExpression()
        consume(TokenType.SEMICOLON, "Expected a ';' after the condition of the for loop")
        val incrementor = if (peek(TokenType.BRCKTR)) Expression.Empty(null) else expression()
        consume(TokenType.BRCKTR, "expect ')' after clauses")
        return Expression.ForStmnt(listOf(init, condition, incrementor), statement())
    }

    private fun whileStatement(): Expression {
        consume(TokenType.BRCKTL, "Expected a '(' after 'while'")
        val condition = orExpression()
        consume(TokenType.BRCKTR, "Expected a ')' closing the condition")
        return Expression.WhileStmnt(condition, statement())
    }

    private fun ifStatement(): Expression {
        consume(TokenType.BRCKTL, "Expected a '(' after 'if'")
        val condition = orExpression()
        consume(TokenType.BRCKTR, "Expected a ')' closing the condition")
        val thenBranch = statement()
        var elseBranch: Expression? = null
        if (match(TokenType.ELSE))
            elseBranch = statement()
        return Expression.IfStmnt(condition, thenBranch, elseBranch)
    }

    private fun variableDefinition(): Expression {
        val name = consume(TokenType.IDENTIFIER, "Expected a name for the variable")
        val type = getType()
        val value = if (match(TokenType.EQUALS)) expression() else null
        consume(TokenType.SEMICOLON, "Expected a ';' after the variable definition.")
        return Expression.VarInitialization(name, type, value)

    }

    private fun assignment(): Expression {
        val expression = orExpression()

        if (match(TokenType.EQUALS)) {
            val equals = previous()
            val value = assignment()
            return when (expression) {
                is Expression.VarCall -> {
                    Expression.Assignment(expression.varName, value)
                }
                is Expression.InstanceGet -> {
                    Expression.InstanceSet(expression.expression, expression.identifier, value)
                }
                is Expression.ArrayGet -> {
                    Expression.ArraySet(expression, value)
                }
                else -> {
                    throw ParseException("Invalid assignment target", equals)
                }
            }
        }
        return expression
    }

    private fun orExpression(): Expression {
        var expression = andExpression()
        while (match(TokenType.OR))
            expression = Expression.Logical(expression, previous(), andExpression())
        return expression
    }

    private fun previous(): Token = tokens[current - 1]


    private fun andExpression(): Expression {
        var expression = equality()
        while (match(TokenType.AND))
            expression = Expression.Logical(expression, previous(), equality())
        return expression
    }

    private fun equality(): Expression {
        var expression = comparison()
        while (match(TokenType.EQUALITY))
            expression = Expression.Comparison(expression, current(), comparison())
        return expression
    }

    private fun comparison(): Expression {
        var expression = addition()
        while (match(TokenType.COMPARATOR))
            expression = Expression.Comparison(expression, current(), addition())
        return expression
    }

    private fun addition(): Expression {
        var expression = multiplication()
        while (match(TokenType.PLUSMINUS))
            expression = Expression.Operation(expression, current(), multiplication())
        return expression
    }

    private fun multiplication(): Expression {
        var expression = unary()
        while (match(TokenType.OPERATOR))
            expression = Expression.Operation(expression, current(), unary())
        return expression
    }

    private fun unary(): Expression {
        while (match(listOf(TokenType.BANG, TokenType.PLUSMINUS)))
            return Expression.Unary(previous(), unary())
        return call()
    }

    private fun call(): Expression {
        var expression = primary()

        while (true) {
            expression = if (match(TokenType.BRCKTL)) {
                finishCall(expression)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expected an identifier after '.'")
                Expression.InstanceGet(expression, name)
            } else {
                break
            }
        }
        return expression
    }

    private fun finishCall(callee: Expression): Expression {
        val arguments: MutableList<Expression> = ArrayList()

        while (!peek(TokenType.BRCKTR)) {
            arguments.add(expression())
            while (match(TokenType.COMMA))
                arguments.add(expression())
        }
        consume(TokenType.BRCKTR, "Expected a ')' closing the call")
        return Expression.Call(callee, arguments)
    }

    private fun primary(): Expression {
        return when (advance().tokenType) {
            TokenType.NULLVALUE -> Expression.Literal(null)
            TokenType.STRING -> Expression.Literal(current())
            TokenType.IDENTIFIER -> {
                var result: Expression = Expression.VarCall(current())
                while(match(TokenType.CLOSEDL)) {
                    val number = expression()
                    consume(TokenType.CLOSEDR, "Expected ']'")
                    result = Expression.ArrayGet(result, number)
                }
                result
            }
            TokenType.CHAR -> Expression.Literal(current().substring[0])
            TokenType.NUMBER -> {
                return if (!current().substring.contains(".")) {
                    Expression.Literal(current().substring.toInt())
                } else {
                    Expression.Literal(current().substring.toDouble())
                }
            }
            TokenType.BRCKTL -> {
                val expression = expression()
                consume(TokenType.BRCKTR, "Expected a closing ')'")
                Expression.Grouping(expression)
            }
            else -> throw ParseException("Expected an expression", previous())

        }
    }

    private fun declaration(): Expression{
        try{
            if(match(TokenType.VAR))
                return variableDefinition()
            return statement()
        } catch (error: ParseException) {
            synchronize()
            error.printStackTrace()
            exitProcess(0)
        }
    }

    private fun synchronize(){
        advance()
        val list = listOf(TokenType.IF, TokenType.VAR, TokenType.FOR, TokenType.WHILE, TokenType.CLASS, TokenType.RETURN)
        while (!isAtEnd()){
            if(previous().tokenType == TokenType.SEMICOLON)
                return
            if(list.contains(peek().tokenType))
                return
            advance()
        }
    }

    private fun expression(): Expression {
        return assignment()
    }

    private fun advance(): Token = tokens[++current]
    private fun current(): Token = tokens[current]

    private fun match(tokenTypes: List<TokenType>): Boolean {
        for (tokenType in tokenTypes) {
            if (match(tokenType)) return true
        }
        return false
    }
    private fun peek(): Token = tokens[current + 1]

    private fun match(tokenType: TokenType): Boolean {
        if (peek(tokenType)) {
            current++
            return true
        }
        return false
    }

    private fun getType(): Type? {
        if (peek(TokenType.COLON)) {
            current++
            return type()
        }
        return null
    }
    private fun type(): Type {
        return if(peek(TokenType.IDENTIFIER))
            NormalType(consume(TokenType.IDENTIFIER, "Expected a type identifier after ':'").substring)
        else{
            consume(TokenType.CLOSEDL, "Expected '['.")
            val name = type()
            consume(TokenType.COLON, "Expected an ':' in the argument type.")
            val amount = consume(TokenType.NUMBER, "Expected an amount in the array type.").substring.toInt()
            consume(TokenType.CLOSEDR, "Expected ']' closing the array definition.")
            ArrayType(name, amount)
        }
    }

    private fun isAtEnd(): Boolean = current >= tokens.size && !peek(TokenType.EOF)

    private fun peek(tokenType: TokenType): Boolean = tokens[current + 1].tokenType == tokenType

    private fun consume(tokenType: TokenType, errorMessage: String): Token {
        this.current++
        if (tokens[current].tokenType != tokenType) {
            throw ParseException(errorMessage, tokens[current])
        }
        return tokens[current]
    }

}

class ParseException(override var message: String, var token: Token) : Throwable()

