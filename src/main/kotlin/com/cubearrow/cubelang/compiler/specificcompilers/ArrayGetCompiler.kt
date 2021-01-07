package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getASMPointerLength
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.CompilerUtils.Companion.getTokenFromArrayGet
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.*

class ArrayGetCompiler(val context: CompilerContext) : SpecificCompiler<Expression.ArrayGet> {
    override fun accept(expression: Expression.ArrayGet): String {
        val variable = context.getVariableFromArrayGet(expression)
        if(variable == null){
            CommonErrorMessages.xNotFound("requested array-variable", getTokenFromArrayGet(expression))
            return ""
        }
        if(variable.type is ArrayType) {
            return getArrayType(expression, variable, variable.type as ArrayType)
        } else if(variable.type is PointerType && expression.inBrackets is Expression.Literal){
            return getPointerType(expression, variable.type as PointerType)
        }
        Main.error(-1, -1, "Unable to compile the requested type of array access. This may be changed in the future.")
        TODO()
    }

    private fun getPointerType(expression: Expression.ArrayGet, type: PointerType): String {
        val firstTriple = context.moveExpressionToX(expression.expression)
        context.operationResultType = type.normalType
        return (if (firstTriple.first.isNotBlank()) "${firstTriple.first}\n" else "") +
                (if (!CompilerUtils.isAXRegister(firstTriple.second)) "mov rax, ${firstTriple.second}\n" else "") +
                "mov ${
                    getRegister(
                        "ax",
                        type.normalType.getRawLength()
                    )
                }, ${getASMPointerLength(type.normalType.getRawLength())} [rax+${expression.inBrackets.accept(context.compilerInstance)}]"
    }

    private fun getArrayType(
        expression: Expression.ArrayGet,
        variable: Compiler.LocalVariable,
        type: ArrayType
    ): String {
        context.operationResultType = type.subType
        if (expression.expression is Expression.VarCall && expression.inBrackets is Expression.Literal) {
            val index = variable.index - getIndex(expression, variable.type, variable.type.getRawLength())
            return "mov ${getRegister("ax", type.subType.getRawLength())}[rbp - $index]"
        } else if (expression.expression is Expression.VarCall) {
            val triple = context.moveExpressionToX(expression.inBrackets)
            return """movsx rbx, ${triple.second}
                            |mov ${
                getRegister(
                    "ax",
                    type.getRawLength()
                )
            }, ${getASMPointerLength(type.getRawLength())} [rbp-${variable.index}+rbx*${variable.type.getRawLength()}]
                        """.trimMargin()
        }
        TODO()
    }

    private fun getIndex(expression: Expression, type: Type, rawLength: Int): Int {
        if (expression is Expression.ArrayGet) {
            if (expression.inBrackets is Expression.Literal) {
                val literalValue = expression.inBrackets.value
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