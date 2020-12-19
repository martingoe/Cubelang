package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class LiteralCompiler(var context: CompilerContext):SpecificCompiler<Expression.Literal> {
    override fun accept(expression: Expression.Literal): String {
        if (expression.value is Int) {
            return expression.value.toString()
        } else if (expression.value is Char) {
            return expression.value.toByte().toInt().toString()
        }
        TODO("Not yet implemented")
    }
}