package com.cubearrow.cubelang.parsing.tokenization

/**
 * This initiates a sequence of tokens from the content of a source file. This lexical analysis is used in the parser when creating the abstract syntax tree.
 * A [TokenGrammar] object is required for the appropriate grammar and the regex rules that must be matched.
 *
 *
 * The tokens are saved in a list if Maps each representing a line, the Map contains the original String and the [Token] which it represents.
 *
 */
class TokenSequence(fileContent: String, private var tokenGrammar: TokenGrammar) {
    var tokenSequence: MutableList<Pair<Token, String>> = ArrayList()


    /**
     * Walks through the line creating the tokens and saves them as a Map. This map is linked in order to preserve the order.
     * @param fileContent The string to translate into tokens
     * @return Returns the tokens in a Map containing the Token and the responsible substring
     */
    private fun loadTokenSequence(fileContent: String) {
        var substringStartingIndex = 0
        for (i in fileContent.indices) {
            val substring = fileContent.substring(substringStartingIndex, i)
            val stringAtIndex = fileContent[i].toString()
            if (tokenGrammar.isSeparator(stringAtIndex)) {
                addTokenToResult(tokenSequence, substring)
                addTokenToResult(tokenSequence, stringAtIndex)
                substringStartingIndex = i + 1
            }
        }
    }

    private fun addTokenToResult(result: MutableList<Pair<Token, String>>, substring: String) {
        val substringToken = Token.fromString(substring, tokenGrammar)
        if (substringToken != Token.NOT_FOUND) {
            result.add(Pair(substringToken, substring))
        }
    }

    /**
     * Creates a TokenSequence based on the content of the source file and the grammar.
     */
    init {
        loadTokenSequence(fileContent)
    }
}