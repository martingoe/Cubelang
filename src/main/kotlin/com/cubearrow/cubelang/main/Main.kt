package com.cubearrow.cubelang.main

import com.cubearrow.cubelang.bnf.BnfParser
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.interpreter.Interpreter
import com.cubearrow.cubelang.lexer.TokenGrammar
import com.cubearrow.cubelang.lexer.TokenSequence
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.parser.Parser
import com.cubearrow.cubelang.utils.ConsoleColor
import com.cubearrow.cubelang.utils.Singleton
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size == 1) {
        Main().compileFile(File(args[0]))
    } else {
        println("No source file was provided")
        exitProcess(64)
    }
}

class Main {
    companion object {
        val syntaxParserSingleton: Singleton<BnfParser> = Singleton()
        val tokenGrammarSingleton: Singleton<TokenGrammar> = Singleton()
        var containsError: Boolean = false
        var useCompiler = false


        fun error(line: Int, index: Int, fullLine: String?, message: String) {
            if (fullLine != null) {
                val indicator = " ".repeat(index - 1) + "^"
                println("""
                ${ConsoleColor.ANSI_RED}${fullLine}
                $indicator
                Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimIndent())
            } else {
                println("""
                ${ConsoleColor.ANSI_RED}Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimIndent())
            }
            containsError = true
            exitProcess(65)
        }
    }


    fun compileFile(sourceFile: File) {
        val bnfFile = File(Main::class.java.classLoader.getResource("TokenGrammar.bnf")!!.file)
        val sourceCode = sourceFile.readText()
        val syntaxGrammarFile = File(Main::class.java.classLoader.getResource("SyntaxGrammar.txt")!!.file).readText()
        tokenGrammarSingleton.instance = TokenGrammar(bnfFile.readText())

        syntaxParserSingleton.instance = BnfParser(syntaxGrammarFile, tokenGrammarSingleton.instance!!.bnfParser)
        val tokenSequence = TokenSequence(sourceCode, tokenGrammarSingleton.instance!!)
        val expressions = Parser(tokenSequence.tokenSequence, listOf(TokenType.SEMICOLON)).parse()
        if (useCompiler) {
            Compiler(expressions, "src/main/resources/output.asm")
        } else {
            Interpreter(expressions)
        }

        if (containsError)
            exitProcess(65)
    }
}