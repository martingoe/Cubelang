package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class LiteralCompiler(var context: CompilerContext):SpecificCompiler<Expression.Literal> {
    override fun accept(expression: Expression.Literal): String {
        if (expression.any is Int) {
            return expression.any.toString()
        } else if (expression.any is Char) {
            return expression.any.toByte().toInt().toString()
        }
        TODO("Not yet implemented")
    }
}