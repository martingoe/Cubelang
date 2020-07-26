package com.cubearrow.cubelang.lexer

import com.cubearrow.cubelang.bnf.BnfParser
import java.io.File

/**
 * Wrapper for [BnfParser] with specific functions for Tokens
 *
 * @param grammarFile The BNF file representing the token grammar
 */
class TokenGrammar(private var grammarFile: File) {
    var bnfParser: BnfParser = BnfParser(this.grammarFile)
    private val separators = listOf(getRegex("BRCKTL"), getRegex("BRCKTR"),
            getRegex("KOMMA"), getRegex("EQUALS"), getRegex("SEMICOLON"),
            getRegex("OPERATOR"), getRegex("LINE_COMMENT"), Regex("\\s|\\t|\\z"))


    /**
     * Returns if the given String matches the predefined rules of separators
     *
     * @param string The string to check weather or not it's a separator
     */
    fun isSeparator(string: String): Boolean {
        return separators.stream().anyMatch { string.matches(it) }
    }

    /**
     * Returns a [Regex] instance of the required key.
     *
     * @param key The key to search for
     * @throws KotlinNullPointerException if the key does not exist
     */
    private fun getRegex(key: String): Regex {
        return this.bnfParser.getRuleFromString(key.toLowerCase())!!.toRegex()
    }
}