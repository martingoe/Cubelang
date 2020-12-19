package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import Main
import com.cubearrow.cubelang.parser.Expression

class ReturnCompiler (var context: CompilerContext): SpecificCompiler<Expression.ReturnStmnt>{
    override fun accept(expression: Expression.ReturnStmnt): String {
        if (context.currentReturnLength == null) {
            Main.error(-1, -1, null, "You cannot return from the current function or are not in one.")
            return ""
        }

        if (expression.returnValue is Expression.Literal || expression.returnValue is Expression.VarCall) {
            return "mov ${CompilerUtils.getRegister("ax", context.currentReturnLength!!)}, " +
                    expression.returnValue.accept(context.compilerInstance)
        }
        return expression.returnValue?.accept(context.compilerInstance) ?: ""
    }
}