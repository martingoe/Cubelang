package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType
import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.errors.ErrorManager
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList


/**
 * This class creates a [List] of [Expression]s that form an abstract syntax tree.
 */
class Parser(private var tokens: List<Token>, private var errorManager: ErrorManager) {
    private var current = -1

    fun parse(): List<Expression> {
        val statements: MutableList<Expression> = ArrayList()

        while (!isAtEnd()) {
            if(peek(TokenType.EOF)) break
            statements.add(statement())
        }
        return statements
    }

    private fun statement(): Expression {
        try {
            return when (advance().tokenType) {
                TokenType.IMPORT -> importStatement()
                TokenType.VAR -> variableDefinition()
                TokenType.IF -> ifStatement()
                TokenType.WHILE -> whileStatement()
                TokenType.FOR -> forStatement()
                TokenType.CURLYL -> blockStatement()
                TokenType.STRUCT -> structDefinition()
                TokenType.FUN -> functionStatement()
                TokenType.RETURN -> returnStatement()
                else -> {
                    current--
                    return expressionStatement()
                }
            }
        } catch (error: ParseException){
            catchException(error.message, error.token)
        }
        return Expression.Empty(null)
    }

    private fun importStatement(): Expression {
        val name = advance()
        consume(TokenType.SEMICOLON, "Expected a ; after the import statement")
        return Expression.ImportStmnt(name)
    }

    private fun expressionStatement(): Expression {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected a ';' after an expression statement")
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
                val type = getType()
                if(type is NoneType)
                    errorManager.error(current().line, current().index, "Expected an argument type")
                args.add(Expression.ArgumentDefinition(parameterName, type))

                if (!match(TokenType.COMMA)) break
            }
        }
        consume(TokenType.BRCKTR, "Expected a ')' closing the argument definitions")
        val type = getType()

        return Expression.FunctionDefinition(name, args, type, statement())
    }

    private fun structDefinition(): Expression {
        val name = consume(TokenType.IDENTIFIER, "Expected an identifier as the class name")
        consume(TokenType.CURLYL, "Expected '{' starting the class body")
        val methods: MutableList<Expression.VarInitialization> = ArrayList()
        while (!peek(TokenType.CURLYR) && !isAtEnd()) {
            when {
                match(TokenType.VAR) -> {
                    methods.add(variableDefinition())
                    if(methods.last().valueExpression != null){
                        throw ParseException("Variables inside a struct can not be initialized and must have a valid type.", previous())
                    }
                }
                else -> throw ParseException("Wrong statement type for a struct", previous())
            }
        }
        consume(TokenType.CURLYR, "")
        return Expression.StructDefinition(name, methods)
    }

    private fun blockStatement(): Expression {
        val statements: MutableList<Expression> = ArrayList()

        while (!peek(TokenType.CURLYR) && !isAtEnd())
            statements.add(statement())

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

    private fun variableDefinition(): Expression.VarInitialization {
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
                    Expression.InstanceSet(expression, value)
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
            expression = Expression.Logical(expression, current(), andExpression())
        return expression
    }

    private fun previous(): Token = tokens[current - 1]


    private fun andExpression(): Expression {
        var expression = equality()
        while (match(TokenType.AND))
            expression = Expression.Logical(expression, current(), equality())
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
        while (match(listOf(TokenType.SLASH, TokenType.STAR)))
            expression = Expression.Operation(expression, current(), unary())
        return expression
    }

    private fun unary(): Expression {
        if (match(listOf(TokenType.BANG, TokenType.PLUSMINUS)))
            return Expression.Unary(current(), unary())
        return call()
    }

    private fun call(): Expression {
        var expression = primary()

        while (true) {
            expression = if (match(TokenType.BRCKTL)) {
                finishCall(expression as Expression.VarCall)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expected an identifier after '.'")
                Expression.InstanceGet(expression, name)
            } else {
                break
            }
        }
        return expression
    }

    private fun finishCall(callee: Expression.VarCall): Expression {
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
            TokenType.POINTER -> Expression.PointerGet(Expression.VarCall(consume(TokenType.IDENTIFIER, "Expected an identifier after '&'.")))
            TokenType.STAR -> Expression.ValueFromPointer(call())
            TokenType.NULLVALUE -> Expression.Literal(null)
            TokenType.STRING -> Expression.Literal(current())
            TokenType.IDENTIFIER -> {
                lexIdentifier()
            }
            TokenType.CHAR -> Expression.Literal(current().substring[0])
            TokenType.NUMBER -> {
                return number()
            }
            TokenType.BRCKTL -> {
                val expression = expression()
                consume(TokenType.BRCKTR, "Expected a closing ')'")
                Expression.Grouping(expression)
            }
            else -> throw ParseException("Expected an expression", previous())

        }
    }

    private fun number(): Expression {
        return if (!current().substring.contains(".")) {
            Expression.Literal(current().substring.toInt())
        } else {
            errorManager.error(current().line, current().index, "Floating point numbers are not available yet.")
            throw ParseException("Floating point numbers are not available yet.", current())
        }
    }

    private fun lexIdentifier(): Expression {
        var result: Expression = Expression.VarCall(current())
        while (match(TokenType.CLOSEDL)) {
            val number = expression()
            consume(TokenType.CLOSEDR, "Expected ']'")
            result = Expression.ArrayGet(result, number)
        }
        return result
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

    private fun getType(): Type {
        if (peek(TokenType.COLON)) {
            current++
            return type()
        }
        return NoneType()
    }
    private fun isNormalOrStructType(typeName: String): Type{
        return try{
            NormalType(NormalTypes.valueOf(typeName.uppercase(Locale.getDefault())))
        } catch (exception: IllegalArgumentException){
            StructType(typeName)
        }
    }

    private fun type(): Type {
        return if(peek(TokenType.IDENTIFIER) && tokens[current + 2].tokenType == TokenType.STAR){
            val substring = consume(TokenType.IDENTIFIER, "Expected a type identifier after ':'").substring
            advance()
            return PointerType(isNormalOrStructType(substring))
        } else if(peek(TokenType.IDENTIFIER)) {
            isNormalOrStructType(consume(TokenType.IDENTIFIER, "Expected a type identifier after ':'").substring)
        }
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
            catchException(errorMessage, tokens[current])
        }
        return tokens[current]
    }

    private fun skipError() {
        while (!isAtEnd()) {
            if (previous().tokenType === TokenType.SEMICOLON) return
            when (peek().tokenType) {
                TokenType.STRUCT, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.RETURN, TokenType.CURLYR -> return
                else -> advance()
            }
        }
    }

    private fun catchException(message: String, token: Token){
        errorManager.error(token.line, token.index, message)
        skipError()
    }

}

class ParseException(override var message: String, var token: Token) : Throwable()

