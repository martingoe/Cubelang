package com.cubearrow.cubelang.lexer

import com.cubearrow.cubelang.main.Main

/**
 * This initiates a sequence of tokens from the content of a source file. This lexical analysis is used in the parser when creating the abstract syntax tree.
 * A [TokenGrammar] object is required for the appropriate grammar and the regex rules that must be matched.
 *
 *
 * The tokens are saved in a list if Maps each representing a line, the Map contains the original String and the [TokenType] which it represents.
 */


class TokenSequence(private val fileContent: String, private var tokenGrammar: TokenGrammar) {
    private var lineIndex: Int = 0
    var tokenSequence: MutableList<Token> = ArrayList()
    private var line = 1
    private var isComment = false
    private var isString = false
    private var isChar = false
    private var stringResult = ""
    private var charResult: Char? = null


    /**
     * Walks through the line creating the tokens and saves them as a Map. This map is linked in order to preserve the order.
     * @param fileContent The string to translate into tokens
     * @return Returns the tokens in a Map containing the Token and the responsible substring
     */
    private fun loadTokenSequence(fileContent: String) {
        var substringStartingIndex = 0
        for (i in fileContent.indices) {
            if (substringStartingIndex > i) continue

            lineIndex++
            val substring = fileContent.substring(substringStartingIndex, i)
            val stringAtIndex = fileContent[i].toString()

            if (tokenGrammar.isSeparator(stringAtIndex)) {
                if (stringAtIndex == "." && substring.matches(tokenGrammar.getRegex("DOUBLE"))) continue
                substringStartingIndex = if (fileContent.length > i + 1 && TokenType.fromString(stringAtIndex + fileContent[i + 1], tokenGrammar) != TokenType.NOT_FOUND) {
                    addTwoTokens(substring, stringAtIndex + fileContent[i + 1])
                    i + 2
                } else {
                    addTwoTokens(stringAtIndex, substring)
                    i + 1
                }
            }

            adjustNewLine(fileContent[i])
        }
        tokenSequence.add(Token("", TokenType.EOF, line, lineIndex + 1))
    }

    private fun adjustNewLine(character: Char) {
        if (character == '\n') {
            line++
            lineIndex = 0
            isComment = false
        }
    }

    private fun addTwoTokens(s1: String, s2: String) {
        setCommentMode(s1)
        lineIndex--
        if (s2.isNotEmpty()) addTokenToResult(s2)
        lineIndex++
        if (s1.isNotEmpty()) addTokenToResult(s1)
    }

    private fun setStringMode(s1: String): Boolean {
        this.isString = s1 == "\"" && !isComment
        return isString
    }

    private fun setCommentMode(stringAtIndex: String) {
        isComment = stringAtIndex.matches(tokenGrammar.bnfParser.getRuleFromString("line_comment")!!.toRegex())
    }

    private fun addTokenToResult(substring: String) {
        if (isString && substring == "\"") {
            val index = (lineIndex - substring.length)
            tokenSequence.add(Token(stringResult, TokenType.STRING, line, index))
            isString = false
            return
        } else if (isString) {
            stringResult += substring
        } else if (isChar && substring == "'") {
            val index = (lineIndex - substring.length)
            tokenSequence.add(Token(charResult.toString(), TokenType.CHAR, line, index))
            isChar = false
            return
        } else if (isChar) {
            charResult = substring[0]
        } else {
            setStringMode(substring)
            setCharMode(substring)
        }

        if (!isComment && !isString && !isChar) {
            val substringToken = TokenType.fromString(substring, tokenGrammar)
            val index = (lineIndex - substring.length)
            if (substringToken == TokenType.NOT_FOUND) {
                catchTokenError(substring, index)
            } else {
                tokenSequence.add(Token(substring, substringToken, line, index))
            }
        }
    }

    private fun setCharMode(substring: String): Boolean {
        this.isChar = substring == "'" && !isString
        return isChar
    }

    private fun catchTokenError(substring: String, index: Int) {
        if (substring != " " && !substring.isBlank()) {
            val fullLine = fileContent.split("\n")[line - 1]
            Main.error(line, index, fullLine, "Unexpected token \"$substring\"")
        }
    }

    /**
     * Creates a TokenSequence based on the content of the source file and the grammar.
     */
    init {
        loadTokenSequence(fileContent)
    }
}