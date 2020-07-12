package com.cubearrow.cubelang.parsing.tokenization

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



    /**
     * Walks through the line creating the tokens and saves them as a Map. This map is linked in order to preserve the order.
     * @param fileContent The string to translate into tokens
     * @return Returns the tokens in a Map containing the Token and the responsible substring
     */
    private fun loadTokenSequence(fileContent: String) {
        var substringStartingIndex = 0
        for (i in fileContent.indices) {
            lineIndex++
            val substring = fileContent.substring(substringStartingIndex, i)
            val stringAtIndex = fileContent[i].toString()
            if (tokenGrammar.isSeparator(stringAtIndex)) {
                addTokenToResult(substring)
                addTokenToResult(stringAtIndex)
                substringStartingIndex = i + 1
            }
            if (fileContent[i] == '\n') {
                line++
                lineIndex = 0
            }
        }
    }

    private fun addTokenToResult(substring: String) {
        val substringToken = TokenType.fromString(substring, tokenGrammar)
        val index = (lineIndex - substring.length)
        if (substringToken == TokenType.NOT_FOUND) {
            if (substring != " " && !substring.isBlank()) {
                val fullLine = fileContent.split("\n")[line - 1]
                Main.error(line, index, fullLine, "Unexpected token \"$substring\"")
            }
        } else {
            tokenSequence.add(Token(substring, substringToken, lineIndex, index))
        }
    }

    /**
     * Creates a TokenSequence based on the content of the source file and the grammar.
     */
    init {
        loadTokenSequence(fileContent)
    }
}