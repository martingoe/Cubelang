package com.martingoe.cubelang.test.integration

import com.martingoe.cubelang.common.SymbolTableSingleton
import com.martingoe.cubelang.main.Main
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Compiles the test suite with the given name, runs it and returns the output
 */
fun getResultOfTest(name: String): Pair<String?, String?> {
    val pathString = "src/test/resources/testsources/$name"
    val pathFile = File(pathString)
    SymbolTableSingleton.resetAll()
    val files = pathFile.walkTopDown().asIterable().filter { it.name.endsWith(".cube") }.map { it.path }.toTypedArray()
    Main("src/test/resources/library").compileFile(files)

    "nasm -f elf64 source.asm ".runCommand(pathFile)
    "gcc source.o -no-pie".runCommand(pathFile)
    return Pair("./a.out".runCommand(pathFile), File("$pathString/expectedResult.txt").readText())
}

/**
 * Runs a process using [[Process]].
 */
fun String.runCommand(workingDir: File): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(5, TimeUnit.MINUTES)
        proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}