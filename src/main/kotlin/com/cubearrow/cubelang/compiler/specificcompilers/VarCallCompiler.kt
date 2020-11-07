package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class VarCallCompiler(var context: CompilerContext): SpecificCompiler<Expression.VarCall> {
    override fun accept(expression: Expression.VarCall): String {
        val x: Compiler.LocalVariable? = context.variables.peek()[expression.identifier1.substring]
        if (x != null) {
            val lengthAsString = CompilerUtils.getASMPointerLength(x.length)
            return "$lengthAsString [rbp-${x.index}]"
        }
        Main.error(expression.identifier1.line, expression.identifier1.index, null,
                "The requested variable does not exist in the current scope.")
        return ""
    }
}