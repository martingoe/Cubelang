package com.cubearrow.cubelang.bnf

import java.util.*


/**
 * A simple parser for BNF files. An additional parser can be used to find secondary rules
 *
 * The constructor initializes the rules from the provided source file
 *
 * @param source The String containing the rules in bnf format
 * @param additionalParser The secondary parser that can be added
 */
class BnfParser(source: String, additionalParser: BnfParser? = null) {
    val rules: MutableList<BnfRule?> = ArrayList()

    /**
     * Returns the first found rule that has provided name
     */
    fun getRuleFromString(name: String): BnfRule? {
        return rules.stream().filter { rule: BnfRule? -> rule?.name == name }.findFirst().orElse(null)
    }

    override fun toString(): String {
        return "BnfParser{ \n" +
                "rules=\n" +
                rules.joinToString("\n") +
                "\n}"
    }


    init {
        for (line in source.split("\n")) {
            if (line.isNotBlank() && !line.startsWith("//")) {
                rules.add(BnfRule(line, this, additionalParser))
            }
        }
    }
}