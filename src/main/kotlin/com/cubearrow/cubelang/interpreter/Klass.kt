package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.utils.Type
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils


class Klass(override val name: String, private var inheritsFrom: Klass?, private var classBody: List<Expression>) :
    Callable {
    private val functionStorage = FunctionStorage()
    private val variableStorage = VariableStorage()
    override var args: Map<String, Type> = mapOf()

    /**
     * Creates an instance of the Klass
     */
    override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage): ClassInstance {
        //Initialize both the variable and the function storage of the instance
        val tempVariableStorage = VariableStorage()
        tempVariableStorage.addScope()
        tempVariableStorage.addVariablesToCurrentScope(variableStorage.getCurrentVariables())
        tempVariableStorage.addVariablesToCurrentScope(this.variableStorage.getCurrentVariables())

        val tempFunctionStorage = FunctionStorage()
        tempFunctionStorage.addFunctions(functionStorage.functions)
        tempFunctionStorage.addFunctions(this.functionStorage.functions)

        val instance = ClassInstance(tempFunctionStorage, tempVariableStorage, name)
        this.functionStorage.getFunction("init", args.size)?.call(instance.variableStorage, instance.functionStorage)
        return instance
    }
    init{
        classBody.filterIsInstance<Expression.FunctionDefinition>().filter { it.name.substring == "init" }
                .forEach {ExpressionUtils.mapArgumentDefinitions(it.args)}
    }

    /**
     * Initialize the variables and function definitions defined in the instances of the class
     */
    fun initializeVariables(interpreter: Interpreter) {
        variableStorage.addScope()
        if(inheritsFrom != null){
            functionStorage.addFunctions(inheritsFrom!!.functionStorage.functions)
            variableStorage.addVariablesToCurrentScope(inheritsFrom!!.variableStorage.getCurrentVariables())
        }
        for (expression in classBody) {
            if (expression is Expression.FunctionDefinition) {
                val expressionArgs = expression.args.map { (it as Expression.ArgumentDefinition).name.substring to it.type }.toMap()
                this.functionStorage.removeFunction(expression.name, expressionArgs)
                this.functionStorage.addFunction(expression.name, expressionArgs, expression.body)
            }
            if (expression is Expression.VarInitialization) {
                ExpressionUtils.computeVarInitialization(expression, variableStorage, interpreter)
            }
        }
    }
}
