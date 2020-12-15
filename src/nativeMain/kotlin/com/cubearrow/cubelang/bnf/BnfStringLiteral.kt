package com.cubearrow.cubelang.bnf

class BnfStringLiteral(val regexString: String) : BnfTerm() {
    override fun toRegex(): Regex {
        return Regex(this.regexString)
    }


    override fun toString(): String {
        return "\"$regexString\""
    }

    /**
     * Compares two string literals based on the regexString
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as BnfStringLiteral

        if (regexString != other.regexString) return false

        return true
    }

    override fun hashCode(): Int {
        return regexString.hashCode()
    }

}