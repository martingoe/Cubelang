package com.cubearrow.cubelang.interpreter.library

import com.cubearrow.cubelang.utils.IOUtils.Companion.writeAllLines

fun main(args: Array<String>) {
    LibraryGenerator(args[0])
}

class LibraryGenerator(private var outputDir: String) {
    companion object {
        private val BASE_CLASSES = listOf(PrintingLibrary.printDouble::class, PrintingLibrary.printInt::class, PrintingLibrary.printString::class, UtilLibrary.Abs::class, UtilLibrary.Len::class)
    }
    private var resultClassSource = ""
    init{
        generateBaseClass()
    }

    private fun generateBaseClass() {
        resultClassSource += """package com.cubearrow.cubelang.interpreter
            |
            |import com.cubearrow.cubelang.interpreter.Callable
            |
            |class Library {
            |    val classes = ArrayList<Callable>()
            |   
            |${generateInitialization()}
            |}
        """.trimMargin()
        writeAllLines(outputDir + "Library.kt", resultClassSource)
    }

    private fun generateInitialization(): String {
        var result = "    init {\n"
        BASE_CLASSES.forEach {
            result += "        classes.add(${it.simpleName}.${it.simpleName}())\n"
        }
        return "$result    }\n"
    }
}
