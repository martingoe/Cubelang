package com.cubearrow.cubelang.library

import java.io.File

fun main(args: Array<String>) {
    LibraryGenerator(args[0])
}

class LibraryGenerator(private var outputDir: String) {
    companion object {
        private val BASE_CLASSES = listOf(PrintingLibrary::class, UtilLibrary::class)
    }
    private var resultClassSource = ""
    init{
        generateBaseClass()
    }

    private fun generateBaseClass() {
        resultClassSource += """${this::class.java.`package`}
            |
            |import com.cubearrow.cubelang.interpreter.Callable
            |
            |class Library {
            |    val classes = ArrayList<Callable>()
            |   
            |${generateInitialization()}
            |}
        """.trimMargin()
        File(outputDir + "Library.kt").writeText(resultClassSource)
    }

    private fun generateInitialization(): String {
        var result = "    init {\n"
        BASE_CLASSES.forEach {
            it.nestedClasses.forEach { subClass ->

                result += "        classes.add(${it.simpleName}.${subClass.simpleName}())\n"
            }
        }
        return "$result    }\n"
    }
}