package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class AssignmentCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Assignment> {
    override fun accept(expression: Expression.Assignment): String {
        val variable = context.variables.peek()[expression.identifier1.substring]
        if (variable == null) {
            Main.error(expression.identifier1.line, expression.identifier1.index, null, "The variable \"${expression.identifier1.substring}\" is not defined")
            //Unreachable
            return ""
        }

        return when {
            expression.expression1 is Expression.Literal && (expression.expression1 as Expression.Literal).any1 is Int -> {
                "mov ${CompilerUtils.getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${expression.expression1.accept(context.compilerInstance)}"
            }
            expression.expression1 is Expression.VarCall -> {
                CompilerUtils.assignVariableToVariable(variable, context.variables.peek()[(expression.expression1 as Expression.VarCall).identifier1.substring]
                        ?: error("Variable not found"))
            }
            else -> {
                "${expression.expression1.accept(context.compilerInstance)} \n" +
                        "mov ${CompilerUtils.getASMPointerLength(variable.length)} [rbp - ${variable.index}], ${CompilerUtils.getRegister("ax", variable.length)}"
            }
        }
    }
}