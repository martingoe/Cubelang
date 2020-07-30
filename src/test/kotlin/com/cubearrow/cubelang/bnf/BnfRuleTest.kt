package com.cubearrow.cubelang.bnf

import org.junit.jupiter.api.Test

class BnfRuleTest {
    @Test
    internal fun testToRegex() {
        val bnfParser = BnfParser("<test> ::= \"test\"")
        assert(bnfParser.rules[0]!!.toRegex().toString() == Regex("(test)").toString())
    }
}