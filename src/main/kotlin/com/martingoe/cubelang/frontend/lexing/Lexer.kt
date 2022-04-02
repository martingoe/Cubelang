package com.martingoe.cubelang.frontend.lexing

import com.martingoe.cubelang.common.errors.ErrorManager
import com.martingoe.cubelang.common.tokens.Token
import com.martingoe.cubelang.common.tokens.TokenType


/**
 * This initiates a sequence of tokens from the content of a source file. This lexical analysis is used in the parser when creating the abstract syntax tree.
 *
 * Regular Expressions are avoided for performance reasons.
 */

class Lexer(private val fileContent: String) {
    private var column = 0
    var tokenSequence: MutableList<Token> = ArrayList()
    private var line = 1
    private var char: Char = ' '
    private var index = 0
    private val errorLibrary = ErrorManager(fileContent.split("\n"), false)

    /**
     * Walks through the line creating the tokens and saves them as a Map. This map is linked in order to preserve the order.
     * @param fileContent The string to translate into tokens
     * @return Returns the tokens in a Map containing the Token and the responsible substring
     */
    private fun loadTokenSequence(fileContent: String) {
        while (index < fileContent.length) {
            char = fileContent[index]
            when (char) {
                '(' -> addToken(TokenType.BRCKTL)
                ')' -> addToken(TokenType.BRCKTR)
                '{' -> addToken(TokenType.CURLYL)
                ':' -> addToken(TokenType.COLON)
                '}' -> addToken(TokenType.CURLYR)
                '[' -> addToken(TokenType.CLOSEDL)
                ']' -> addToken(TokenType.CLOSEDR)
                ',' -> addToken(TokenType.COMMA)
                '.' -> addToken(TokenType.DOT)
                '-', '+' -> {
                    if (isDigit(fileContent[index + 1])) {
                        advance()
                        number(char.toString())
                        index--
                    } else {
                        addToken(TokenType.PLUSMINUS)
                    }
                }
                ';' -> addToken(TokenType.SEMICOLON)
                '*' -> addToken(TokenType.STAR)
                '/' -> addToken(TokenType.SLASH)
                '|' -> {
                    if (match('|'))
                        addToken(TokenType.OR, "||")
                }
                '#' -> comment()
                '&' -> addConditionalNextChar('&', TokenType.AND, TokenType.POINTER)
                '<', '>' -> addConditionalNextChar('=', TokenType.COMPARATOR)
                '=' -> addConditionalNextChar('=', TokenType.EQUALITY, TokenType.EQUALS)
                '!' -> addConditionalNextChar('=', TokenType.EQUALITY, TokenType.BANG)
                '"' -> string()
                '\'' -> char()
                '\n' -> {
                    line++
                    column = 1
                }
                else -> {
                    when {
                        isDigit(char) -> {
                            number(""); index--
                        }
                        isAlpha(char) -> {
                            keyword(); index--
                        }
                        else -> catchTokenError(char.toString())
                    }
                }
            }
            index++
        }

        addToken(TokenType.EOF, "")
    }

    private fun addConditionalNextChar(nextChar: Char, ifMatches: TokenType, elseToken: TokenType = ifMatches) {
        if (match(nextChar))
            addToken(ifMatches, "$char$nextChar")
        else
            addToken(elseToken)
    }

    private fun errorOnCurrent(message: String) {
        errorLibrary.error(line, column, message)
    }

    private fun char() {
        var buffer = ""
        index++
        while (peek() != '\'' && index < fileContent.length) {
            if (peek() == '\n') line++
            buffer += advance()
        }
        if (index >= fileContent.length) {
            errorOnCurrent("Unterminated char.")
            return
        }

        addToken(TokenType.CHAR, buffer)
    }

    private fun comment() {
        while (peek() != '\n') advance()
        line++
        column = 0
    }

    private fun match(expected: Char): Boolean {
        if (fileContent[index + 1] != expected) return false
        index++
        return true
    }


    private fun keyword() {
        var buffer = ""
        while (isAlphaNumeric(peek())) buffer += advance()

        when (buffer) {
            "fun" -> addToken(TokenType.FUN, buffer)
            "import" -> addToken(TokenType.IMPORT, buffer)
            "struct" -> addToken(TokenType.STRUCT, buffer)
            "return" -> addToken(TokenType.RETURN, buffer)
            "var" -> addToken(TokenType.VAR, buffer)
            "if" -> addToken(TokenType.IF, buffer)
            "else" -> addToken(TokenType.ELSE, buffer)
            "while" -> addToken(TokenType.WHILE, buffer)
            "null" -> addToken(TokenType.NULLVALUE, buffer)
            "for" -> addToken(TokenType.FOR, buffer)
            else -> addToken(TokenType.IDENTIFIER, buffer)
        }
    }

    private fun advance(): Char {
        index++
        column++
        return fileContent[index - 1]
    }

    private fun string() {
        var buffer = ""
        index++
        while (peek() != '"' && index < fileContent.length) {
            if (peek() == '\n') line++
            buffer += advance()
        }
        if (index >= fileContent.length) {
            errorOnCurrent("Unterminated string.")
            return
        }

        addToken(TokenType.STRING, buffer)
    }

    private fun number(start: String) {
        var buffer = start
        if (char == '0' && match('x'))
            return hexadecimalNumber()
        if (char == '0' && match('b'))
            return binaryNumber()
        while (isDigit(peek())) buffer += advance()

        if (peek() == '.' && isDigit(fileContent[index + 1])) {
            while (isDigit(peek())) buffer += advance()
        }

        addToken(TokenType.NUMBER, buffer)
    }

    private fun binaryNumber() {
        var buffer = ""
        advance()
        while (isBinaryDigit(peek())) buffer += advance()
        addToken(TokenType.NUMBER, buffer.toInt(2).toString())
    }

    private fun hexadecimalNumber() {
        var buffer = ""
        advance()
        while (isHexDigit(peek())) buffer += advance()
        addToken(TokenType.NUMBER, buffer.toInt(16).toString())
    }

    private fun peek(): Char {
        if (index >= fileContent.length) return '\u0000'
        return fileContent[index]
    }

    private fun addToken(tokenType: TokenType) {
        addToken(tokenType, char.toString())
    }

    private fun addToken(tokenType: TokenType, string: String) {
        tokenSequence.add(Token(string, tokenType, line, column - string.length))
    }

    private fun isAlpha(c: Char): Boolean {
        return c in 'a'..'z' ||
                c in 'A'..'Z' || c == '_'
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun isHexDigit(c: Char): Boolean {
        return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
    }

    private fun isBinaryDigit(c: Char): Boolean {
        return c in '0'..'1'
    }

    private fun catchTokenError(substring: String) {
        if (substring == " ") {
            column++
            return
        }
        if (substring.isNotBlank()) {
            errorOnCurrent("Unexpected token \"$substring\"")
        }
    }

    /**
     * Creates a TokenSequence based on the content of the source file and the grammar.
     */
    init {
        loadTokenSequence(fileContent)
    }
}