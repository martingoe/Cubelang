package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ArrayType
import com.cubearrow.cubelang.utils.CommonErrorMessages
import com.cubearrow.cubelang.utils.PointerType

class PointerGetCompiler(val context: CompilerContext): SpecificCompiler<Expression.PointerGet> {
    override fun accept(expression: Expression.PointerGet): String {
        val string = expression.varCall.accept(context.compilerInstance)
        val variable = context.getVariable(expression.varCall.varName.substring)
        if (variable == null) {
            CommonErrorMessages.xNotFound("requested variable '${expression.varCall.varName.substring}'", expression.varCall.varName)
            return ""
        }
        setOperationType(variable)
        return "lea rax, ${string.substring(string.indexOf("["))}"
    }

    private fun setOperationType(variable: Compiler.LocalVariable) {
        if (variable.type is ArrayType) {
            context.operationResultType = PointerType((variable.type as ArrayType).subType)
        } else {
            context.operationResultType = PointerType(variable.type)
        }
    }
}