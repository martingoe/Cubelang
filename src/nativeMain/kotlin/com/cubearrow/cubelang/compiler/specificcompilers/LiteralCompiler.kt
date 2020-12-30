package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.NormalType

class LiteralCompiler(var context: CompilerContext):SpecificCompiler<Expression.Literal> {
    override fun accept(expression: Expression.Literal): String {
        if (expression.value is Int) {
            context.operationResultType = NormalType("int")
            return expression.value.toString()
        } else if (expression.value is Char) {
            context.operationResultType = NormalType("char")
            return expression.value.toByte().toInt().toString()
        }
        TODO("Not yet implemented")
    }
}