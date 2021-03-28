package com.cubearrow.cubelang.lexing

import com.cubearrow.cubelang.common.errors.ErrorManager
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType


/**
 * This initiates a sequence of tokens from the content of a source file. This lexical analysis is used in the parser when creating the abstract syntax tree.
 *
 * Regular Expressions are avoided for preformance reasons.
 */

class Lexer(private val fileContent: String) {
    private var lineIndex = 1
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
                        number("-")
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
                '&' -> {
                    if (match('&'))
                        addToken(TokenType.AND, "&&")
                    else
                        addToken(TokenType.POINTER, "&")
                }

                '#' -> comment()
                '<', '>' -> {
                    if (match('='))
                        addToken(TokenType.COMPARATOR, "$char=")
                    else
                        addToken(TokenType.COMPARATOR, char.toString())
                }
                '=' -> {
                    if (match('='))
                        addToken(TokenType.EQUALITY, "$char=")
                    else
                        addToken(TokenType.EQUALS, char.toString())
                }
                '!' -> {
                    if (match('='))
                        addToken(TokenType.EQUALITY, "$char=")
                    else
                        addToken(TokenType.BANG, char.toString())
                }
                '"' -> string()
                '\'' -> char()
                '\n' -> {
                    line++
                    lineIndex = 1
                }
                else -> {
                    when {
                        isDigit(char) -> {
                            number("");index--
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

    private fun char() {
        var buffer = "\'"
        index++
        while (peek() != '\'' && index < fileContent.length) {
            if (peek() == '\n') line++
            buffer += advance()
        }
        if (index >= fileContent.length) {
            errorLibrary.error(line, lineIndex, "Unterminated char.")
            return
        }

        buffer += advance()
        val value = buffer.substring(1, buffer.length - 1)
        index--
        addToken(TokenType.CHAR, value)
    }

    private fun comment() {
        while (peek() != '\n') index++
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
        lineIndex++
        return fileContent[index - 1]
    }

    private fun string() {
        var buffer = "\""
        index++
        while (peek() != '"' && index < fileContent.length) {
            if (peek() == '\n') line++
            buffer += advance()
        }
        if (index >= fileContent.length) {
            errorLibrary.error(line, lineIndex, "Unterminated string.")
            return
        }

        buffer += advance()
        val value: String = buffer.substring(1, buffer.length - 1)
        index--
        addToken(TokenType.STRING, value)
    }

    private fun number(start: String) {
        var buffer = start
        while (isDigit(peek())) buffer += advance()

        if (peek() == '.' && isDigit(fileContent[index + 1])) {
            while (isDigit(peek())) buffer += advance()
        }

        addToken(TokenType.NUMBER, buffer)
    }

    private fun peek(): Char {
        if (index >= fileContent.length) return '\u0000'
        return fileContent[index]
    }

    private fun addToken(tokenType: TokenType) {
        addToken(tokenType, char.toString())
    }

    private fun addToken(tokenType: TokenType, string: String) {
        tokenSequence.add(Token(string, tokenType, line, lineIndex - string.length))
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

    private fun catchTokenError(substring: String) {
        if(substring == " "){
            lineIndex++
            return
        }
        if (substring.isNotBlank()) {
            errorLibrary.error(line, lineIndex, "Unexpected token \"$substring\"")
        }
    }

    /**
     * Creates a TokenSequence based on the content of the source file and the grammar.
     */
    init {
        loadTokenSequence(fileContent)
    }
}