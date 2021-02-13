package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.PointerType

class ValueFromPointerCompiler(val context: CompilerContext): SpecificCompiler<Expression.ValueFromPointer> {
    override fun accept(expression: Expression.ValueFromPointer): String {
        val moveInformation = context.moveExpressionToX(expression.expression)
        if(moveInformation.type !is PointerType){
            context.error(-1, -1, "Expected a pointer type when getting the value from a type.")
            return ""
        }
        val pointerType = moveInformation.type
        context.operationResultType = pointerType.subtype

        return moveInformation.moveTo("rax") +
                "&${CompilerUtils.getASMPointerLength(pointerType.subtype.getRawLength())} [rax]"
    }
}