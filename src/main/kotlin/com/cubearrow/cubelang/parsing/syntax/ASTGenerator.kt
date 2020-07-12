package com.cubearrow.cubelang.parsing.syntax

import com.cubearrow.cubelang.bnf.BnfParser
import com.cubearrow.cubelang.bnf.BnfRule
import com.cubearrow.cubelang.bnf.BnfTerm
import com.cubearrow.cubelang.parsing.tokenization.Token
import com.cubearrow.cubelang.parsing.tokenization.TokenGrammar
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Argument usage: <outputDirectory> <SyntaxGrammarFile> <TokenGrammarFile>")
        exitProcess(64)
    }
    ASTGenerator(args[0], args[1], args[2])
}

class ASTGenerator(private val outputDir: String, syntaxGrammarFile: String, tokenGrammarFile: String) {
    private var syntaxGrammarParser: BnfParser

    init {
        val tokenGrammar = TokenGrammar(File(tokenGrammarFile))
        this.syntaxGrammarParser = BnfParser(File(syntaxGrammarFile), tokenGrammar.bnfParser)

        generateExpressionClass()

    }

    private fun generateVisitorInterface(): String {
        var visitorClassContent = "    interface ExpressionVisitor<R> {\n"

        syntaxGrammarParser.rules.forEach {
            if (it != null && it.name != "expression") {
                visitorClassContent += "        fun visit${it.name.capitalize()}(${it.name}: ${it.name.capitalize()}): R\n"
            }
        }
        return "$visitorClassContent    }\n"
    }

    private fun generateExpressionClass() {
        var expressionFileContent = generateBaseExpressionClass()
        expressionFileContent += generateSubclasses()
        expressionFileContent += generateVisitorInterface()
        expressionFileContent += "    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R\n"
        expressionFileContent += "}\n"

        val file = File(outputDir + "Expression.kt")
        file.writeText(expressionFileContent)
    }

    private fun generateBaseExpressionClass(): String {
        return """
                package ${this::class.java.packageName}
                
                import ${Token::class.java.canonicalName}
                
                /**
                 * This class is generated automatically by the [ASTGenerator]
                 **/
                abstract class Expression {
            """.trimIndent() + "\n"
    }

    private fun generateSubclasses(): String {
        var expressionFileContent1 = ""
        syntaxGrammarParser.rules.forEach {
            if (it != null && it.name != "expression") {
                val parameters = expressionToParameters(it.expression)
                expressionFileContent1 += """   class ${it.name.capitalize()} ($parameters) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visit${it.name.capitalize()}(this)
        }
    }
"""
            }
        }
        return expressionFileContent1
    }


    private fun expressionToParameters(expression: List<List<BnfTerm?>>?): String {
        var result = ""
        var ruleCount = HashMap<BnfRule, Int>()
        expression?.get(0)?.forEach { term ->
            if (term is BnfRule && term.name != "semicolon") {
                if(ruleCount.containsKey(term)){
                    ruleCount[term] = ruleCount[term]!! + 1
                }else{
                    ruleCount[term] = 1
                }

                val type = syntaxGrammarParser.getRuleFromString(term.name)?.name?.capitalize() ?: "Token"
                result += "var ${term.name}${ruleCount[term]}: ${type}, "

            }
        }
        return result.substring(0, result.length - 2)
    }
}