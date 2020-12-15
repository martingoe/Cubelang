package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.UsualErrorMessages

class GroupingCompiler(var context: CompilerContext): SpecificCompiler<Expression.Grouping> {
    override fun accept(expression: Expression.Grouping): String {
        return when (expression.expression1){
            is Expression.Literal -> "mov ${
                CompilerUtils.getRegister(
                    "ax",
                    Compiler.LENGTHS_OF_TYPES[ExpressionUtils.getType(
                        null,
                        (expression.expression1 as Expression.Literal).any1
                    )]!!
                )
            }"
            is Expression.VarCall -> {
                val varCall = expression.expression1 as Expression.VarCall
                val variable = context.variables.last()[varCall.identifier1.substring]
                if(variable == null)
                    UsualErrorMessages.xNotFound("variable", varCall.identifier1)
                context.operationResultSize = variable!!.length
                "mov ${CompilerUtils.getRegister("ax", variable.length)} ${varCall.accept(context.compilerInstance)}"}
            else -> expression.expression1.accept(context.compilerInstance)
        }
    }

}