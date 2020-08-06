package com.cubearrow.cubelang.library

import com.cubearrow.cubelang.interpreter.Callable

class Library {
    val classes = ArrayList<Callable>()

    init {
        classes.add(PrintingLibrary.Println())
    }

}