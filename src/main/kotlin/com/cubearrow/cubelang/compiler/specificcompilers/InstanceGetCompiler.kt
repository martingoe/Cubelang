package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.NormalType

class InstanceGetCompiler(val context: CompilerContext): SpecificCompiler<Expression.InstanceGet> {
    override fun accept(expression: Expression.InstanceGet): String {
        val variable = context.getVariable((expression.expression as Expression.VarCall).varName.substring)!!
        val struct = context.structs[(variable.type as NormalType).typeName]!!
        val requestedVar = struct.vars.first { pair -> pair.first == expression.identifier.substring }
        val argumentsBefore = struct.vars.subList(
            0, struct.vars.indexOf(requestedVar))
        val index = variable . index - argumentsBefore.fold(0, {acc, pair -> acc + pair.second.getLength() })
        context.operationResultType = requestedVar.second
        return "${CompilerUtils.getASMPointerLength(requestedVar.second.getRawLength())} [rbp-${index}]"
    }
}