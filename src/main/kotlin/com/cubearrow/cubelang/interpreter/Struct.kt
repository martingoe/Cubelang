package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.Type


class Struct(override val name: String, private var classBody: List<Expression.VarInitialization>) :
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

    init {
        classBody.filterIsInstance<Expression.FunctionDefinition>().filter { it.name.substring == "init" }
            .forEach { ExpressionUtils.mapArgumentDefinitions(it.args) }
    }

    /**
     * Initialize the variables and function definitions defined in the instances of the class
     */
    fun initializeVariables(interpreter: Interpreter) {
        variableStorage.addScope()
        for (expression in classBody) {
            ExpressionUtils.computeVarInitialization(expression, variableStorage, interpreter)
        }
    }
}