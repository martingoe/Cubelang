package com.cubearrow.cubelang.main

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.common.definitions.DefinedFunctions
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.errors.ErrorManager
import com.cubearrow.cubelang.ir.IRCompiler
import com.cubearrow.cubelang.ircompiler.X86IRCompiler
import com.cubearrow.cubelang.lexing.Lexer
import com.cubearrow.cubelang.parser.Parser
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        Main(System.getenv("CUBELANG_LIB") ?: error("Could not find the environment variable for the library path.")).compileFile(args)
    } else {
        println("No source file was provided")
        exitProcess(64)
    }
}


class Main(private val libraryPath: String) {
    fun compileFile(sourceFile: Array<String>) {
        val expressionsList = HashMap<String, List<Expression>>()
        val errorManagers: MutableMap<String, ErrorManager> = mutableMapOf()
        for (source in sourceFile) {
            val sourceCode = File(source).readText()
            val lines = sourceCode.split("\n")
            val tokenSequence = Lexer(sourceCode)
            val errorManager = ErrorManager(lines, false)
            errorManagers[source] = errorManager
            val expressions = Parser(tokenSequence.tokenSequence, errorManager).parse()
            addFunctionsToMap(source, expressions)
            expressionsList[source] = expressions
        }
        errorManagers.forEach { it.value.exitIfError() }

        for (expressions in expressionsList) {
            val file = File(expressions.key)
            val resultFile = File(file.absoluteFile.parentFile.absolutePath + "/" + file.nameWithoutExtension + ".asm")
            val irCompiler = IRCompiler(expressions.value, libraryPath, DefinedFunctions.definedFunctions, errorManagers[expressions.key]!!)
            val irValues = irCompiler.parse()
            println(irValues.joinToString("\n"))
            resultFile.writeText(X86IRCompiler(irValues, irCompiler.context.structs).compile())
        }
    }

    private fun addFunctionsToMap(fileName: String, expressions: List<Expression>) {
        DefinedFunctions.definedFunctions[fileName] = ArrayList()
        expressions.filterIsInstance<Expression.FunctionDefinition>().forEach {
            val args = mapArgumentDefinitions(it.args)
            DefinedFunctions.definedFunctions[fileName]!!.add(Function(it.name.substring, args, it.type))
        }
    }

    /**
     * Maps a [List] of [Expression] which may only contain [Expression.ArgumentDefinition] to their substrings
     *
     * @throws TypeCastException Throws this exception when one of the elements of the expressions are not a [Expression.VarCall]
     * @param expressions The expressions whose names are to be returned
     * @return Returns a [Map] of [String]s mapped to [String]s with the substrings of the identifier of the [Expression.ArgumentDefinition]
     */
    private fun mapArgumentDefinitions(expressions: List<Expression>): Map<String, Type> {
        return expressions.associate { Pair((it as Expression.ArgumentDefinition).name.substring, it.type) }
    }
}