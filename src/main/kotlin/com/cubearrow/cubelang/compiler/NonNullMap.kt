package com.cubearrow.cubelang.compiler

import com.cubearrow.cubelang.main.Main
import kotlin.system.exitProcess

class NonNullMap<K, V>(m: MutableMap<out K, out V>?) : LinkedHashMap<K, V>(m) {
    var message = "The requested key could not be found"
    override fun get(key: K): V {
        val value = super.get(key)
        if (value != null) return value
        Main.error(-1, -1, null, message)
        exitProcess(65)
    }
}