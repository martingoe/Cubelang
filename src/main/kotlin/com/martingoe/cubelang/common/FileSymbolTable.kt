package com.martingoe.cubelang.common

import com.martingoe.cubelang.common.definitions.Function
import com.martingoe.cubelang.common.definitions.Struct
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A singleton holding all information about variable names etc.
 * This saves a [[FileSymbolTable]] for every file.
 */
object SymbolTableSingleton {
    lateinit var currentIndex: String
    var fileSymbolTables: MutableMap<String, FileSymbolTable> = HashMap()
    fun getCurrentSymbolTable(): FileSymbolTable = fileSymbolTables[currentIndex]!!
    fun resetAll() {
        currentIndex = ""
        fileSymbolTables = HashMap()
    }
}

/**
 * Saves all defined structs, functions, and variables
 */
class FileSymbolTable {
    var structs: HashMap<String, Struct> = HashMap()
    var functions: MutableList<Function> = ArrayList()
    var variables: Node = Scope(ArrayList())
    var stringLiterals: MutableMap<String, Int> = HashMap()
    var currentStringLiteralIndex = 0

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

    /**
     * Returns the summed offset of all variables defined in the given scope.
     */
    fun getVariablesOffsetDefinedAtScope(scope: Stack<Int>): Int {
        val node = getNodeAtScope(scope)
        return getOffsetAtNode(node)
    }

    private fun getOffsetAtNode(node: Node): Int {
        return when (node) {
            is VarNode -> node.type.getLength()
            is Scope -> node.symbols.fold(0) { acc, scopeNode -> acc + getOffsetAtNode(scopeNode) }
            else -> error("Unreachable")
        }
    }


    /**
     * Returns the struct with the provided name
     */
    fun getStruct(name: String): Struct? = structs[name]

    /**
     * Defines a new variable in the given scope
     */
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

    /**
     * Adds a new scope with no values at the given scope
     */
    fun addScopeAt(scope: Stack<Int>) {
        val currentNode = getNodeAtScope(scope)
        (currentNode as Scope).symbols.add(Scope(ArrayList()))
    }

    fun addStringLiteral(value: String) {
        this.stringLiterals[value] = currentStringLiteralIndex++
    }


}

open class Node
class VarNode(val name: String, val type: Type, val offset: Int) : Node()
class Scope(val symbols: MutableList<Node>) : Node()