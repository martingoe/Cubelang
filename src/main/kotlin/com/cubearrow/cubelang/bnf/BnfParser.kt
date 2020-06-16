package com.cubearrow.cubelang.bnf

import java.util.*

class BnfParser(content: String) {
    val rules: MutableList<BnfRule?> = ArrayList()
    fun getRuleFromString(substring: String): BnfRule? {
        return rules.stream().filter { rule: BnfRule? -> rule!!.name == substring }.findFirst().orElse(null)
    }


    init {
        val lines = content.split("\n").toTypedArray()
        for (line in lines) {
            if (!line.isBlank() && !line.startsWith("//")) {
                rules.add(BnfRule(line, this))
            }
        }
    }
}