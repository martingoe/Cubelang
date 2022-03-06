package com.cubearrow.cubelang.common.errors

import com.cubearrow.cubelang.common.ConsoleColor
import com.cubearrow.cubelang.common.tokens.Token
import kotlin.system.exitProcess

class ErrorManager(private val lines: List<String>, private var exitAfterError: Boolean) {
    private var containsError = false
    /**
     * Prints an error in the console while specifying the line and character index.
     *
     * If [exitAfterError] is active, this will exit the process.
     *
     * @param line The line at which the error is located
     * @param index The character index at which the error is located/starts
     * @param message The error message itself
     */
    fun error(line: Int, index: Int, message: String) {
        if (line >= 0 || index >= 0) {
            val indicator = " ".repeat(index) + "^"
            println(
                """${ConsoleColor.ANSI_RED}${lines[line - 1]}
                |$indicator 
                |Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimMargin()
            )
        } else {
            println(
                """
                ${ConsoleColor.ANSI_RED}Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimIndent()
            )
        }
        containsError = true
        if (exitAfterError)
            exitProcess(65)
    }
    fun error(token: Token, message: String){
        error(token.line, token.index, message)
    }

    fun exitIfError(){
        if(containsError)
            exitProcess(65)
    }
}