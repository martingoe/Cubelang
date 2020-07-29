package com.cubearrow.cubelang.lexer

import com.cubearrow.cubelang.bnf.BnfRule

/**
 * The separate tokens in enum form. The same ones can be found in the grammar file.
 */
enum class TokenType {
    IDENTIFIER, CURLYR, CURLYL, LINE_COMMENT,IF, RETURN, FUN, WHILE, BRCKTL, BRCKTR, SEMICOLON, OPERATOR, EQEQ, EXCLEQ, AND_GATE, OR_GATE, NUMBER, EQUALS, NOT_FOUND, COMMA;

    companion object {
        /**
         * Parses a Token from a string using a [TokenGrammar] object to know what RegEx to match.
         *
         * @param string       The string to parse the token from
         * @param tokenGrammar The lexification grammar in [TokenGrammar]. This contains the regexes that the string has to match.
         * @return Returns the valid Token, returns Token.NOT_FOUND if nothing matches.
         */
        fun fromString(string: String, tokenGrammar: TokenGrammar): TokenType {
            val rules: MutableList<BnfRule?> = tokenGrammar.bnfParser.rules
            // Iterate over the grammar entries in order to see which RegEx key matches the string
            for (rule in rules) {
                if(rule == null){
                    continue
                }
                if (string.matches(rule.toRegex())) {
                    return valueOf(rule.name.toUpperCase())
                }
            }
            return NOT_FOUND
        }
    }
}