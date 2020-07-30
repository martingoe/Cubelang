package com.cubearrow.cubelang.interpreter

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parser.Expression

class FunctionStorage {
    data class Function(val name: String, var args: List<String>, var body: List<Expression>)

    var functions = ArrayList<Function>()

    fun addFunction(name: Token, args: List<String>, body: List<Expression>){
        if(functions.stream().anyMatch{it.name == name.substring && it.args == args}){
            Main.error(name.line, name.index, null, "A function with the specified name and arguments already exists")
        } else{
            functions.add(Function(name.substring, args, body))
        }
    }
    fun getFunction(name:String, argsSize: Int): Function? = functions.stream().filter {it.name == name && it.args.size == argsSize}.findFirst().orElse(null)
}