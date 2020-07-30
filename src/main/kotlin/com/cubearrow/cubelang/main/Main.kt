package com.cubearrow.cubelang.main

import com.cubearrow.cubelang.bnf.BnfParser
import com.cubearrow.cubelang.interpreter.Interpreter
import com.cubearrow.cubelang.parser.Parser
import com.cubearrow.cubelang.lexer.TokenGrammar
import com.cubearrow.cubelang.lexer.TokenSequence
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.utils.ConsoleColor
import com.cubearrow.cubelang.utils.Singleton
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size == 1) {
        Main().compileFile(File(Main::class.java.classLoader.getResource(args[0])!!.file))
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


        fun error(line: Int, index: Int, fullLine: String?, message: String) {
            var indicator = ""

            if (fullLine != null) {
                indicator = " ".repeat(index - 1) + "^"
            }
            println("""
                ${ConsoleColor.ANSI_RED}${fullLine}
                $indicator
                Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimIndent())
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
            Interpreter(expressions)
//        println(TokenSequence(sourceCode, tokenGrammarSingleton.instance!!).tokenSequence)
//        println(Assignment(ArrayList()).getRule())
//        println(TokenGrammar(bnfFile).bnfParser.rules.joinToString("\n"))


            if (containsError)
                exitProcess(65)
        }


    }