package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.utils.Type
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.interpreter.library.Library
import com.cubearrow.cubelang.parser.Expression

/**
 * Stores the instances of [Callable] defined in a program
 */
class FunctionStorage {
    class Function(override val name: String, override var args: Map<String, Type>, private var body: Expression) :
        Callable {
        override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage): Any? {
            return Interpreter(listOf(this.body), variableStorage, functionStorage).returnedValue
        }
    }

    var functions = ArrayList<Callable>()

    /**
     * Adds a single [Function] to the functions
     *
     * @param name The name of the function, a [Token] is used to throw a potential error at the correct location
     * @param args The arguments the function will take
     * @param body The [List] of [Expression] representing the body
     */
    fun addFunction(name: Token, args: Map<String, Type>, body: Expression) {
        if (functions.any { it.name == name.substring && it.args == args }) {
            Main.error(name.line, name.index, "A function with the specified name and argument size already exists")
        } else {
            functions.add(Function(name.substring, args, body))
        }
    }

    /**
     * Adds a single [Callable] to the functions
     *
     * @param callable The callable to be added
     */
    fun addFunction(callable: Callable) {
        if (functions.any { it.name == callable.name && it.args == callable.args }) {
            Main.error(-1, -1, "A function with the specified name and argument size already exists")
        } else {
            functions.add(callable)
        }
    }

    /**
     * Returns the first found function which matches the name and argument size. If no function matches, null is returned
     *
     * @param name The name of the function
     * @param argsSize The number of arguments of the function
     */
    fun getFunction(name: String, argsSize: Int): Callable? =
        functions.firstOrNull { it.name == name && it.args.size == argsSize }

    /**
     * Adds multiple [Callable]s to the function while ensuring no duplicates
     *
     * @param callables The callables to be added
     */
    fun addFunctions(callables: List<Callable>) {
        val result = ArrayList<Callable>()
        result.addAll(callables)

        for(callable in callables){
            if(result.all { it.name != callable.name && it.args != callable.args }){
                result.add(callable)
            }
        }
        this.functions = result
    }

    /**
     * Removes the first matching function with the provided name and arguments-
     *
     * @param name The name of the function
     * @param arguments The arguments of the function
     */
    fun removeFunction(name: Token, arguments: Map<String, Type>) {
        functions.remove(functions.firstOrNull { it.name == name.substring && it.args == arguments })
    }

    init {
        functions.addAll(Library().classes)
    }
}