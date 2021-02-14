package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.TypeUtils

class ReturnCompiler(var context: CompilerContext) : SpecificCompiler<Expression.ReturnStmnt> {
    override fun accept(expression: Expression.ReturnStmnt): String {
        if (expression.returnValue != null) {
            val moveInformation = context.moveExpressionToX(expression.returnValue!!)
            CompilerUtils.checkMatchingTypes(moveInformation.type, context.currentReturnType, -1, -1, context)
            var result = moveInformation.moveTo(CompilerUtils.getRegister("ax", TypeUtils.getRawLength(moveInformation.type)))
            if (context.jmpOnReturn) {
                context.separateReturnSegment = true
                result += "jmp .L${context.lIndex + 1}\n"
            }
            return result
        }
        if (context.currentReturnType != null) {
            context.error(-1, -1, "Expected a return value of the type ${context.currentReturnType}")
        }
        return ""
    }
}