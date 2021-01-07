package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class ReturnCompiler (var context: CompilerContext): SpecificCompiler<Expression.ReturnStmnt>{
    override fun accept(expression: Expression.ReturnStmnt): String {
        if(expression.returnValue != null) {
            val (before, pointer, type) = context.moveExpressionToX(expression.returnValue)
            CompilerUtils.checkMatchingTypes(type, context.currentReturnType, -1, -1)
            return if (before.isNotBlank()) before + "\n" else "" +
                    if(!CompilerUtils.isAXRegister(pointer)) "mov ${CompilerUtils.getRegister("ax", type.getRawLength())}, $pointer" else ""
        }
        if(context.currentReturnType != null){
            Main.error(-1, -1, "Expected a return value of the type ${context.currentReturnType}")
        }
        return ""
    }
}