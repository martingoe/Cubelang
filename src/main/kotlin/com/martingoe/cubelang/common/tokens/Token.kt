package com.martingoe.cubelang.common.tokens

/**
 * Saves a token in a data structure
 *
 * @param substring The substring of the Token
 * @param tokenType The [TokenType] parsed
 * @param line The line index from the source file
 * @param index The index of the character in the line
 */
data class Token(var substring: String, var tokenType: TokenType, var line: Int = -1, var index: Int = -1){
    /**
     * Compares two Tokens based on the substring and the tokenType
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as Token

        if (substring != other.substring) return false
        if (tokenType != other.tokenType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = substring.hashCode()
        result = 31 * result + tokenType.hashCode()
        return result
    }
}
