package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.common.ArrayType
import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.utils.CommonErrorMessages
import com.cubearrow.cubelang.common.PointerType

class PointerGetCompiler(val context: CompilerContext): SpecificCompiler<Expression.PointerGet> {
    override fun accept(expression: Expression.PointerGet): String {
        val variable = context.getVariable(expression.varCall.varName.substring)
        if (variable == null) {
            CommonErrorMessages.xNotFound("requested variable '${expression.varCall.varName.substring}'", expression.varCall.varName, context)
            return ""
        }
        setOperationType(variable)
        return "lea rax, ${context.getVariablePointer(variable)}"
    }

    private fun setOperationType(variable: Compiler.LocalVariable) {
        if (variable.type is ArrayType) {
            context.operationResultType = PointerType((variable.type as ArrayType).subType)
        } else {
            context.operationResultType = PointerType(variable.type)
        }
    }
}