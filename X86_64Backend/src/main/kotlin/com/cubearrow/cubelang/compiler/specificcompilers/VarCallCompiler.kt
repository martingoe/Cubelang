package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.CommonErrorMessages
import com.cubearrow.cubelang.compiler.utils.TypeUtils

class VarCallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.VarCall> {
    override fun accept(expression: Expression.VarCall): String {
        val x: Compiler.LocalVariable? = context.getVariable(expression.varName.substring)
        if (x != null) {
            val lengthAsString = CompilerUtils.getASMPointerLength(TypeUtils.getRawLength(x.type))
            return "$lengthAsString [rbp-${x.index}]"
        }
        CommonErrorMessages.xNotFound("requested variable", expression.varName, context)
        return ""
    }
}