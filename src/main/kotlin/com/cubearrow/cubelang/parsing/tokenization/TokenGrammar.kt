package com.cubearrow.cubelang.parsing.tokenization

import com.cubearrow.cubelang.bnf.BnfParser
import java.io.File
import java.nio.file.Files

class TokenGrammar(private var grammarFile: File) {
    public var bnfParser: BnfParser = BnfParser(Files.readString(this.grammarFile.toPath()))

    public fun isSeparator(string: String): Boolean {
        return string.matches(Regex("\\s|\\t")) ||
                string.matches(getRegexString("BRCKTL")) ||
                string.matches(getRegexString("BRCKTR")) ||
                string.matches(getRegexString("KOMMA")) ||
                string.matches(getRegexString("EQUALS")) ||
                string.matches(getRegexString("SEMICOLON"))
    }

    private fun getRegexString(key: String): Regex {
        return this.bnfParser.getRuleFromString(key.toLowerCase())!!.toRegex()
    }
}