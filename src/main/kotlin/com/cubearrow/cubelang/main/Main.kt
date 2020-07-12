package com.cubearrow.cubelang.main

import com.cubearrow.cubelang.bnf.BnfParser
import com.cubearrow.cubelang.parsing.syntax.Parser
import com.cubearrow.cubelang.parsing.tokenization.TokenGrammar
import com.cubearrow.cubelang.parsing.tokenization.TokenSequence
import com.cubearrow.cubelang.utils.ConsoleColor
import com.cubearrow.cubelang.utils.Singleton
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size == 1) {
        Main().compileFile(File(Main::class.java.classLoader.getResource(args[0])!!.file))
    } else {
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
                ${ConsoleColor.ANSI_RED.ansiCode}${fullLine}
                $indicator
                Error [$line:$index]: $message 
            """.trimIndent())
            containsError = true
        }
    }


        fun compileFile(sourceFile: File) {
            val bnfFile = File(Main::class.java.classLoader.getResource("TokenGrammar.bnf")!!.file)
            val sourceCode = sourceFile.readText()
            val syntaxGrammarFile = File(Main::class.java.classLoader.getResource("SyntaxGrammar.bnf")!!.file)
            tokenGrammarSingleton.instance = TokenGrammar(bnfFile)

            syntaxParserSingleton.instance = BnfParser(syntaxGrammarFile, tokenGrammarSingleton.instance!!.bnfParser)
            val tokenSequence = TokenSequence(sourceCode, tokenGrammarSingleton.instance!!)
            Parser(tokenSequence.tokenSequence).parse()
//        println(TokenSequence(sourceCode, tokenGrammarSingleton.instance!!).tokenSequence)
//        println(Assignment(ArrayList()).getRule())
//        println(TokenGrammar(bnfFile).bnfParser.rules.joinToString("\n"))


            if (containsError)
                exitProcess(65)
        }


    }