package com.cubearrow.cubelang.main

import com.cubearrow.cubelang.parsing.tokenization.TokenGrammar
import com.cubearrow.cubelang.parsing.tokenization.TokenSequence
import java.io.File
import java.io.IOException

object Main {
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
//        File codeFile = new File("src/main/resources/test.cl");
//        TokenGrammar tokenGrammar = new TokenGrammar(new File("src/main/resources/TokenGrammar.json"));
//        TokenSequence tokenSequence = new TokenSequence(Files.readString(codeFile.toPath()), tokenGrammar);
//        System.out.println(tokenSequence.getTokenSequence().toString());

        val bnfFile = File(this.javaClass.classLoader.getResource("TokenGrammar.bnf")!!.file)
        val sourceCode = this.javaClass.classLoader.getResource("test.cl")
        val tokenSequence = TokenSequence(sourceCode!!.readText(), TokenGrammar(bnfFile)).tokenSequence
        println(tokenSequence)
        println(TokenGrammar(bnfFile).bnfParser.rules.joinToString("\n"))
    }
}