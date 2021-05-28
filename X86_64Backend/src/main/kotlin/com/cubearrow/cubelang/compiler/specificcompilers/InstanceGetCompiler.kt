package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.compiler.utils.TypeUtils

/**
 * Compiles getting from a struct instance.
 *
 * @param context The needed [CompilerContext].
 */
class InstanceGetCompiler(val context: CompilerContext): SpecificCompiler<Expression.InstanceGet> {
    override fun accept(expression: Expression.InstanceGet): String {
        val varCall  = expression.expression as Expression.VarCall
        val variable = context.getVariable(varCall.varName.substring)
        if(variable == null){
            context.error(varCall.varName.line, varCall.varName.index, "The requested variable '${varCall.varName.substring}' does not exist.")
            return ""
        }
        val struct = context.structs[(variable.type as NormalType).typeName]!!
        val requestedVar = struct.vars.first { pair -> pair.first == expression.identifier.substring }
        val argumentsBefore = struct.vars.subList(0, struct.vars.indexOf(requestedVar))
        val index = variable.index - argumentsBefore.fold(0) { acc, pair -> acc + TypeUtils.getLength(pair.second) }
        context.operationResultType = requestedVar.second
        return "${CompilerUtils.getASMPointerLength(TypeUtils.getRawLength(requestedVar.second))} [rbp-${index}]"
    }
}