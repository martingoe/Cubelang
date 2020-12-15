package com.cubearrow.cubelang.interpreter.library

import com.cubearrow.cubelang.interpreter.Callable

class Library {
    val classes = ArrayList<Callable>()
   
    init {
        classes.add(PrintingLibrary.printDouble())
        classes.add(PrintingLibrary.printInt())
        classes.add(PrintingLibrary.printString())
        classes.add(UtilLibrary.Abs())
        classes.add(UtilLibrary.Len())
    }

}