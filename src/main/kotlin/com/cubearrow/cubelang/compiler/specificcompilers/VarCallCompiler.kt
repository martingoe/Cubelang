package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.UsualErrorMessages

class VarCallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarCall> {
    override fun accept(expression: Expression.VarCall): String {
        val x: Compiler.LocalVariable? = context.variables.peek()[expression.identifier1.substring]
        if (x != null) {
            val lengthAsString = CompilerUtils.getASMPointerLength(x.length)
            return "$lengthAsString [rbp-${x.index}]"
        }
        UsualErrorMessages.xNotFound("requested variable", expression.identifier1)
        return ""
    }
}