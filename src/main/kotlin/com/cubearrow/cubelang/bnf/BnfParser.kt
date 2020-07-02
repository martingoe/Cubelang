package com.cubearrow.cubelang.bnf

import java.io.File
import java.util.*

class BnfParser(file: File, additionalParser: BnfParser? = null) {
    val rules: MutableList<BnfRule?> = ArrayList()
    fun getRuleFromString(substring: String): BnfRule? {
        return rules.stream().filter { rule: BnfRule? -> rule!!.name == substring }.findFirst().orElse(null)
    }


    init {
        val lines = file.readLines()
        for (line in lines) {
            if (!line.isBlank() && !line.startsWith("//")) {
                rules.add(BnfRule(line, this, additionalParser))
            }
        }
    }
}