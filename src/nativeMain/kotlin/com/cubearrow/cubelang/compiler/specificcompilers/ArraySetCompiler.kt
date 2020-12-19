package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.UsualErrorMessages

class ArraySetCompiler(val context: CompilerContext): SpecificCompiler<Expression.ArraySet> {
    override fun accept(expression: Expression.ArraySet): String {
        val variable = CompilerUtils.getVariableFromArrayGet(expression.arrayGet, context)
        val (before, pointer) = CompilerUtils.beforeAndPointerArrayGet(expression.arrayGet, context)
        if(variable == null){
            Main.error(-1, -1, null, "Could not find the requested array-variable.")
            return ""
        }
        return when (expression.value) {
            is Expression.Literal -> {
                "$before\nmov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} $pointer" +
                        ", ${expression.value.accept(context.compilerInstance)}"
            }
            is Expression.VarCall -> {
                val varCall = expression.value
                val localVariable = context.variables.last()[varCall.varName.substring]
                if (localVariable != null) {
                    val length = localVariable.type.getRawLength()

                    val register = CompilerUtils.getRegister("ax", length)

                    return """$before
                        |mov $register, ${CompilerUtils.getASMPointerLength(length)} [rbp - ${localVariable.index}]
                        |mov ${CompilerUtils.getASMPointerLength(length)} ${pointer}, $register""".trimMargin()
                }
                UsualErrorMessages.xNotFound("variable", varCall.varName)
                ""
            }
            else -> {
                "$before\n${expression.value.accept(context.compilerInstance)} \n" +
                        "mov ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())} $pointer, ${
                            CompilerUtils.getRegister(
                                "ax",
                                variable.type.getRawLength()
                            )
                        }"
            }
        }
    }
}