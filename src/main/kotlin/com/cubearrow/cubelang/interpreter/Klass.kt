package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.parser.Expression


class Klass(override val name: String, expressionLst1: MutableList<Expression>, interpreter: Interpreter) : Callable {
    val functionStorage = FunctionStorage()
    val variableStorage = VariableStorage()
    override lateinit var args: List<String>
    override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage): ClassInstance {
        val instance = ClassInstance(functionStorage, variableStorage)
        this.functionStorage.getFunction("init", args.size)?.call(instance.variableStorage, instance.functionStorage)
        return instance
    }

    init {
        variableStorage.addScope()
        for (expression in expressionLst1) {
            if (expression is Expression.FunctionDefinition) {

                val expressionArgs = expression.expressionLst1.map { (it as Expression.VarCall).identifier1.substring }
                if (expression.identifier1.substring == "init") {
                    this.args = expressionArgs
                }

                functionStorage.addFunction(expression.identifier1, expressionArgs, expression.expressionLst2)
            }
            if (expression is Expression.VarInitialization) {

                variableStorage.addVariableToCurrentScope(expression.identifier1.substring, interpreter.evaluate(expression.expression1))
            }
        }
    }


}
