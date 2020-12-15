import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.interpreter.Interpreter
import com.cubearrow.cubelang.lexer.TokenGrammar
import com.cubearrow.cubelang.lexer.TokenSequence
import com.cubearrow.cubelang.parser.Parser
import com.cubearrow.cubelang.utils.ConsoleColor
import com.cubearrow.cubelang.utils.IOUtils.Companion.readAllText
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size == 1) {
        Main().compileFile(args[0])
    } else {
        println("No source file was provided")
        exitProcess(64)
    }
}
@ThreadLocal
var containsError = false
@ThreadLocal
var exitAfterError = false

class Main {
    companion object {
        const val useCompiler = true


        fun error(line: Int, index: Int, fullLine: String?, message: String) {
            if (fullLine != null) {
                val indicator = " ".repeat(index - 1) + "^"
                println(
                    """
                ${ConsoleColor.ANSI_RED}${fullLine}
                $indicator
                Error [$line:$index]: $message ${ConsoleColor.ANSI_WHITE}
            """.trimIndent()
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
    }


    fun compileFile(sourceFile: String) {
        val sourceCode = readAllText(sourceFile)
        val tokenGrammar = TokenGrammar(readAllText("src/nativeMain/resources/TokenGrammar.bnf"))

        val tokenSequence = TokenSequence(sourceCode, tokenGrammar)
        val expressions = Parser(tokenSequence.tokenSequence).parse()
        if (containsError)
            exitProcess(65)
        exitAfterError = true
        if (useCompiler) {
            Compiler(expressions, "src/nativeMain/resources/output.asm")
        } else {
            Interpreter(expressions)
        }
    }
}