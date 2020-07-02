package com.cubearrow.cubelang.bnf

import java.util.*

class BnfRule(ruleString: String, parser: BnfParser, additionalParser: BnfParser? = null) : BnfTerm() {
    var name: String
    var expression: List<List<BnfTerm?>>
    private fun parseName(line: String): String {
        val relevantSubString = line.split("::=").toTypedArray()[0]
        return relevantSubString.replace("<", "").replace(">", "").replace(" ", "")
    }

    private fun parseExpression(line: String, parser: BnfParser, additionalParser: BnfParser?): List<List<BnfTerm?>> {
        val options = line.split("::=")[1].split("/\\|(?![^\"]*\")/g")
        val result: MutableList<List<BnfTerm?>> = ArrayList()
        for (option in options) {
            result.add(parseExpressionInOption(option, parser, additionalParser))
        }
        return result
    }

    private fun parseExpressionInOption(option: String, parser: BnfParser, additionalParser: BnfParser?): List<BnfTerm?> {
        val result: MutableList<BnfTerm?> = ArrayList()
        var i = 0
        while (i < option.length) {
            if (option[i] == '<') {
                i = addOptionToResult(option, i, result, parser, additionalParser)
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

    private fun addOptionToResult(option: String, i: Int, result: MutableList<BnfTerm?>, parser: BnfParser, additionalParser: BnfParser?): Int {
        val closingIndex = option.indexOf(">", i + 1)
        var primaryRule = parser.getRuleFromString(option.substring(i + 1, closingIndex))
        if(primaryRule == null){
            primaryRule = additionalParser?.getRuleFromString(option.substring(i + 1, closingIndex))
        }
        result.add(primaryRule)
        return closingIndex
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
        expression = parseExpression(ruleString, parser, additionalParser)
    }
}