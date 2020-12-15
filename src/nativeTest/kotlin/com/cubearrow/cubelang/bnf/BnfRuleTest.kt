package com.cubearrow.cubelang.bnf

import kotlin.test.Test

class BnfRuleTest {
    @Test
    internal fun testToRegex() {
        val bnfParser = BnfParser("<test> ::= \"test\"")
        assert(bnfParser.rules[0]!!.toRegex().toString() == Regex("(test)").toString())
    }
}