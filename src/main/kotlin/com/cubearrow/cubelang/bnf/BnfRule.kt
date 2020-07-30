package com.cubearrow.cubelang.bnf

import java.util.*

/**
 * Parser for a single bnf rule. When done parsing, it contains the name and the possible expressions
 *
 * While the name is a [String] the expression is a [List] of the possible options each saved as a [List] of [BnfTerm] of their own.
 *
 * @param ruleString The string to be parsed into the rule
 * @param parser The primary [BnfParser] saving the rule
 * @param additionalParser The additional parser that may contain rules accessed in the expression
 */
class BnfRule(ruleString: String, private var parser: BnfParser, private var additionalParser: BnfParser? = null) : BnfTerm() {
    var name: String
    var expression: List<List<BnfTerm?>>


    /**
     * Parses the name from the line by looking at the string before "::="
     */
    private fun parseName(line: String): String {
        val relevantSubString = line.split("::=").toTypedArray()[0]
        return relevantSubString.replace("<", "").replace(">", "").replace(" ", "")
    }

    /**
     * Parses the expression of the rule by splitting by "|"
     */
    private fun parseExpression(line: String): List<List<BnfTerm?>> {
        val options = line.split("::=")[1].split("/\\|(?![^\"]*\")/g")
        val result: MutableList<List<BnfTerm?>> = ArrayList()
        for (option in options) {
            result.add(parseRuleInExpression(option))
        }
        return result
    }

    /**
     * Parses a single option into a [List] of [BnfTerm]
     *
     * @param option The string representing the option
     *
     * @return Returns a [List] of [BnfTerm] representing the option
     */
    private fun parseRuleInExpression(option: String): List<BnfTerm?> {
        val result: MutableList<BnfTerm?> = ArrayList()
        var i = 0
        // Iterate over the string in order to parse it properly
        while (i < option.length) {
            // Parse a single rule if a "<" is found
            if (option[i] == '<') {
                i = parseRuleInExpression(option, i, result)
            }
            // Parse a string if a '"' is found
            if (option[i] == '"') {
                val closingIndex = option.indexOf("\"", i + 1)
                result.add(BnfStringLiteral(option.substring(i + 1, closingIndex)))
                i = closingIndex
            }
            i++
        }
        return result
    }

    /**
     * Parses a previously defined rule in the expression by looking for definitions in the parser
     */
    private fun parseRuleInExpression(option: String, i: Int, result: MutableList<BnfTerm?>): Int {
        val closingIndex = option.indexOf(">", i + 1)
        var primaryRule = parser.getRuleFromString(option.substring(i + 1, closingIndex))
        if (primaryRule == null) {
            primaryRule = additionalParser?.getRuleFromString(option.substring(i + 1, closingIndex))
        }
        result.add(primaryRule)
        return closingIndex
    }


    /**
     * Prints the rule in the usual bnf fashion
     */
    override fun toString(): String {
        return "<$name> ::= $expression"
    }

    /**
     * Converts the rule to a [Regex] instance by chaining the options together
     */
    override fun toRegex(): Regex {
        var result = "("
        this.expression.forEach { option ->
            option.forEach {
                result += it?.toRegex().toString()
            }
            result += ")|("
        }
        // Remove the last ")|(" and add a ")"
        return Regex(result.substring(0, result.length - 3) + ")")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BnfRule

        if (name != other.name) return false
        if (expression != other.expression) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + expression.hashCode()
        return result
    }

    init {
        name = parseName(ruleString)
        expression = parseExpression(ruleString)
    }
}