package com.cubearrow.cubelang.main

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.lexer.Tokenizer
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.parser.Parser
import com.cubearrow.cubelang.utils.ConsoleColor
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.IOUtils.Companion.readAllText
import com.cubearrow.cubelang.utils.NormalType
import com.cubearrow.cubelang.utils.PointerType
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        Main().compileFile(args)
    } else {
        println("No source file was provided")
        exitProcess(64)
    }
}


class Main {
    companion object {
        var definedFunctions = mutableMapOf(
            "io" to mutableListOf(
                Compiler.Function("printChar", mapOf("value" to NormalType("char")), null),
                Compiler.Function("printInt", mapOf("value" to NormalType("i32")), null),
                Compiler.Function("printShort", mapOf("value" to NormalType("i8")), null),
                Compiler.Function("printPointer", mapOf("value" to PointerType(NormalType("any"))), null)
            ),
            "time" to mutableListOf(
                Compiler.Function("getCurrentTime", mapOf(), NormalType("i32"))
            ),
            "IntMath" to mutableListOf(
                Compiler.Function("min", mapOf("first" to NormalType("i32"), "sec" to NormalType("i32")), NormalType("i32")),
                Compiler.Function("max", mapOf("first" to NormalType("i32"), "sec" to NormalType("i32")), NormalType("i32"))
            )
        )

        var containsError = false
        var exitAfterError = false
        var lines: MutableMap<String, List<String>> = HashMap()
        var compilers: MutableMap<Compiler, String> = HashMap()


        /**
         * Prints an error in the console while specifying the line and character index.
         *
         * If [exitAfterError] is active, this will exit the process.
         *
         * @param line The line at which the error is located
         * @param index The character index at which the error is located/starts
         * @param message The error message itself
         */
        fun error(line: Int, index: Int, message: String, sourceFile: String) {
            if (line >= 0 || index >= 0) {
                val indicator = " ".repeat(index - 1) + "^"
                println(
                    """${ConsoleColor.ANSI_RED}${lines[sourceFile]!![line - 1]}
                |$indicator 
                |Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimMargin()
                )
            } else {
                println(
                    """
                ${ConsoleColor.ANSI_RED}Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimIndent()
                )
            }
            containsError = true
            if (exitAfterError)
                exitProcess(65)
        }

        fun error(line: Int, index: Int, message: String, compiler: Compiler) {
            error(line, index, message, compilers[compiler]!!)
        }
    }


    fun compileFile(sourceFile: Array<String>) {
//        ASTGenerator("src/main/kotlin/com/cubearrow/cubelang/parser/", "src/main/resources/SyntaxGrammar.txt")
        val expressionsList = HashMap<String, List<Expression>>()
        for (source in sourceFile) {
            val sourceCode = readAllText(source)
            lines[source] = sourceCode.split("\n")
            val tokenSequence = Tokenizer(sourceCode, source)
            val expressions = Parser(tokenSequence.tokenSequence).parse()
            addFunctionsToMap(source, expressions)
            expressionsList[source] = expressions
        }
        if (containsError)
            exitProcess(65)

        exitAfterError = true
        for (expressions in expressionsList) {
            val file = File(expressions.key)
            val compiler = Compiler(
                expressions.value,
                definedFunctions[expressions.key]!!,
                file.absoluteFile.parentFile.absolutePath + "/" + file.nameWithoutExtension + ".asm"
            )
            compilers[compiler] = expressions.key
            compiler.compile()
        }
    }

    private fun addFunctionsToMap(fileName: String, expressions: List<Expression>) {
        definedFunctions[fileName] = ArrayList()
        expressions.filterIsInstance<Expression.FunctionDefinition>().forEach {
            val args = ExpressionUtils.mapArgumentDefinitions(it.args)
            if (args.size > 5)
                error(it.name.line, it.name.index, "The function must only have 5 arguments", fileName)
            definedFunctions[fileName]!!.add(Compiler.Function(it.name.substring, args, it.type))
        }
    }
}