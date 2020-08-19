package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.parser.Expression


class Klass(override val name: String, var classBody: MutableList<Expression>) : Callable {
    private val functionStorage = FunctionStorage()
    private val variableStorage = VariableStorage()
    override var args: List<String> = listOf()
    override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage): ClassInstance {
        val tempVariableStorage = VariableStorage()
        tempVariableStorage.addScope()
        tempVariableStorage.addVariablesToCurrentScope(variableStorage.getCurrentVariables())
        tempVariableStorage.addVariablesToCurrentScope(this.variableStorage.getCurrentVariables())
        val tempFunctionStorage = FunctionStorage()
        tempFunctionStorage.addFunctions(functionStorage.functions)
        tempFunctionStorage.addFunctions(this.functionStorage.functions)

        val instance = ClassInstance(functionStorage, tempVariableStorage)
        this.functionStorage.getFunction("init", args.size)?.call(instance.variableStorage, instance.functionStorage)
        return instance
    }
    init{
        classBody.filterIsInstance<Expression.FunctionDefinition>().filter { it.identifier1.substring == "init" }
                .forEach() {this.args = it.expressionLst1.map { (it as Expression.VarCall).identifier1.substring}}
    }

    fun initializeVariables(interpreter: Interpreter) {
        variableStorage.addScope()
        for (expression in classBody) {
            if (expression is Expression.FunctionDefinition) {

                val expressionArgs = expression.expressionLst1.map { (it as Expression.VarCall).identifier1.substring }

                functionStorage.addFunction(expression.identifier1, expressionArgs, expression.expressionLst2)
            }
            if (expression is Expression.VarInitialization) {
                variableStorage.addVariableToCurrentScope(expression.identifier1.substring, interpreter.evaluate(expression.expression1))
            }
        }
    }


}
