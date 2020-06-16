package com.cubearrow.cubelang.bnf

class BnfStringLiteral(val regexString: String) : BnfTerm() {
    override fun toRegex(): Regex {
        return Regex(this.regexString)
    }

    override fun toString(): String {
        return "\"$regexString\""
    }

}