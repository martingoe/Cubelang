package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.utils.IOUtils.Companion.readAllText
import com.cubearrow.cubelang.utils.IOUtils.Companion.writeAllLines


/**
 * A generator for the AST from BNF grammar files
 *
 * An abstract base class is created that contains the specific subclasses with the appropriate arguments.
 *
 * The baseclass also contains a Visitor-Interface which has call functions for every [Expression] type.
 * These also each implement the abstract [Expression.accept] function calling their specific call function.
 * The visitor pattern is used to be able to use functions on the specific [Expression] types without changing the generated class.
 *
 *
 * @param outputDir The directory path to output the Expression class to
 * @param syntaxGrammarFile The path to the bnf syntax grammar file
 */
class ASTGenerator(private val outputDir: String, syntaxGrammarFile: String) {
    private var syntaxGrammar: String = readAllText(syntaxGrammarFile)

    /**
     * Initialize the class by creating the grammar objects and generating the actual class
     */
    init {
        generateExpressionClass()
    }


    private fun generateVisitorInterface(): String {
        var visitorClassContent = "    interface ExpressionVisitor<R> {\n"

        for (line in syntaxGrammar.lines()) {
            if (line.isBlank() || line.isEmpty()) continue
            val split = line.split(" = ")
            visitorClassContent += "        fun visit${split[0].capitalize()}(${split[0]}: ${split[0].capitalize()}): R\n"
        }
        return "$visitorClassContent    }\n"
    }

    private fun generateExpressionClass() {
        var expressionFileContent = generateBaseExpressionClass()
        expressionFileContent += generateSubclasses()
        expressionFileContent += generateVisitorInterface()
        expressionFileContent += "    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R\n"
        expressionFileContent += "}\n"

        writeAllLines(outputDir + "Expression.kt", expressionFileContent)
    }

    private fun generateBaseExpressionClass(): String {
        return """
                package com.cubearrow.cubelang.parser
                
                
                import com.cubearrow.cubelang.utils.Type
                import com.cubearrow.cubelang.lexer.Token
                
                /**
                 * This class is generated automatically by the [ASTGenerator]
                 **/
                abstract class Expression {
            """.trimIndent() + "\n"
    }

    /**
     * Generates the subclasses from the syntax grammar including implementing [Expression.accept]
     */
    private fun generateSubclasses(): String {
        var expressionFileContent1 = ""
        for (line in syntaxGrammar.lines()) {
            if (line.isBlank() || line.isEmpty()) continue
            val split = line.split(" = ")
            val parameters = getParameters(split[1])
            expressionFileContent1 += """
    class ${split[0].capitalize()} ($parameters) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visit${split[0].capitalize()}(this)
        }
    }
"""
        }
        return expressionFileContent1
    }


    /**
     * Generates the parameters to the constructor of a subclass of [Expression] from a [String]
     */
    private fun getParameters(parameterString: String): String {
        var result = ""
        val ruleCount = HashMap<String, Int>()
        val arguments = parameterString.split(", ")
        if (arguments.isNotEmpty()) {
            arguments.forEach { term ->
                result += addParameter(term, ruleCount)
            }
        } else {
            result += addParameter(parameterString, ruleCount)
        }

        // Returns the result while removing ", " from the end
        return result.substring(0, result.length - 2)
    }

    private fun addParameter(term: String, ruleCount: HashMap<String, Int>): String {
        val split = term.split(": ")
        handleRuleCount(ruleCount, split[0])
        return "val ${split[0]}${if (ruleCount[split[0]] != 1) ruleCount[split[0]] else ""}: ${split[1]}, "
    }


    /**
     * Increment the ruleCount for rules that were already used or initialize it as 1
     *
     * The ruleCount is used to avoid duplicate parameter names in the subclasses of [Expression]
     */
    private fun handleRuleCount(ruleCount: HashMap<String, Int>, term: String) {
        if (ruleCount.containsKey(term)) {
            ruleCount[term] = ruleCount[term]!! + 1
        } else {
            ruleCount[term] = 1
        }
    }
}