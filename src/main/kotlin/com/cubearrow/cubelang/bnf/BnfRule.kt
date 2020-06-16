package com.cubearrow.cubelang.bnf

import java.util.*

class BnfRule(ruleString: String, parser: BnfParser) : BnfTerm() {
    var name: String
    private var expression: List<List<BnfTerm?>>
    private fun parseName(line: String): String {
        val relevantSubString = line.split("::=").toTypedArray()[0]
        return relevantSubString.replace("<", "").replace(">", "").replace(" ", "")
    }

    private fun parseExpression(line: String, parser: BnfParser): List<List<BnfTerm?>> {
        val options = line.split("::=")[1].split("/\\|(?![^\"]*\")/g")
        val result: MutableList<List<BnfTerm?>> = ArrayList()
        for (option in options) {
            result.add(parseExpressionInOption(option, parser))
        }
        return result
    }

    private fun parseExpressionInOption(option: String, parser: BnfParser): List<BnfTerm?> {
        val result: MutableList<BnfTerm?> = ArrayList()
        var i = 0
        while (i < option.length) {
            if (option[i] == '<') {
                val closingIndex = option.indexOf(">", i + 1)
                result.add(parser.getRuleFromString(option.substring(i + 1, closingIndex)))
                i = closingIndex
            }
            if (option[i] == '"') {
                val closingIndex = option.indexOf("\"", i + 1)
                result.add(BnfStringLiteral(option.substring(i + 1, closingIndex)))
                i = closingIndex
            }
            i++
        }
        return result
    }

    public fun containsRuleOption(rule: BnfRule) : Boolean{
        for (option in this.expression){
            if (option.size == 1 && option[0] == rule){
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return "<$name> ::= $expression"
    }

    override fun toRegex(): Regex {
        var result = "("
        this.expression.forEach{ option ->
            option.forEach{
                result += it?.toRegex().toString()
            }
            result += ")|("
        }
        return Regex(result.substring(0, result.length - 3) + ")")
    }

    init {
        name = parseName(ruleString)
        expression = parseExpression(ruleString, parser)
    }
}