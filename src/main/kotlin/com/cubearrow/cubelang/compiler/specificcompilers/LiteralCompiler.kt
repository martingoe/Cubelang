package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression

class LiteralCompiler(var context: CompilerContext):SpecificCompiler<Expression.Literal> {
    override fun accept(expression: Expression.Literal): String {
        if (expression.any1 is Int) {
            return expression.any1.toString()
        } else if (expression.any1 is Char) {
            return (expression.any1 as Char).toByte().toInt().toString()
        }
        TODO("Not yet implemented")
    }
}