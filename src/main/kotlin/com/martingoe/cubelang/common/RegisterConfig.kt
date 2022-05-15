package com.martingoe.cubelang.common

class RegisterConfig {
    companion object{
        const val REGISTER_ARG_COUNT = 6
        const val REGISTER_TEMP_COUNT = 6
        val NORMAL_REGISTER = arrayOf("ax", "bx", "dx", "di", "si", "cx")
        val ARG_REGISTERS = arrayOf("di", "si", "dx", "cx", "8", "9")
    }
}