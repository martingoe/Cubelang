package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class ReturnCompiler (var context: CompilerContext): SpecificCompiler<Expression.ReturnStmnt>{
    override fun accept(expression: Expression.ReturnStmnt): String {
        if (context.currentReturnLength == null) {
            Main.error(-1, 1, null, "You cannot return from the current function or are not in one.")
            return ""
        }

        if (expression.expression1 is Expression.Literal || expression.expression1 is Expression.VarCall) {
            return "mov ${CompilerUtils.getRegister("ax", context.currentReturnLength!!)}, " +
                    expression.expression1.accept(context.compilerInstance)
        }
        return expression.expression1.accept(context.compilerInstance)
    }
}