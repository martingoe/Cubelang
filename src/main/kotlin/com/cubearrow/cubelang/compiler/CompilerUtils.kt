package com.cubearrow.cubelang.compiler


class CompilerUtils {
    companion object {
        fun getOperator(operatorString: String): String{
            return when(operatorString){
                "+" -> "add"
                "-" -> "sub"
                "*" -> "mul"
                "/" -> "div"
                else -> error("Unexpected operator")
            }
        }
        fun getRegister(baseName: String, length: Int): String {
            return try {
                baseName.toInt()
                when (length) {
                    8 -> "r$baseName"
                    4 -> "r${baseName}d"
                    2 -> "r${baseName}w"
                    1 -> "r${baseName}b"
                    else -> ""
                }
            } catch (e: NumberFormatException) {
                when (length) {
                    8 -> "r${baseName}"
                    4 -> "e${baseName}"
                    2 -> "${baseName[0]}h"
                    1 -> "${baseName[0]}l"
                    else -> ""
                }
            }
        }
    }
}