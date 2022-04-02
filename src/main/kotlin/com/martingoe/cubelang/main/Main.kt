package com.martingoe.cubelang.main

import com.martingoe.cubelang.backend.StatementCompiler
import com.martingoe.cubelang.middleend.treemodification.TreeRewriter
import com.martingoe.cubelang.backend.instructionselection.ASTToIRService
import com.martingoe.cubelang.middleend.validation.TypeChecker
import com.martingoe.cubelang.common.*
import com.martingoe.cubelang.common.definitions.StandardLibraryFunctions
import com.martingoe.cubelang.common.definitions.Function
import com.martingoe.cubelang.common.errors.ErrorManager
import com.martingoe.cubelang.frontend.lexing.Lexer
import com.martingoe.cubelang.frontend.parser.Parser
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
        SymbolTableSingleton.resetAll()
        val expressionsList = HashMap<String, List<Statement>>()
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

        var i = 0
        val asmASTToIRService = ASTToIRService(ASMEmitter())
        for (expressions in expressionsList) {
            val file = File(expressions.key)
            val resultFile = File(file.absoluteFile.parentFile.absolutePath + "/" + file.nameWithoutExtension + ".asm")
            SymbolTableSingleton.currentIndex = i
            SymbolTableSingleton.fileSymbolTables.add(FileSymbolTable())

            TypeChecker(expressions.value, errorManagers[expressions.key]!!, StandardLibraryFunctions.definedFunctions).checkTypes()

            TreeRewriter().rewriteMultiple(expressions.value)
            asmASTToIRService.asmEmitter = ASMEmitter()
            StatementCompiler(asmASTToIRService.asmEmitter, asmASTToIRService, libraryPath).evaluateList(expressions.value)
            resultFile.writeText(asmASTToIRService.asmEmitter.finishedString)
            println("Wrote the resulting asm file to ${resultFile.path}")
            i++
        }
    }

    private fun addFunctionsToMap(fileName: String, expressions: List<Statement>) {
        StandardLibraryFunctions.definedFunctions[fileName] = ArrayList()
        expressions.filterIsInstance<Statement.FunctionDefinition>().forEach {
            val args = mapArgumentDefinitions(it.args)
            StandardLibraryFunctions.definedFunctions[fileName]!!.add(Function(it.name.substring, args, it.type))
        }
    }

    /**
     * Maps a [List] of [Statement] which may only contain [Statement.ArgumentDefinition] to their substrings
     *
     * @throws TypeCastException Throws this exception when one of the elements of the expressions are not a [Statement.ArgumentDefinition]
     * @param expressions The expressions whose names are to be returned
     * @return Returns a [Map] of [String]s mapped to [String]s with the substrings of the identifier of the [Statement.ArgumentDefinition]
     */
    private fun mapArgumentDefinitions(expressions: List<Statement>): Map<String, Type> {
        return expressions.associate { Pair((it as Statement.ArgumentDefinition).name.substring, it.type) }
    }
}