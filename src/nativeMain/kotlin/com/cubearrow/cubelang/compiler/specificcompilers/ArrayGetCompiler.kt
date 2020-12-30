package com.cubearrow.cubelang.compiler.specificcompilers

import Main
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getTokenFromArrayGet
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ArrayType
import com.cubearrow.cubelang.utils.CommonErrorMessages
import com.cubearrow.cubelang.utils.Type

class ArrayGetCompiler(val context: CompilerContext) : SpecificCompiler<Expression.ArrayGet> {
    override fun accept(expression: Expression.ArrayGet): String {
        val variable = CompilerUtils.getVariableFromArrayGet(expression, context)
        if(variable == null){
            CommonErrorMessages.xNotFound("requested array-variable", getTokenFromArrayGet(expression))
            return ""
        }
        if (expression.expression is Expression.VarCall && expression.inBrackets is Expression.Literal) {
            val index = variable.index - getIndex(expression, variable.type, variable.type.getRawLength())
            context.operationResultType = (variable.type as ArrayType).subType
            return "[rbp - $index]"
        } else if (expression.expression is Expression.VarCall) {
            val triple = CompilerUtils.moveExpressionToX(expression.inBrackets, context)
            context.operationResultType = (variable.type as ArrayType).subType
            return """
                        |movsx rbx, ${triple.second}
                        |[rbp-${variable.index}+rbx*${variable.type.getRawLength()}]
                    """.trimMargin()
        }
        Main.error(-1, -1, "Unable to compile the requested type of array access. This may be changed in the future.")
        TODO()
    }



    private fun getIndex(expression: Expression, type: Type, rawLength: Int): Int {
        if (expression is Expression.ArrayGet) {
            if (expression.inBrackets is Expression.Literal) {
                val literalValue = expression.inBrackets.value
                if (literalValue is Int) {
                    return getIndex(expression.expression, (type as ArrayType).subType, rawLength) * type.count + literalValue * rawLength
                }
            }
        } else if (expression is Expression.VarCall) {
            return 0
        }
        return -1
    }

}