package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.ArrayType
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.Type
import com.cubearrow.cubelang.parser.Expression

class ArrayGetCompiler(val context: CompilerContext): SpecificCompiler<Expression.ArrayGet> {
    override fun accept(expression: Expression.ArrayGet): String {
        if (expression.expression2 is Expression.Literal) {
            val variable = CompilerUtils.getVariableFromArrayGet(expression, context)
            val index = variable!!.index - getIndex(expression, variable.type, variable.type.getRawLength())
            return "[rbp - $index]"
        } else if (expression.expression is Expression.VarCall) {
            if (expression.expression2 is Expression.VarCall) {
                val variable = CompilerUtils.getVariableFromArrayGet(expression.expression2, context)
                val arrayVariable = CompilerUtils.getVariableFromArrayGet(expression, context)
                if (variable != null) {
                    val register = CompilerUtils.getRegister("ax", variable.type.getRawLength())
                    return """mov $register, ${expression.expression2.accept(context.compilerInstance)}
                        |mov rbx, rax
                        |[rbp-${arrayVariable!!.index}+rbx*${arrayVariable.type.getRawLength()}]
                    """.trimMargin()
                }
            }
        }
        Main.error(-1, -1, null, "Unable to compile the requested type of array access. This may be changed in the future.")
        TODO()
    }

    private fun getIndex(expression: Expression, type: Type, rawLength: Int): Int {
        if (expression is Expression.ArrayGet) {
            if (expression.expression2 is Expression.Literal) {
                val literalValue = expression.expression2.any
                if (literalValue is Int) {
                    return getIndex(expression.expression, (type as ArrayType).subType, rawLength) * type.count + literalValue * rawLength
                }
            }
        } else if (expression is Expression.VarCall) {
            return 0
        }
        return -1
    }

}