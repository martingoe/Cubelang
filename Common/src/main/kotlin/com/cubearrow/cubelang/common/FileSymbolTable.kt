package com.cubearrow.cubelang.common

import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.common.definitions.Struct
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object SymbolTableSingleton {
    var currentIndex = 0
    var fileSymbolTables: MutableList<FileSymbolTable> = ArrayList()
    fun getCurrentSymbolTable(): FileSymbolTable = fileSymbolTables[currentIndex]
    fun resetAll() {
        currentIndex = 0
        fileSymbolTables = ArrayList()

    }
}

class FileSymbolTable {
    init {
        println("Singleton class invoked")
    }

    var structs: HashMap<String, Struct> = HashMap()
    var functions: MutableList<Function> = ArrayList()
    var variables: Node = Scope(ArrayList())

    fun getVariablesInCurrentScope(scope: Stack<Int>): List<VarNode> {
        val scopeClone: Stack<Int> = scope.clone() as Stack<Int>
        var currentNode = variables
        val accessibleVariables: MutableList<VarNode> = ArrayList()

        while (scopeClone.size > 1) {
            currentNode = (currentNode as Scope).symbols.filterIsInstance<Scope>()[scopeClone.removeAt(0)]

            accessibleVariables.addAll(currentNode.symbols.filterIsInstance(VarNode::class.java))
        }
        return accessibleVariables
    }

    fun getVariablesOffsetDefinedAtScope(scope: Stack<Int>): Int {
        val node = getNodeAtScope(scope)
        return getOffsetAtNode(node)
    }

    private fun getOffsetAtNode(node: Node): Int {
        return when (node) {
            is VarNode -> node.type.getLength()
            is Scope -> node.symbols.fold(0) { acc, node -> acc + getOffsetAtNode(node) }
            else -> error("Unreachable")
        }
    }


    fun getStruct(name: String): Struct? = structs[name]
    fun defineVariable(scope: Stack<Int>, name: String, type: Type, offset: Int) {
        val currentNode = getNodeAtScope(scope)
        (currentNode as Scope).symbols.add(VarNode(name, type, offset))
    }

    private fun getNodeAtScope(scope: Stack<Int>): Node {
        val scopeClone: Stack<Int> = scope.clone() as Stack<Int>
        var currentNode = variables
        while (scopeClone.size > 1) {
            currentNode = (currentNode as Scope).symbols.filterIsInstance<Scope>()[scopeClone.removeAt(0)]
        }
        return currentNode
    }

    fun addScopeAt(scope: Stack<Int>) {
        val currentNode = getNodeAtScope(scope)
        (currentNode as Scope).symbols.add(Scope(ArrayList()))
    }


}

open class Node
class VarNode(val name: String, val type: Type, val offset: Int) : Node()
class Scope(val symbols: MutableList<Node>) : Node()