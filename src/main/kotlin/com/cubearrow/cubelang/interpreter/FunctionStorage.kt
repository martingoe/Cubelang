package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.library.PrintingLibrary
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class FunctionStorage {
    class Function(override val name: String, override var args: List<String>, var body: List<Expression>): Callable {
        override fun call(variableStorage: VariableStorage, functionStorage: FunctionStorage): Any? {
            return Interpreter(this.body, variableStorage, functionStorage).returnedValue
        }
    }

    var functions = ArrayList<Callable>()

    fun addFunction(name: Token, args: List<String>, body: List<Expression>){
        if(functions.stream().anyMatch{it.name == name.substring && it.args == args}){
            Main.error(name.line, name.index, null, "A function with the specified name and arguments already exists")
        } else{
            functions.add(Function(name.substring, args, body))
        }
    }
    fun getFunction(name:String, argsSize: Int): Callable? = functions.stream().filter {it.name == name && it.args.size == argsSize}.findFirst().orElse(null)

    init{
        functions.add(PrintingLibrary.Println())
    }
}