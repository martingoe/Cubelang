package com.martingoe.cubelang.main

import com.martingoe.cubelang.backend.StatementCompiler
import com.martingoe.cubelang.middleend.treemodification.TreeRewriter
import com.martingoe.cubelang.backend.instructionselection.ASTToIRService
import com.martingoe.cubelang.frontend.semantic.TypeChecker
import com.martingoe.cubelang.common.*
import com.martingoe.cubelang.common.definitions.StandardLibraryFunctions
import com.martingoe.cubelang.common.definitions.Function
import com.martingoe.cubelang.common.definitions.Struct
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
    /**
     * Compiles an array of files represented with their path strings
     */
    fun compileFile(sourceFile: Array<String>) {
        SymbolTableSingleton.resetAll()
        StandardLibraryFunctions.defineFileSymbolTables()
        val expressionsList = HashMap<String, List<Statement>>()
        val errorManagers: MutableMap<String, ErrorManager> = mutableMapOf()
        var i = 0

        for (source in sourceFile) {
            val sourceCode = File(source).readText()
            val lines = sourceCode.split("\n")
            val tokenSequence = Lexer(sourceCode)
            val errorManager = ErrorManager(lines, false)
            errorManagers[source] = errorManager
            val expressions = Parser(tokenSequence.tokenSequence, errorManager).parse()

            SymbolTableSingleton.currentIndex = source
            SymbolTableSingleton.fileSymbolTables[source] = FileSymbolTable()

            addFunctionsToMap(expressions, errorManager)
            expressionsList[source] = expressions
            i++
        }
        errorManagers.forEach { it.value.exitIfError() }

        i = 0
        val asmASTToIRService = ASTToIRService(ASMEmitter())
        for (expressions in expressionsList) {
            val file = File(expressions.key)
            val resultFile = File(file.absoluteFile.parentFile.absolutePath + "/" + file.nameWithoutExtension + ".asm")

            val errorManager = errorManagers[expressions.key]!!
            SymbolTableSingleton.currentIndex = expressions.key

            TypeChecker(expressions.value, errorManager).checkTypes()

            TreeRewriter(errorManager).rewriteMultiple(expressions.value)
            asmASTToIRService.asmEmitter = ASMEmitter()
            StatementCompiler(asmASTToIRService.asmEmitter, asmASTToIRService, libraryPath, errorManager).evaluateList(expressions.value)
            resultFile.writeText(asmASTToIRService.asmEmitter.finishedString)
            println("Wrote the resulting asm file to ${resultFile.path}")
            i++
        }
    }

    private fun addFunctionsToMap(expressions: List<Statement>, errorManager: ErrorManager) {
        expressions.filterIsInstance<Statement.FunctionDefinition>().forEach {
            val args = mapArgumentDefinitions(it.args)
            if (SymbolTableSingleton.getCurrentSymbolTable().functions.any { inner -> inner.name == it.name.substring }) {
                errorManager.error(it.name, "A function with this name has already been defined.")
            } else {
                SymbolTableSingleton.getCurrentSymbolTable().functions.add(Function(it.name.substring, args, it.type))
            }
        }
        expressions.filterIsInstance<Statement.StructDefinition>().forEach { structDefinition ->
            if (SymbolTableSingleton.getCurrentSymbolTable().structs.containsKey(structDefinition.name.substring)) {
                errorManager.error(structDefinition.name, "A struct with this name has already been defined.")
            } else {
                SymbolTableSingleton.getCurrentSymbolTable().structs[structDefinition.name.substring] =
                    Struct(structDefinition.name.substring, structDefinition.body.map { Pair(it.name.substring, it.type) })
            }
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