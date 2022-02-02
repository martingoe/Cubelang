package com.cubearrow.cubelang

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.nasm_rules.ASMEmitter


class Trie(private val rules: List<Rule>) {
    var isArgument: Boolean = false
    var trieEntries: MutableList<TrieEntry> = ArrayList()
    private val emitter: ASMEmitter = ASMEmitter()

    private val astGetSymbol = ASTGetSymbol()

    init {
        trieEntries.add(TrieEntry(' ', 0))
        buildTrieFromRules(rules)
    }

    private fun buildTrieFromRules(rules: List<Rule>) {
        for (i in rules.indices) {
            buildTrieFromExpression(rules[i].expression, i)
        }
    }

    private fun buildTrieFromExpression(expression: Expression, ruleIndex: Int, currentState: Int = 0) {
        val ruleChar = astGetSymbol.evaluate(expression)
        val newState = generateTrieEntryIfNeeded(currentState, ruleChar)
        val children = Utils.getChildren(expression)
        if (children.isEmpty())
            trieEntries[newState].isAccepting[ruleIndex] = true

        children.forEachIndexed { index, child ->
            val indexState = generateTrieEntryIfNeeded(newState, index.toChar())
            buildTrieFromExpression(child, ruleIndex, indexState)
        }
    }

    private fun generateTrieEntryIfNeeded(currentState: Int, newChar: Char): Int {
        return try {
            trieEntries[currentState].next.first { trieEntries[it].value == newChar }
        } catch (e: NoSuchElementException) {
            val originalLength = trieEntries.size
            val newTrieEntry = TrieEntry(newChar, trieEntries[currentState].length + 1)
            trieEntries[currentState].next.add(originalLength)
            trieEntries.add(newTrieEntry)
            originalLength
        }
    }

    fun emitCodeForExpression(visitedExpression: Expression): Expression {
        visit(visitedExpression)
        val rule = visitedExpression.match['r'] ?: TODO("NO RULE for ${visitedExpression::class}")

        emitSubRuleReductions(rules[rule].expression, visitedExpression)
        return rules[rule].constructString(visitedExpression, emitter, this)
    }

    private fun emitSubRuleReductions(rule: Expression, actual: Expression) {
        // Expects rule and actual to have the same arity
        val ruleChildren = Utils.getChildren(rule)
        val actualChildren = Utils.getChildren(actual)

        for (i in ruleChildren.indices) {
            if (ruleChildren[i]::class != actualChildren[i]::class) {
                // Update new child
                val ruleToApply = actualChildren[i].match[astGetSymbol.evaluate(ruleChildren[i])] ?: TODO("Can this be reached?")
                emitSubRuleReductions(rules[ruleToApply].expression, actualChildren[i])
                val newExpression = rules[ruleToApply].constructString(actualChildren[i], emitter, this)
                Utils.setNthChild(i, newExpression, actual)
            } else {
                emitSubRuleReductions(ruleChildren[i], actualChildren[i])
            }
        }


    }



    fun visit(expression: Expression, previous: Int = 0, index: Int = -1) {
        if (index != -1) {
            val indexState = succ(previous, index.toChar())
            val finalState = succ(indexState, astGetSymbol.evaluate(expression))
            expression.state = finalState
        } else {
            // Expression is the root
            val newState = succ(previous, astGetSymbol.evaluate(expression))
            expression.state = newState
        }

        val children = Utils.getChildren(expression)
        children.forEachIndexed { index, it -> visit(it, expression.state, index) }
        postProcess(expression, children, previous, index)
    }

    private fun postProcess(expression: Expression, children: List<Expression>, previousState: Int = 0, index: Int = -1) {
        // TODO: have to reset expression.b?
        for (i in expression.b.indices)
            expression.b[i] = 0
        setPartial(expression, expression.state)
        if (children.isNotEmpty()) {
            for (i in rules.indices) {
                var product = children[0].b[i] / 2
                for (child in children.subList(1, children.size)) {
                    product = product and child.b[i] / 2
                }

                expression.b[i] = expression.b[i] or product
            }
        }
        doReduce(expression, previousState, index)
    }

    private fun doReduce(expression: Expression, previousState: Int = 0, index: Int = -1) {
        for (i in rules.indices) {
            // If there is a rule matching exactly
            if (expression.b[i] % 2 == 1) {
                val possibleNewCost = rules[i].getCost(expression, astGetSymbol, rules)
                if (possibleNewCost < (expression.cost[rules[i].resultSymbol] ?: Int.MAX_VALUE)) {
                    expression.cost[rules[i].resultSymbol] = possibleNewCost
                    expression.match[rules[i].resultSymbol] = i
                    // Expression is root

                    val x: Int = if (index == -1) {
                        succ(0, rules[i].resultSymbol)
                    } else {
                        succ(succ(previousState, index.toChar()), rules[i].resultSymbol)
                    }
                    setPartial(expression, x)
                }
            }
        }
    }

    private fun setPartial(expression: Expression, state: Int) {
        for (rule in rules.indices) {
            if (trieEntries[state].isAccepting[rule]) {
                expression.b[rule] = expression.b[rule] or twoToThePowerOf(getTreeLength(trieEntries[state].length))
            }
        }
    }


    private fun twoToThePowerOf(n: Int): Int {
        val base = 2
        var result = 1
        var temp = n

        while (temp != 0) {
            result *= base
            --temp
        }
        return result
    }

    private fun getTreeLength(pathStringLength: Int): Int = (pathStringLength - 1) / 2

    private fun succ(from: Int, to_value: Char): Int {
        return try {
            trieEntries[from].next.first { trieEntries[it].value == to_value }
        } catch (e: NoSuchElementException) {
            if (trieEntries[from].failureState == from)
                return 0
            // TODO: Is this right? Or just return failure state
            val newState = succ(trieEntries[from].failureState, to_value)
            newState
        }
    }

    fun emitCodeForCallExpression(arg: Expression) {
        this.isArgument = true
        emitCodeForExpression(arg)
        this.isArgument = false
    }


}

class TrieEntry(val value: Char, val length: Int) {
    var next: MutableList<Int> = ArrayList()
    var isAccepting: Array<Boolean> = Array(Rule.RULE_COUNT) { false }
    var failureState: Int = 0
}