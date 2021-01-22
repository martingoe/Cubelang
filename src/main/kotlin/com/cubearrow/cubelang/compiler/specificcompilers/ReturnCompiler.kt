package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class ReturnCompiler(var context: CompilerContext) : SpecificCompiler<Expression.ReturnStmnt> {
    override fun accept(expression: Expression.ReturnStmnt): String {
        if (expression.returnValue != null) {
            val moveInformation = context.moveExpressionToX(expression.returnValue)
            CompilerUtils.checkMatchingTypes(moveInformation.type, context.currentReturnType, -1, -1)
            var result = moveInformation.moveTo(CompilerUtils.getRegister("ax", moveInformation.type.getRawLength()))
            if (context.jmpOnReturn) {
                context.separateReturnSegment = true
                result += "jmp .L${context.lIndex + 1}\n"
            }
            return result
        }
        if (context.currentReturnType != null) {
            Main.error(-1, -1, "Expected a return value of the type ${context.currentReturnType}")
        }
        return ""
    }
}