package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils


class Klass(override val name: String, private var inheritsFrom: Klass?, private var classBody: MutableList<Expression>) : Callable {
    private val functionStorage = FunctionStorage()
    private val variableStorage = VariableStorage()
    override var args: List<String> = listOf()

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

        val instance = ClassInstance(tempFunctionStorage, tempVariableStorage)
        this.functionStorage.getFunction("init", args.size)?.call(instance.variableStorage, instance.functionStorage)
        return instance
    }
    init{
        classBody.filterIsInstance<Expression.FunctionDefinition>().filter { it.identifier1.substring == "init" }
                .forEach() {this.args = ExpressionUtils.mapVarCallsToStrings(it.expressionLst1)}
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
                val expressionArgs = expression.expressionLst1.map { (it as Expression.VarCall).identifier1.substring }
                this.functionStorage.removeFunction(expression.identifier1, expressionArgs)
                this.functionStorage.addFunction(expression.identifier1, expressionArgs, expression.expressionLst2)
            }
            if (expression is Expression.VarInitialization) {
                variableStorage.addVariableToCurrentScope(expression.identifier1.substring, interpreter.evaluate(expression.expression1))
            }
        }
    }
}
