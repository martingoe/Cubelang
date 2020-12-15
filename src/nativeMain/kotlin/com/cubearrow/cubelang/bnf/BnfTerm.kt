package com.cubearrow.cubelang.bnf

abstract class BnfTerm {
    abstract fun toRegex():Regex
}