package com.cubearrow.cubelang.parsing.tokenization

import com.cubearrow.cubelang.bnf.BnfParser
import java.io.File
import java.nio.file.Files

class TokenGrammar(private var grammarFile: File) {
    var bnfParser: BnfParser = BnfParser(this.grammarFile)

    /**
     * Returns if the given String matches the predefined rules of separators
     */
    fun isSeparator(string: String): Boolean {
        return string.matches(Regex("\\s|\\t|\\Z")) ||
                string.matches(getRegex("BRCKTL")) ||
                string.matches(getRegex("BRCKTR")) ||
                string.matches(getRegex("KOMMA")) ||
                string.matches(getRegex("EQUALS")) ||
                string.matches(getRegex("SEMICOLON"))
    }

    /**
     * Returns a [Regex] instance of the required key. Throws a Nullpointer exception if it does not exist.
     */
    private fun getRegex(key: String): Regex {
        return this.bnfParser.getRuleFromString(key.toLowerCase())!!.toRegex()
    }
}