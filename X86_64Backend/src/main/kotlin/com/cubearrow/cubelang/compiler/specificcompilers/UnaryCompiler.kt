package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.tokens.TokenType
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.TypeUtils

class UnaryCompiler(val context: CompilerContext): SpecificCompiler<Expression.Unary>{
    override fun accept(expression: Expression.Unary): String {
        if(expression.identifier.tokenType == TokenType.PLUSMINUS && expression.identifier.substring == "-"){
            val moveTo = context.moveExpressionToX(expression.expression)
            val register = CompilerUtils.getRegister("ax", TypeUtils.getRawLength(moveTo.type))
            val moveString = moveTo.moveTo(register)
            context.operationResultType = moveTo.type
            return "$moveString\nneg $register"
        }
        TODO("Not yet implemented")
    }

}