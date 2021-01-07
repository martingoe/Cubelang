package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getTokenFromArrayGet
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ArrayType
import com.cubearrow.cubelang.utils.CommonErrorMessages

class ArraySetCompiler(val context: CompilerContext) : SpecificCompiler<Expression.ArraySet> {
    override fun accept(expression: Expression.ArraySet): String {
        val variable = context.getVariableFromArrayGet(expression.arrayGet)
        val (before, pointer, type) = context.moveExpressionToX(expression.arrayGet)
        if (variable == null) {
            Main.error(-1, -1, "Could not find the requested array-variable.")
            return ""
        }
        val token = getTokenFromArrayGet(expression.arrayGet)
        return when (expression.value) {
            is Expression.VarCall,
            is Expression.ArrayGet -> {
                val localVariable = context.getVariableFromArrayGet(expression.value)
                if (localVariable != null) {
                    checkMatchingTypes((variable.type as ArrayType).subType, localVariable.type, token.line, token.index)
                    val length = localVariable.type.getRawLength()

                    val register = CompilerUtils.getRegister("ax", length)

                    return """$before
                        |mov $register, ${CompilerUtils.getASMPointerLength(length)} [rbp - ${localVariable.index}]
                        |mov ${CompilerUtils.getASMPointerLength(length)} ${pointer}, $register""".trimMargin()
                }
                CommonErrorMessages.xNotFound("variable", getTokenFromArrayGet(expression.value))
                ""
            }
            else -> {
                val triple = context.moveExpressionToX(expression.value)
                checkMatchingTypes(triple.third, type, token.line, token.index)
                if (before.isNotBlank()) "$before\n" else "" + if (triple.first.isNotBlank()) "${triple.first}\n" else "" + "mov ${
                    CompilerUtils.getASMPointerLength(
                        variable.type.getRawLength()
                    )
                } $pointer, ${triple.second}"
            }
        }
    }
}