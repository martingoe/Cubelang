package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.MoveInformation
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ArrayType
import com.cubearrow.cubelang.utils.CommonErrorMessages


/**
 *  Compiles setting a value to an array or a pointer element.
 *
 * @param context The needed [CompilerContext].
 */
class ArraySetCompiler(val context: CompilerContext) : SpecificCompiler<Expression.ArraySet> {
    override fun accept(expression: Expression.ArraySet): String {
        val variable = context.getVariableFromArrayGet(expression.arrayGet)
        val arrayGetMoveInformation = context.moveExpressionToX(expression.arrayGet)
        if (variable == null) {
            Main.error(-1, -1, "Could not find the requested array-variable.")
            return ""
        }
        val token = CompilerUtils.getTokenFromArrayGet(expression.arrayGet)
        return when (expression.value) {
            is Expression.VarCall,
            is Expression.ArrayGet -> setVariableToArray(expression, variable, token, arrayGetMoveInformation)
            else -> {
                val moveInformation = context.moveExpressionToX(expression.value)
                CompilerUtils.checkMatchingTypes(moveInformation.type, arrayGetMoveInformation.type, token.line, token.index)
                return arrayGetMoveInformation.before + moveInformation.moveTo(CompilerUtils.getASMPointerLength(variable.type.getRawLength()) + arrayGetMoveInformation.pointer)
            }
        }
    }

    private fun setVariableToArray(
        expression: Expression.ArraySet,
        variable: Compiler.LocalVariable,
        token: Token,
        arrayGetMoveInformation: MoveInformation
    ): String {
        val localVariable = context.getVariableFromArrayGet(expression.value)
        if (localVariable != null) {
            CompilerUtils.checkMatchingTypes((variable.type as ArrayType).subType, localVariable.type, token.line, token.index)
            val length = localVariable.type.getRawLength()
            val register = CompilerUtils.getRegister("ax", length)

            return """${arrayGetMoveInformation.before}
                            |mov $register, ${CompilerUtils.getASMPointerLength(length)} [rbp - ${localVariable.index}]
                            |mov ${CompilerUtils.getASMPointerLength(length)} ${arrayGetMoveInformation.pointer}, $register""".trimMargin()
        }
        CommonErrorMessages.xNotFound("variable", token)
        return ""
    }
}