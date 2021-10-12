package com.cubearrow.cubelang.tests

import com.cubearrow.cubelang.main.Main
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun getResultOfTest(name: String): Pair<String?, String?>{
    val pathString = "src/test/resources/testsources/$name"
    val pathFile = File(pathString)
    Main("src/test/resources/library").compileFile(arrayOf("$pathString/source.cube"))

    "nasm -f elf64 source.asm ".runCommand(pathFile)
    "gcc source.o -no-pie".runCommand(pathFile)
    return Pair("./a.out".runCommand(pathFile), File("$pathString/expectedResult.txt").readText())
}
fun String.runCommand(workingDir: File): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}