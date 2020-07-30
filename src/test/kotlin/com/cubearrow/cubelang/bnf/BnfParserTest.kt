package com.cubearrow.cubelang.bnf

import org.junit.jupiter.api.Test


class BnfParserTest {
    @Test
    internal fun testSingleRule() {
        val bnfParser = BnfParser("<test> ::= \"test\"")
        val expected = BnfRule("<test> ::= \"test\"", bnfParser)
        assert(bnfParser.rules[0]!! == expected)
    }
    @Test
    internal fun testMultipleRules() {
        val bnfParser = BnfParser("<test> ::= \"test\"\n" +
                "<foo> ::= <test>")
        assert(bnfParser.rules[0] == BnfRule("<test> ::= \"test\"", bnfParser))
        assert(bnfParser.rules[1] == BnfRule("<foo> ::= <test>", bnfParser))
    }
    @Test
    internal fun testRuleFromString(){
        val bnfParser = BnfParser("<test> ::= \"test\"\n" +
                "<foo> ::= \"bar\"")
        assert(bnfParser.getRuleFromString("test") == BnfRule("<test> ::= \"test\"", bnfParser))
    }


}